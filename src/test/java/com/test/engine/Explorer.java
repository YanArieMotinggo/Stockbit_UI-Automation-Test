package com.test.engine;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import java.util.*;

public class Explorer {

    private final Session app;
    private final Set<String> visitedScreens = new HashSet<>();
    private final Set<String> visitedElements = new HashSet<>();
    private final Map<String, ScreenNode> screenGraph = new HashMap<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> actionLog = new ArrayList<>();
    
    private int maxDepth = 30;
    private int currentDepth = 0;
    private boolean fillForms = true;
    private boolean tryScrolling = true;
    private String rootScreen = null;
    private String appPackage = "com.saucelabs.mydemoapp.android";
    private int crashCount = 0;
    private static final int MAX_CRASH_RECOVERY = 3;
    
    private long startTime;
    private static final long MAX_EXPLORATION_TIME = 5 * 60 * 1000;
    private int stuckCounter = 0;
    private static final int MAX_STUCK_COUNT = 3;
    private String lastScreenSignature = "";

    public Explorer() {
        this.app = Session.current();
    }

    public Explorer maxDepth(int depth) {
        this.maxDepth = depth;
        return this;
    }

    public Explorer fillForms(boolean fill) {
        this.fillForms = fill;
        return this;
    }

    public Explorer scrollToDiscover(boolean scroll) {
        this.tryScrolling = scroll;
        return this;
    }

    public void explore() {
        startTime = System.currentTimeMillis();
        log("Starting exploration, max depth: " + maxDepth + ", timeout: " + (MAX_EXPLORATION_TIME/1000) + "s");
        
        pause(1500);
        waitForAppToLoad();
        rootScreen = getScreenSignature();
        lastScreenSignature = rootScreen;
        dfsExplore(new ArrayList<>());
        printNavigationReport();
    }
    
    private boolean isTimedOut() {
        return System.currentTimeMillis() - startTime > MAX_EXPLORATION_TIME;
    }
    
    private boolean isStuck(String currentScreen) {
        if (currentScreen.equals(lastScreenSignature)) {
            stuckCounter++;
            if (stuckCounter >= MAX_STUCK_COUNT) {
                log("Stuck on same screen " + stuckCounter + " times, breaking out");
                return true;
            }
        } else {
            stuckCounter = 0;
            lastScreenSignature = currentScreen;
        }
        return false;
    }

    private void dfsExplore(List<NavigationStep> pathToHere) {
        if (isTimedOut()) {
            log("Exploration timeout reached, stopping");
            return;
        }
        
        if (currentDepth >= maxDepth) {
            log("Max depth " + maxDepth + " reached, backtracking");
            return;
        }

        if (!verifyAndRecoverAppState()) {
            log("Cannot continue - app in bad state");
            return;
        }

        String currentScreen = getScreenSignature();
        
        if (isStuck(currentScreen)) {
            goBack();
            return;
        }
        
        String screenStateKey = currentScreen + "|depth" + currentDepth;
        if (visitedScreens.contains(screenStateKey)) {
            log("Screen already explored: " + truncate(currentScreen, 40));
            return;
        }

        currentDepth++;
        visitedScreens.add(screenStateKey);
        
        log("\n[Depth " + currentDepth + "] " + truncate(currentScreen, 50));

        // Create or update screen node
        ScreenNode node = screenGraph.computeIfAbsent(currentScreen, k -> new ScreenNode(currentScreen));
        if (node.pathFromRoot == null || pathToHere.size() < node.pathFromRoot.size()) {
            node.pathFromRoot = new ArrayList<>(pathToHere);
        }

        // Check for crash dialogs
        if (checkForCrash()) {
            log("APP CRASH DETECTED!");
            errors.add("CRASH on screen: " + currentScreen + " via path: " + formatPath(pathToHere));
            dismissCrashDialog();
            if (!verifyAndRecoverAppState()) {
                currentDepth--;
                return;
            }
        }

        List<ElementInfo> elements = discoverAllElements();
        node.elements.addAll(elements);
        log("  Found " + elements.size() + " elements");

        if (tryScrolling) {
            List<ElementInfo> moreElements = scrollAndDiscover();
            for (ElementInfo el : moreElements) {
                if (!containsElement(node.elements, el)) {
                    node.elements.add(el);
                }
            }
            scrollToTop();
        }

        if (fillForms) {
            fillAllTextFields(elements);
        }

        List<ElementInfo> clickables = new ArrayList<>();
        for (ElementInfo el : node.elements) {
            if (el.isClickable && !shouldSkipElement(el)) {
                clickables.add(el);
            }
        }
        
        log("  " + clickables.size() + " clickables");
        
        for (ElementInfo element : clickables) {
            if (isTimedOut()) {
                log("Timeout, stopping");
                break;
            }
            
            if (!isStillInApp()) {
                if (!verifyAndRecoverAppState()) {
                    continue;
                }
            }
            
            String elementKey = currentScreen + "|" + element.id;
            
            if (visitedElements.contains(elementKey)) {
                continue;
            }
            visitedElements.add(elementKey);
            
            // Try to tap this element
            log("  Tapping: " + element.id);
            
            // Verify app state before tapping
            if (!verifyAndRecoverAppState()) {
                log("    App crashed, stopping exploration of this branch");
                break;
            }
            
            String beforeTapScreen = getScreenSignature();
            boolean tapped = tapElement(element);
            
            if (!tapped) {
                log("    Could not tap element");
                continue;
            }
            
            pause(100);
            
            // Check if app crashed after tap
            if (!verifyAndRecoverAppState()) {
                log("    App crashed after tapping " + element.id);
                errors.add("Crash after tapping: " + element.id + " on " + truncate(currentScreen, 30));
                continue;
            }
            
            String afterTapScreen = getScreenSignature();
            List<ElementInfo> afterTapElements = discoverAllElements();
            
            // Check what changed
            boolean screenChanged = !afterTapScreen.equals(beforeTapScreen);
            boolean newElementsAppeared = hasNewElements(node.elements, afterTapElements);
            
            if (screenChanged) {
                log("    → NEW SCREEN DISCOVERED!");
                
                // Record the navigation edge
                NavigationStep step = new NavigationStep(currentScreen, element.id, afterTapScreen);
                node.outgoingEdges.add(step);
                
                // Build path to new screen
                List<NavigationStep> newPath = new ArrayList<>(pathToHere);
                newPath.add(step);
                
                // DFS: Explore the new screen deeply
                dfsExplore(newPath);
                
                // Backtrack: Return to current screen
                log("    ← Backtracking to: " + truncate(currentScreen, 40));
                goBack();
                pause(200);
                
                // Verify we're back
                String backScreen = getScreenSignature();
                if (!backScreen.equals(currentScreen)) {
                    log("    Could not return, trying navigation path...");
                    navigateToScreen(pathToHere);
                }
                
            } else if (newElementsAppeared) {
                log("    → NEW ELEMENTS APPEARED (popup/menu/dropdown)!");
                
                // Collect all new clickable elements (menu items)
                List<ElementInfo> newClickables = new ArrayList<>();
                for (ElementInfo newEl : afterTapElements) {
                    if (!containsElement(node.elements, newEl)) {
                        newEl.triggeredBy = element.id;
                        newEl.pathToTrigger = new ArrayList<>(pathToHere);
                        node.elements.add(newEl);
                        
                        log("      + " + newEl.id + " (triggered by " + element.id + ")");
                        
                        if (newEl.isClickable && !shouldSkipElement(newEl)) {
                            newClickables.add(newEl);
                        }
                    }
                }
                
                // Close the menu first
                goBack();
                pause(150);
                
                // Now explore EACH menu item deeply (DFS)
                log("    Exploring " + newClickables.size() + " menu items");
                
                for (ElementInfo menuItem : newClickables) {
                    String menuItemKey = currentScreen + "|menu|" + menuItem.id;
                    if (visitedElements.contains(menuItemKey)) {
                        continue;
                    }
                    visitedElements.add(menuItemKey);
                    
                    if (!verifyAndRecoverAppState()) {
                        continue;
                    }
                    
                    log("    Menu item: " + menuItem.id);
                    
                    // Re-open the menu
                    log("      Opening menu via: " + element.id);
                    boolean menuOpened = tapElement(element);
                    if (!menuOpened) {
                        log("      Could not re-open menu");
                        continue;
                    }
                    pause(200);
                    
                    if (!isStillInApp()) {
                        verifyAndRecoverAppState();
                        continue;
                    }
                    
                    // Tap the menu item
                    log("      Tapping: " + menuItem.id);
                    boolean menuItemTapped = tapElement(menuItem);
                    if (!menuItemTapped) {
                        log("      Could not tap menu item");
                        goBack();
                        pause(150);
                        continue;
                    }
                    pause(100);
                    
                    // Check if app crashed after tapping menu item
                    if (!verifyAndRecoverAppState()) {
                        log("      App crashed after tapping menu item: " + menuItem.id);
                        errors.add("Crash after menu item: " + menuItem.id);
                        continue;
                    }
                    
                    // Check if we navigated to a new screen
                    String menuItemScreen = getScreenSignature();
                    
                    if (!menuItemScreen.equals(currentScreen)) {
                        log("      → NEW SCREEN from menu item!");
                        
                        // Record navigation edge
                        NavigationStep menuStep = new NavigationStep(
                            currentScreen,
                            element.id + " → " + menuItem.id,
                            menuItemScreen
                        );
                        node.outgoingEdges.add(menuStep);
                        
                        // Build path: current path + open menu + tap menu item
                        List<NavigationStep> menuPath = new ArrayList<>(pathToHere);
                        menuPath.add(new NavigationStep(currentScreen, element.id, currentScreen + "|menu"));
                        menuPath.add(new NavigationStep(currentScreen + "|menu", menuItem.id, menuItemScreen));
                        
                        // DFS: Explore the new screen deeply
                        dfsExplore(menuPath);
                        
                        // Backtrack to original screen
                        log("      ← Backtracking from menu item screen");
                        goBack();
                        pause(200);
                        
                        if (!verifyAndRecoverAppState()) {
                            continue;
                        }
                        
                        String backCheck = getScreenSignature();
                        if (!backCheck.equals(currentScreen)) {
                            log("      Not back to original, navigating...");
                            // Try multiple backs
                            for (int i = 0; i < 5; i++) {
                                goBack();
                                pause(150);
                                if (!isStillInApp()) {
                                    verifyAndRecoverAppState();
                                    break;
                                }
                                if (getScreenSignature().equals(currentScreen)) break;
                            }
                        }
                    } else {
                        // Menu item didn't navigate, just close
                        goBack();
                        pause(150);
                    }
                }
            }
        }

        currentDepth--;
    }

    private void navigateToScreen(List<NavigationStep> path) {
        log("  Navigating via path: " + formatPath(path));
        
        // First, go back to root
        for (int i = 0; i < currentDepth + 2; i++) {
            goBack();
            pause(100);
        }
        pause(100);
        
        // Then follow the path
        for (NavigationStep step : path) {
            ElementInfo el = new ElementInfo();
            el.id = step.elementTapped;
            el.resourceId = step.elementTapped;
            
            tapElement(el);
            pause(200);
        }
    }

    private List<ElementInfo> discoverAllElements() {
        List<ElementInfo> elements = new ArrayList<>();
        
        // Quick app check before discovery
        if (!quickAppCheck()) {
            log("    Not in app during element discovery");
            return elements;
        }
        
        // Find all clickable elements
        try {
            List<WebElement> clickables = app.driver().findElements(By.xpath("//*[@clickable='true']"));
            for (WebElement el : clickables) {
                if (isVisible(el)) {
                    ElementInfo info = createElementInfo(el, true);
                    if (!containsElement(elements, info)) {
                        elements.add(info);
                    }
                }
            }
        } catch (Exception e) {
            log("    Error finding clickables: " + e.getMessage());
            // Check if error is because we left the app
            if (!quickAppCheck()) {
                log("    Left app during element discovery");
                return elements;
            }
        }
        
        // Find text fields
        try {
            List<WebElement> inputs = app.driver().findElements(By.className("android.widget.EditText"));
            for (WebElement el : inputs) {
                if (isVisible(el)) {
                    ElementInfo info = createElementInfo(el, false);
                    info.isTextField = true;
                    if (!containsElement(elements, info)) {
                        elements.add(info);
                    }
                }
            }
        } catch (Exception ignored) {}
        
        // Find text views (for verification)
        try {
            List<WebElement> texts = app.driver().findElements(By.className("android.widget.TextView"));
            for (WebElement el : texts) {
                if (isVisible(el)) {
                    String text = el.getText();
                    if (text != null && !text.isEmpty() && text.length() < 50) {
                        ElementInfo info = createElementInfo(el, false);
                        info.isTextView = true;
                        if (!containsElement(elements, info)) {
                            elements.add(info);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        
        return elements;
    }

    private ElementInfo createElementInfo(WebElement el, boolean clickable) {
        ElementInfo info = new ElementInfo();
        try {
            info.resourceId = el.getAttribute("resource-id");
            info.text = el.getText();
            info.contentDesc = el.getAttribute("content-desc");
            info.className = el.getAttribute("class");
            info.isClickable = clickable || "true".equals(el.getAttribute("clickable"));
            
            // Build ID for display
            if (info.resourceId != null && !info.resourceId.isEmpty()) {
                info.id = info.resourceId.substring(info.resourceId.lastIndexOf("/") + 1);
            } else if (info.text != null && !info.text.isEmpty()) {
                info.id = "text:" + info.text.substring(0, Math.min(20, info.text.length()));
            } else if (info.contentDesc != null && !info.contentDesc.isEmpty()) {
                info.id = "desc:" + info.contentDesc;
            } else {
                info.id = "class:" + (info.className != null ? info.className.substring(info.className.lastIndexOf(".") + 1) : "unknown");
            }
        } catch (Exception e) {
            info.id = "error:" + System.currentTimeMillis();
        }
        return info;
    }

    private List<ElementInfo> scrollAndDiscover() {
        List<ElementInfo> allElements = new ArrayList<>();
        int noNewCount = 0;
        
        for (int i = 0; i < 5; i++) {
            // Check app state before scrolling
            if (!quickAppCheck()) {
                log("    Left app during scroll discovery");
                break;
            }
            
            scrollDown();
            pause(150);
            
            // Check app state after scrolling
            if (!quickAppCheck()) {
                log("    Left app after scroll");
                break;
            }
            
            List<ElementInfo> found = discoverAllElements();
            int newCount = 0;
            
            for (ElementInfo el : found) {
                if (!containsElement(allElements, el)) {
                    allElements.add(el);
                    newCount++;
                }
            }
            
            if (newCount > 0) {
                log("    Scroll " + (i+1) + ": Found " + newCount + " new elements");
                noNewCount = 0;
            } else {
                noNewCount++;
                if (noNewCount >= 2) break;
            }
        }
        
        return allElements;
    }

    private void scrollToTop() {
        for (int i = 0; i < 5; i++) {
            scrollUp();
                    pause(100);
        }
    }

    private boolean tapElement(ElementInfo el) {
        // Quick check before attempting tap
        if (!quickAppCheck()) {
            log("      Not in app before tap attempt");
            return false;
        }
        
        try {
            WebElement element = null;
            
            // Try by resource ID
            if (el.resourceId != null && !el.resourceId.isEmpty()) {
                try {
                    element = app.driver().findElement(By.id(el.resourceId));
                } catch (Exception ignored) {}
            }
            
            // Try by text
            if (element == null && el.text != null && !el.text.isEmpty()) {
                try {
                    element = app.driver().findElement(By.xpath("//*[@text='" + el.text + "']"));
                } catch (Exception ignored) {}
            }
            
            // Try by content-desc
            if (element == null && el.contentDesc != null && !el.contentDesc.isEmpty()) {
                try {
                    element = app.driver().findElement(By.xpath("//*[@content-desc='" + el.contentDesc + "']"));
                } catch (Exception ignored) {}
            }
            
            if (element != null && element.isDisplayed()) {
                element.click();
                
                // Quick check after tap
                pause(150);
                if (!quickAppCheck()) {
                    log("      Left app after tapping " + el.id);
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            errors.add("Tap failed: " + el.id + " - " + e.getMessage());
            // Check if we're still in app after error
            if (!quickAppCheck()) {
                log("      Left app after tap error");
            }
        }
        return false;
    }

    private void fillAllTextFields(List<ElementInfo> elements) {
        for (ElementInfo el : elements) {
            if (el.isTextField) {
                try {
                    WebElement field = null;
                    if (el.resourceId != null) {
                        field = app.driver().findElement(By.id(el.resourceId));
                    }
                    if (field != null && field.isDisplayed()) {
                        String value = generateTestValue(el.id);
                        log("    Filling " + el.id + " with: " + value);
                        field.clear();
                        field.sendKeys(value);
                        pause(100);
                    }
                } catch (Exception e) {
                    errors.add("Fill failed: " + el.id);
                }
            }
        }
        hideKeyboard();
    }

    private void waitForAppToLoad() {
        for (int i = 0; i < 10; i++) {
            try {
                List<WebElement> elements = app.driver().findElements(By.xpath("//*[@clickable='true']"));
                if (!elements.isEmpty()) {
                    log("App loaded - found " + elements.size() + " clickable elements");
                    return;
                }
            } catch (Exception ignored) {}
            pause(100);
            log("  Waiting for app... (" + (i+1) + "/10)");
        }
    }

    private String getScreenSignature() {
        try {
            String activity = app.driver().currentActivity();
            if (activity == null) activity = "unknown";
            
            List<WebElement> texts = app.driver().findElements(By.className("android.widget.TextView"));
            StringBuilder sig = new StringBuilder(activity);
            
            int count = 0;
            for (WebElement t : texts) {
                try {
                    String text = t.getText();
                    if (text != null && !text.isEmpty() && text.length() < 30) {
                        sig.append("|").append(text);
                        if (++count >= 2) break;
                    }
                } catch (Exception ignored) {}
            }
            return sig.toString();
        } catch (Exception e) {
            return "screen_" + System.currentTimeMillis();
        }
    }

    private boolean hasNewElements(List<ElementInfo> before, List<ElementInfo> after) {
        for (ElementInfo el : after) {
            if (!containsElement(before, el)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsElement(List<ElementInfo> list, ElementInfo el) {
        for (ElementInfo e : list) {
            if (e.id != null && e.id.equals(el.id)) return true;
        }
        return false;
    }

    private boolean isVisible(WebElement el) {
        try {
            return el.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldSkipElement(ElementInfo el) {
        if (el.id == null) return true;
        String id = el.id.toLowerCase();
        
        if (id.contains("back") || id.contains("navigate_up") || 
            id.contains("home") || id.equals("unknown")) {
            return true;
        }
        
        if (id.contains("aerr_") || id.contains("alert") || 
            id.contains("error:") || id.contains("crash")) {
            return true;
        }
        
        return false;
    }

    private String generateTestValue(String fieldId) {
        if (fieldId == null) return "test123";
        String id = fieldId.toLowerCase();
        if (id.contains("email")) return "test@example.com";
        if (id.contains("password")) return "Test123!";
        if (id.contains("phone")) return "1234567890";
        if (id.contains("name")) return "Test User";
        if (id.contains("search")) return "test";
        return "test123";
    }

    private void scrollDown() {
        try {
            Dimension size = app.driver().manage().window().getSize();
            int startX = size.width / 2;
            int startY = (int) (size.height * 0.7);
            int endY = (int) (size.height * 0.3);
            
            org.openqa.selenium.interactions.PointerInput finger = 
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence scroll = 
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            
            scroll.addAction(finger.createPointerMove(java.time.Duration.ZERO, 
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(finger.createPointerMove(java.time.Duration.ofMillis(300), 
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
            scroll.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            
            app.driver().perform(Arrays.asList(scroll));
        } catch (Exception ignored) {}
    }

    private void scrollUp() {
        try {
            Dimension size = app.driver().manage().window().getSize();
            int startX = size.width / 2;
            int startY = (int) (size.height * 0.3);
            int endY = (int) (size.height * 0.7);
            
            org.openqa.selenium.interactions.PointerInput finger = 
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence scroll = 
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            
            scroll.addAction(finger.createPointerMove(java.time.Duration.ZERO, 
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(finger.createPointerMove(java.time.Duration.ofMillis(300), 
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
            scroll.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            
            app.driver().perform(Arrays.asList(scroll));
        } catch (Exception ignored) {}
    }

    private void hideKeyboard() {
        try { app.driver().hideKeyboard(); } catch (Exception ignored) {}
    }

    private void goBack() {
        try {
            app.driver().navigate().back();
            pause(100);
            // Check if back caused us to leave the app
            if (!quickAppCheck()) {
                log("    Back button caused app exit");
            }
        } catch (Exception e) {
            log("    Error going back: " + e.getMessage());
        }
    }

    private boolean checkForCrash() {
        try {
            // Check for crash/ANR dialogs - be specific
            List<WebElement> crashDialogs = app.driver().findElements(
                By.xpath("//*[contains(@text, 'has stopped') or contains(@text, 'keeps stopping') or " +
                         "contains(@text, 'isn\\'t responding') or contains(@text, 'Unfortunately')]"));
            if (!crashDialogs.isEmpty()) {
                log("CRASH DIALOG DETECTED");
                return true;
            }
            return false;
        } catch (Exception e) {
            // Exception during check doesn't mean crash - could be timing issue
            // Only return true if we're actually not in the app
            return !quickAppCheck();
        }
    }
    
    private boolean isStillInApp() {
        try {
            String currentPackage = app.driver().getCurrentPackage();
            
            if (currentPackage == null) {
                log("Cannot get current package - session may be dead");
                return false;
            }
            
            if (!currentPackage.contains("saucelabs") && !currentPackage.contains("mydemoapp")) {
                log("LEFT THE APP! Current package: " + currentPackage);
                return false;
            }
            
            // Check if we're on launcher
            if (currentPackage.contains("launcher") || currentPackage.contains("home") || 
                currentPackage.contains("nexuslauncher") || currentPackage.contains("settings")) {
                log("ON LAUNCHER/SETTINGS - App may have crashed");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log("Cannot determine app state: " + e.getMessage());
            return false;
        }
    }
    
    private boolean quickAppCheck() {
        try {
            String pkg = app.driver().getCurrentPackage();
            if (pkg == null) {
                // Null package could be timing issue, try again
                pause(150);
                pkg = app.driver().getCurrentPackage();
            }
            return pkg != null && (pkg.contains("saucelabs") || pkg.contains("mydemoapp"));
        } catch (Exception e) {
            // Session error - but this doesn't mean app crashed
            // It could be UiAutomator2 issue
            try {
                // Try to get any response from driver
                app.driver().getPageSource();
                return true; // If we got page source, we're probably still in app
            } catch (Exception e2) {
                return false;
            }
        }
    }
    
    private boolean verifyAndRecoverAppState() {
        // Quick check first - if we're in app, we're good
        if (quickAppCheck()) {
            // Check for crash dialog only if we're in app
            if (checkForCrash()) {
                dismissCrashDialog();
                pause(100);
                crashCount++;
                errors.add("APP CRASH #" + crashCount + " detected and dismissed");
                
                if (crashCount >= MAX_CRASH_RECOVERY) {
                    log("Too many crashes (" + crashCount + "), stopping exploration");
                    return false;
                }
            }
            return true;
        }
        
        // Not in app - try to recover
        log("Not in app, attempting recovery...");
        return restartApp();
    }
    
    private boolean restartApp() {
        try {
            log("Restarting app...");
            
            // Try to launch the app
            app.driver().activateApp(appPackage);
            pause(150);
            
            // Verify we're back in app
            if (isStillInApp()) {
                log("App restarted successfully");
                crashCount = 0;
                return true;
            }
            
            // Try harder - terminate and relaunch
            log("Force restarting app...");
            try {
                app.driver().terminateApp(appPackage);
            } catch (Exception ignored) {}
            pause(100);
            
            app.driver().activateApp(appPackage);
            pause(100);
            
            if (isStillInApp()) {
                log("App force-restarted successfully");
                return true;
            }
            
            log("Could not restart app");
            errors.add("FATAL: Could not restart app after crash");
            return false;
            
        } catch (Exception e) {
            log("Error restarting app: " + e.getMessage());
            errors.add("FATAL: Exception while restarting app: " + e.getMessage());
            return false;
        }
    }

    private void dismissCrashDialog() {
        try {
            // Try various dismiss buttons
            String[] dismissTexts = {"OK", "Close", "Close app", "Wait", "CLOSE", "Dismiss"};
            for (String text : dismissTexts) {
                try {
                    List<WebElement> buttons = app.driver().findElements(By.xpath("//*[@text='" + text + "']"));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                        log("  Dismissed crash dialog via: " + text);
                pause(100);
                        return;
            }
        } catch (Exception ignored) {}
            }
            
            // Try clicking any button
            List<WebElement> anyButtons = app.driver().findElements(By.className("android.widget.Button"));
            if (!anyButtons.isEmpty()) {
                anyButtons.get(0).click();
                log("  Dismissed crash dialog via button");
            }
        } catch (Exception e) {
            log("  Could not dismiss crash dialog: " + e.getMessage());
        }
    }

    private void pause(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void log(String msg) {
        actionLog.add(msg);
        System.out.println(msg);
    }

    private String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String formatPath(List<NavigationStep> path) {
        if (path == null || path.isEmpty()) return "[ROOT]";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(path.get(i).elementTapped);
        }
        return sb.toString();
    }

    private void printNavigationReport() {
        System.out.println("\n=== EXPLORATION COMPLETE ===");
        System.out.println("Screens Discovered: " + screenGraph.size());
        System.out.println("Total Elements: " + getTotalElements());
        System.out.println("Elements Interacted: " + visitedElements.size());
        System.out.println("Errors: " + errors.size());

        System.out.println("\nNAVIGATION MAP:\n");
        
        for (Map.Entry<String, ScreenNode> entry : screenGraph.entrySet()) {
            ScreenNode node = entry.getValue();
            System.out.println("Screen: " + truncate(entry.getKey(), 64));
            System.out.println("  Path: " + formatPath(node.pathFromRoot));
            System.out.println("  Elements (" + node.elements.size() + "):");
            
            Map<String, List<ElementInfo>> byTrigger = new LinkedHashMap<>();
            byTrigger.put("Direct", new ArrayList<>());
            
            for (ElementInfo el : node.elements) {
                String trigger = el.triggeredBy != null ? "Via " + el.triggeredBy : "Direct";
                byTrigger.computeIfAbsent(trigger, k -> new ArrayList<>()).add(el);
            }
            
            for (Map.Entry<String, List<ElementInfo>> group : byTrigger.entrySet()) {
                if (!group.getValue().isEmpty()) {
                    System.out.println("    [" + group.getKey() + "]");
                    for (ElementInfo el : group.getValue()) {
                        String type = el.isClickable ? "(btn)" : (el.isTextField ? "(input)" : "(text)");
                        System.out.println("      " + type + " " + el.id);
                    }
                }
            }
            
            if (!node.outgoingEdges.isEmpty()) {
                System.out.println("  Leads to:");
                for (NavigationStep edge : node.outgoingEdges) {
                    System.out.println("    Tap [" + edge.elementTapped + "] -> " + truncate(edge.toScreen, 40));
                }
            }
            System.out.println();
        }
        
        if (!errors.isEmpty()) {
            System.out.println("\nERRORS:");
            for (String err : errors) {
                System.out.println("  - " + err);
            }
        }
    }

    private int getTotalElements() {
        return screenGraph.values().stream().mapToInt(n -> n.elements.size()).sum();
    }

    public Set<String> getVisitedScreens() { return visitedScreens; }
    public Map<String, ScreenNode> getScreenMap() { return screenGraph; }
    public List<String> getErrors() { return errors; }
    public Map<String, ScreenNode> getNavigationGraph() { return screenGraph; }

    public static class ScreenNode {
        public String id;
        public List<NavigationStep> pathFromRoot;  // How to reach this screen from app start
        public List<ElementInfo> elements = new ArrayList<>();
        public List<NavigationStep> outgoingEdges = new ArrayList<>();  // Where you can go from here

        ScreenNode(String id) { 
            this.id = id; 
        }
        
        public ScreenInfo toScreenInfo() {
            ScreenInfo info = new ScreenInfo(id);
            info.allElements.addAll(elements);
            for (ElementInfo el : elements) {
                if (el.isTextField) info.textFields.add(el);
                if (el.isClickable) info.clickables.add(el);
            }
            return info;
        }
    }

    public static class NavigationStep {
        public String fromScreen;
        public String elementTapped;
        public String toScreen;

        NavigationStep(String from, String element, String to) {
            this.fromScreen = from;
            this.elementTapped = element;
            this.toScreen = to;
        }
        
        @Override
        public String toString() {
            return elementTapped;
        }
    }

    public static class ElementInfo {
        public String id;
        public String resourceId;
        public String text;
        public String contentDesc;
        public String className;
        public boolean isClickable = false;
        public boolean isTextField = false;
        public boolean isTextView = false;
        public boolean interacted = false;
        
        // How to reach this element (for popup/menu items)
        public String triggeredBy;  // Which element shows this one
        public List<NavigationStep> pathToTrigger;  // Path to the screen + trigger element
    }

    public static class ScreenInfo {
        public String id;
        public List<ElementInfo> allElements = new ArrayList<>();
        public List<ElementInfo> textFields = new ArrayList<>();
        public List<ElementInfo> toggles = new ArrayList<>();
        public List<ElementInfo> dropdowns = new ArrayList<>();
        public List<ElementInfo> clickables = new ArrayList<>();

        ScreenInfo(String id) { this.id = id; }
    }
}

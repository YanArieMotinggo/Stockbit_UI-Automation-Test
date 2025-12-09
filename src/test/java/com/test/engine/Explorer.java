package com.test.engine;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;

import java.util.*;

/**
 * Autonomous app explorer that discovers and interacts with EVERY element on each screen.
 * - Taps all buttons/clickables
 * - Fills all text fields
 * - Toggles all switches
 * - Opens all dropdowns
 * - Scrolls to find hidden elements
 * - Records everything it finds
 */
public class Explorer {
    private final Session app;
    private final Set<String> visitedScreens = new HashSet<>();
    private final Set<String> interactedElements = new HashSet<>();
    private final Map<String, ScreenInfo> screenMap = new HashMap<>();
    private final List<String> actionLog = new ArrayList<>();
    
    private int maxDepth = 5;
    private int currentDepth = 0;
    private boolean fillForms = true;
    private boolean tryScrolling = true;

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

    /**
     * Start full exploration
     */
    public void explore() {
        log("=== STARTING FULL APP EXPLORATION ===");
        log("Max depth: " + maxDepth);
        log("Fill forms: " + fillForms);
        log("Scroll discovery: " + tryScrolling);
        
        pause(2000); // Wait for app to load
        exploreCurrentScreen();
        printReport();
    }

    private void exploreCurrentScreen() {
        if (currentDepth >= maxDepth) {
            log("⚠ Max depth " + maxDepth + " reached");
            return;
        }

        String screenId = getScreenSignature();
        
        // Check if we've fully explored this screen
        if (visitedScreens.contains(screenId)) {
            log("↩ Screen already explored: " + truncate(screenId, 50));
            return;
        }

        currentDepth++;
        visitedScreens.add(screenId);
        log("\n══════════════════════════════════════");
        log("📱 NEW SCREEN [Depth " + currentDepth + "]: " + truncate(screenId, 60));
        log("══════════════════════════════════════");

        ScreenInfo info = new ScreenInfo(screenId);
        
        // === PHASE 1: Discover all elements ===
        log("\n--- Phase 1: Discovering Elements ---");
        discoverAllElements(info);
        
        // === PHASE 2: Scroll and discover more ===
        if (tryScrolling) {
            log("\n--- Phase 2: Scrolling to Find More ---");
            scrollAndDiscover(info);
        }
        
        // === PHASE 3: Fill all text fields ===
        if (fillForms) {
            log("\n--- Phase 3: Filling Text Fields ---");
            fillAllTextFields(info);
        }

        // === PHASE 4: Toggle all switches/checkboxes ===
        log("\n--- Phase 4: Toggling Switches ---");
        toggleAllSwitches(info);

        // === PHASE 5: Tap all buttons and explore ===
        log("\n--- Phase 5: Tapping All Buttons ---");
        tapAllClickables(info);

        screenMap.put(screenId, info);
        currentDepth--;
    }

    private void discoverAllElements(ScreenInfo info) {
        // Buttons
        findAndRecord(info, "android.widget.Button", "button");
        // ImageButtons  
        findAndRecord(info, "android.widget.ImageButton", "imageButton");
        // ImageViews
        findAndRecord(info, "android.widget.ImageView", "imageView");
        // TextViews
        findAndRecord(info, "android.widget.TextView", "textView");
        // EditTexts
        findAndRecord(info, "android.widget.EditText", "editText");
        // Switches
        findAndRecord(info, "android.widget.Switch", "switch");
        // CheckBoxes
        findAndRecord(info, "android.widget.CheckBox", "checkBox");
        // RadioButtons
        findAndRecord(info, "android.widget.RadioButton", "radioButton");
        // Spinners (dropdowns)
        findAndRecord(info, "android.widget.Spinner", "spinner");
        // RecyclerView items
        findRecyclerItems(info);
        
        log("  Total elements found: " + info.allElements.size());
    }

    private void findAndRecord(ScreenInfo info, String className, String type) {
        try {
            List<WebElement> elements = app.driver().findElements(By.className(className));
            for (WebElement el : elements) {
                if (isVisible(el)) {
                    ElementInfo elInfo = new ElementInfo(el, type);
                    info.allElements.add(elInfo);
                    
                    if (type.equals("editText")) info.textFields.add(elInfo);
                    if (type.equals("switch") || type.equals("checkBox")) info.toggles.add(elInfo);
                    if (type.equals("spinner")) info.dropdowns.add(elInfo);
                    if (isClickable(el, type)) info.clickables.add(elInfo);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void findRecyclerItems(ScreenInfo info) {
        try {
            List<WebElement> items = app.driver().findElements(
                By.xpath("//androidx.recyclerview.widget.RecyclerView//*[@clickable='true']"));
            for (WebElement el : items) {
                if (isVisible(el)) {
                    ElementInfo elInfo = new ElementInfo(el, "listItem");
                    info.allElements.add(elInfo);
                    info.clickables.add(elInfo);
                }
            }
        } catch (Exception ignored) {}
    }

    private void scrollAndDiscover(ScreenInfo info) {
        int scrollAttempts = 3;
        int previousCount = info.allElements.size();
        
        for (int i = 0; i < scrollAttempts; i++) {
            scrollDown();
            pause(500);
            
            // Re-discover after scroll
            int beforeScroll = info.allElements.size();
            discoverAllElements(info);
            
            if (info.allElements.size() > beforeScroll) {
                log("  ↓ Scroll " + (i+1) + ": Found " + (info.allElements.size() - beforeScroll) + " new elements");
            } else {
                log("  ↓ Scroll " + (i+1) + ": No new elements, stopping scroll");
                break;
            }
        }
        
        // Scroll back to top
        for (int i = 0; i < scrollAttempts; i++) {
            scrollUp();
            pause(300);
        }
    }

    private void fillAllTextFields(ScreenInfo info) {
        for (ElementInfo elInfo : info.textFields) {
            try {
                WebElement el = refindElement(elInfo);
                if (el != null && el.isDisplayed()) {
                    String testValue = generateTestValue(elInfo);
                    log("  ✏ Filling [" + elInfo.id + "] with: " + testValue);
                    el.clear();
                    el.sendKeys(testValue);
                    elInfo.interacted = true;
                    pause(300);
                }
            } catch (Exception e) {
                log("  ✗ Failed to fill: " + elInfo.id);
            }
        }
        
        // Hide keyboard if open
        hideKeyboard();
    }

    private void toggleAllSwitches(ScreenInfo info) {
        for (ElementInfo elInfo : info.toggles) {
            try {
                WebElement el = refindElement(elInfo);
                if (el != null && el.isDisplayed()) {
                    String before = el.getAttribute("checked");
                    log("  🔘 Toggling [" + elInfo.id + "] (was: " + before + ")");
                    el.click();
                    pause(300);
                    String after = el.getAttribute("checked");
                    log("    → Now: " + after);
                    elInfo.interacted = true;
                    
                    // Toggle back
                    el.click();
                    pause(200);
                }
            } catch (Exception e) {
                log("  ✗ Failed to toggle: " + elInfo.id);
            }
        }
    }

    private void tapAllClickables(ScreenInfo info) {
        String originalScreen = getScreenSignature();
        
        for (ElementInfo elInfo : new ArrayList<>(info.clickables)) {
            String elementKey = originalScreen + "|" + elInfo.id;
            
            if (interactedElements.contains(elementKey)) {
                continue;
            }
            
            if (shouldSkipElement(elInfo)) {
                log("  ⏭ Skipping: " + elInfo.id);
                continue;
            }

            try {
                WebElement el = refindElement(elInfo);
                if (el == null || !el.isDisplayed()) {
                    continue;
                }

                log("  👆 Tapping: " + elInfo.id);
                interactedElements.add(elementKey);
                el.click();
                elInfo.interacted = true;
                pause(1000);

                // Check if we navigated away
                String newScreen = getScreenSignature();
                if (!newScreen.equals(originalScreen)) {
                    log("    → Navigated to new screen!");
                    exploreCurrentScreen(); // Recursively explore
                    goBack();
                    pause(800);
                    
                    // Verify we're back
                    String afterBack = getScreenSignature();
                    if (!afterBack.equals(originalScreen)) {
                        log("    ⚠ Could not return to original screen");
                        return; // Stop exploring this screen
                    }
                }
            } catch (Exception e) {
                log("  ✗ Failed to tap: " + elInfo.id + " - " + e.getMessage());
            }
        }
    }

    // === Helper Methods ===

    private String getScreenSignature() {
        try {
            String activity = app.driver().currentActivity();
            if (activity == null) activity = "unknown";
            
            // Get key text elements for uniqueness
            List<WebElement> texts = app.driver().findElements(By.className("android.widget.TextView"));
            StringBuilder sig = new StringBuilder(activity);
            
            int count = 0;
            for (WebElement t : texts) {
                try {
                    String text = t.getText();
                    if (text != null && !text.isEmpty() && text.length() < 25 && !text.matches("\\d+")) {
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

    private WebElement refindElement(ElementInfo info) {
        try {
            if (info.resourceId != null && !info.resourceId.isEmpty()) {
                return app.driver().findElement(By.id(info.resourceId));
            }
            if (info.xpath != null) {
                return app.driver().findElement(By.xpath(info.xpath));
            }
            if (info.text != null && !info.text.isEmpty()) {
                return app.driver().findElement(By.xpath("//*[@text='" + info.text + "']"));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String generateTestValue(ElementInfo info) {
        String id = info.id.toLowerCase();
        
        if (id.contains("email") || id.contains("mail")) return "test@example.com";
        if (id.contains("password") || id.contains("pass")) return "Test123!";
        if (id.contains("phone") || id.contains("mobile")) return "1234567890";
        if (id.contains("name") && id.contains("first")) return "John";
        if (id.contains("name") && id.contains("last")) return "Doe";
        if (id.contains("name")) return "Test User";
        if (id.contains("address")) return "123 Test Street";
        if (id.contains("city")) return "Test City";
        if (id.contains("zip") || id.contains("postal")) return "12345";
        if (id.contains("country")) return "USA";
        if (id.contains("card") || id.contains("credit")) return "4111111111111111";
        if (id.contains("cvv") || id.contains("cvc")) return "123";
        if (id.contains("expir")) return "12/25";
        if (id.contains("quantity") || id.contains("qty")) return "2";
        if (id.contains("search")) return "test";
        
        return "TestInput123";
    }

    private boolean isVisible(WebElement el) {
        try {
            return el.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isClickable(WebElement el, String type) {
        try {
            String clickable = el.getAttribute("clickable");
            if ("true".equals(clickable)) return true;
            
            // These types are usually clickable
            return type.equals("button") || type.equals("imageButton") || 
                   type.equals("listItem") || type.equals("spinner");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldSkipElement(ElementInfo info) {
        String id = info.id.toLowerCase();
        
        // Skip navigation elements that would take us out
        if (id.contains("back") || id.contains("home") || id.contains("navigate_up")) return true;
        if (id.contains("close") || id.contains("cancel") || id.contains("dismiss")) return true;
        
        // Skip if no useful identifier
        if (info.id.equals("unknown") && info.text == null) return true;
        
        return false;
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
            scroll.addAction(finger.createPointerMove(java.time.Duration.ofMillis(500), 
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
            scroll.addAction(finger.createPointerMove(java.time.Duration.ofMillis(500), 
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
            scroll.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            
            app.driver().perform(Arrays.asList(scroll));
        } catch (Exception ignored) {}
    }

    private void hideKeyboard() {
        try {
            app.driver().hideKeyboard();
        } catch (Exception ignored) {}
    }

    private void goBack() {
        try {
            app.driver().navigate().back();
            pause(500);
        } catch (Exception ignored) {}
    }

    private void pause(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void log(String msg) {
        actionLog.add(msg);
        System.out.println(msg);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // === Report ===

    public void printReport() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              EXPLORATION COMPLETE                            ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Screens Discovered: " + pad(String.valueOf(visitedScreens.size()), 38) + " ║");
        System.out.println("║  Elements Found: " + pad(String.valueOf(getTotalElements()), 42) + " ║");
        System.out.println("║  Elements Interacted: " + pad(String.valueOf(interactedElements.size()), 37) + " ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        
        System.out.println("\n📱 SCREEN MAP:\n");
        
        for (Map.Entry<String, ScreenInfo> entry : screenMap.entrySet()) {
            ScreenInfo info = entry.getValue();
            System.out.println("┌─────────────────────────────────────────────────────────");
            System.out.println("│ " + truncate(entry.getKey(), 55));
            System.out.println("├─────────────────────────────────────────────────────────");
            System.out.println("│ Text Fields: " + info.textFields.size());
            for (ElementInfo el : info.textFields) {
                System.out.println("│   • " + el.id + (el.interacted ? " ✓" : ""));
            }
            System.out.println("│ Toggles: " + info.toggles.size());
            for (ElementInfo el : info.toggles) {
                System.out.println("│   • " + el.id + (el.interacted ? " ✓" : ""));
            }
            System.out.println("│ Clickables: " + info.clickables.size());
            for (ElementInfo el : info.clickables) {
                System.out.println("│   • " + el.id + (el.interacted ? " ✓" : ""));
            }
            System.out.println("└─────────────────────────────────────────────────────────\n");
        }
    }

    private int getTotalElements() {
        return screenMap.values().stream().mapToInt(s -> s.allElements.size()).sum();
    }

    private String pad(String s, int len) {
        return s + " ".repeat(Math.max(0, len - s.length()));
    }

    public Set<String> getVisitedScreens() { return visitedScreens; }
    public Map<String, ScreenInfo> getScreenMap() { return screenMap; }

    // === Inner Classes ===

    public static class ScreenInfo {
        String id;
        List<ElementInfo> allElements = new ArrayList<>();
        List<ElementInfo> textFields = new ArrayList<>();
        List<ElementInfo> toggles = new ArrayList<>();
        List<ElementInfo> dropdowns = new ArrayList<>();
        List<ElementInfo> clickables = new ArrayList<>();

        ScreenInfo(String id) { this.id = id; }
    }

    public static class ElementInfo {
        String id;
        String type;
        String resourceId;
        String text;
        String contentDesc;
        String xpath;
        boolean interacted = false;

        ElementInfo(WebElement el, String type) {
            this.type = type;
            try {
                this.resourceId = el.getAttribute("resource-id");
                this.text = el.getText();
                this.contentDesc = el.getAttribute("content-desc");
                
                // Build ID for display
                if (resourceId != null && !resourceId.isEmpty()) {
                    this.id = resourceId.substring(resourceId.lastIndexOf("/") + 1);
                    this.xpath = "//*[@resource-id='" + resourceId + "']";
                } else if (text != null && !text.isEmpty()) {
                    this.id = "text:" + text.substring(0, Math.min(20, text.length()));
                    this.xpath = "//*[@text='" + text + "']";
                } else if (contentDesc != null && !contentDesc.isEmpty()) {
                    this.id = "desc:" + contentDesc;
                    this.xpath = "//*[@content-desc='" + contentDesc + "']";
                } else {
                    this.id = "unknown";
                }
            } catch (Exception e) {
                this.id = "error";
            }
        }
    }
}

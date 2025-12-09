package com.test.engine;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TestGenerator {

    private final Map<String, Explorer.ScreenNode> screenMap;
    private final String outputDir;
    private final List<String> generatedScenarios = new ArrayList<>();

    public TestGenerator(Map<String, Explorer.ScreenNode> screenMap) {
        this.screenMap = screenMap;
        this.outputDir = "src/test/resources/features/generated/";
    }

    public void generateAll() {
        createOutputDirectory();
        
        // Generate navigation tests
        generateNavigationTests();
        
        // Generate element interaction tests
        generateElementTests();
        
        // Generate form tests
        generateFormTests();
        
        // Generate smoke test suite
        generateSmokeTestSuite();
        
        System.out.println("\n✅ TEST GENERATION COMPLETE");
        System.out.println("Generated " + generatedScenarios.size() + " test scenarios");
        System.out.println("Output directory: " + outputDir);
    }

    private void createOutputDirectory() {
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            throw new RuntimeException("Cannot create output directory", e);
        }
    }

    private void generateNavigationTests() {
        StringBuilder feature = new StringBuilder();
        feature.append("@generated @navigation\n");
        feature.append("Feature: Navigation Tests\n");
        feature.append("  Verify all discovered screens are accessible with navigation paths\n\n");

        int scenarioCount = 0;
        
        for (String screenId : screenMap.keySet()) {
            Explorer.ScreenNode screen = screenMap.get(screenId);
            
            // Generate test with full navigation path
            if (screen.pathFromRoot != null && !screen.pathFromRoot.isEmpty()) {
                scenarioCount++;
                String scenarioName = "Navigate to " + sanitizeName(screenId);
                
                feature.append("  @nav").append(scenarioCount).append("\n");
                feature.append("  Scenario: ").append(scenarioName).append("\n");
                feature.append("    # Path: ");
                for (Explorer.NavigationStep step : screen.pathFromRoot) {
                    feature.append(step.elementTapped).append(" → ");
                }
                feature.append("Screen\n");
                feature.append("    Given the app is running\n");
                
                // Add each step in the path
                for (Explorer.NavigationStep step : screen.pathFromRoot) {
                    feature.append("    When I tap on \"").append(step.elementTapped).append("\"\n");
                }
                feature.append("    Then I should see a screen\n\n");
                
                generatedScenarios.add(scenarioName);
                
                if (scenarioCount >= 20) break;
            }
            
            // Also generate tests for outgoing edges
            for (Explorer.NavigationStep edge : screen.outgoingEdges) {
                if (scenarioCount >= 20) break;
                if (shouldSkipForNav(edge.elementTapped)) continue;
                
                scenarioCount++;
                String scenarioName = "Tap " + sanitizeName(edge.elementTapped) + " navigates correctly";
                
                feature.append("  @nav").append(scenarioCount).append("\n");
                feature.append("  Scenario: ").append(scenarioName).append("\n");
                feature.append("    Given the app is running\n");
                
                // First navigate to the source screen
                if (screen.pathFromRoot != null) {
                    for (Explorer.NavigationStep step : screen.pathFromRoot) {
                        feature.append("    When I tap on \"").append(step.elementTapped).append("\"\n");
                    }
                }
                
                // Then tap the element
                feature.append("    When I tap on \"").append(edge.elementTapped).append("\"\n");
                feature.append("    Then I should see a screen\n");
                feature.append("    And I go back\n\n");
                
                generatedScenarios.add(scenarioName);
            }
        }

        writeFeatureFile("Navigation.feature", feature.toString());
    }

    private void generateElementTests() {
        StringBuilder feature = new StringBuilder();
        feature.append("@generated @elements\n");
        feature.append("Feature: Element Interaction Tests\n");
        feature.append("  Verify all discovered elements are interactive\n");
        feature.append("  Includes elements that appear after other actions (popups, menus)\n\n");

        int scenarioCount = 0;
        Set<String> testedElements = new HashSet<>();

        for (String screenId : screenMap.keySet()) {
            Explorer.ScreenNode screen = screenMap.get(screenId);
            
            for (Explorer.ElementInfo el : screen.elements) {
                if (!el.isClickable) continue;
                if (el.id == null || el.id.equals("unknown") || testedElements.contains(el.id)) continue;
                
                testedElements.add(el.id);
                scenarioCount++;
                
                String scenarioName = "Tap " + sanitizeName(el.id) + " element";
                
                feature.append("  @elem").append(scenarioCount).append("\n");
                feature.append("  Scenario: ").append(scenarioName).append("\n");
                
                // If element is triggered by another element, document how to reach it
                if (el.triggeredBy != null) {
                    feature.append("    # This element appears after tapping: ").append(el.triggeredBy).append("\n");
                }
                
                feature.append("    Given the app is running\n");
                
                // Navigate to screen first
                if (screen.pathFromRoot != null && !screen.pathFromRoot.isEmpty()) {
                    for (Explorer.NavigationStep step : screen.pathFromRoot) {
                        feature.append("    When I tap on \"").append(step.elementTapped).append("\"\n");
                    }
                }
                
                // If triggered by another element, tap that first
                if (el.triggeredBy != null) {
                    feature.append("    When I tap on \"").append(el.triggeredBy).append("\"\n");
                }
                
                feature.append("    When I tap on \"").append(el.id).append("\"\n");
                feature.append("    Then the app should not crash\n\n");
                
                generatedScenarios.add(scenarioName);
                
                if (scenarioCount >= 30) break;
            }
            if (scenarioCount >= 30) break;
        }

        writeFeatureFile("Elements.feature", feature.toString());
    }

    private void generateFormTests() {
        StringBuilder feature = new StringBuilder();
        feature.append("@generated @forms\n");
        feature.append("Feature: Form Tests\n");
        feature.append("  Verify all discovered input fields work correctly\n\n");

        int scenarioCount = 0;
        Set<String> testedFields = new HashSet<>();

        for (String screenId : screenMap.keySet()) {
            Explorer.ScreenNode screen = screenMap.get(screenId);
            
            for (Explorer.ElementInfo el : screen.elements) {
                if (!el.isTextField) continue;
                if (el.id == null || el.id.equals("unknown") || testedFields.contains(el.id)) continue;
                
                testedFields.add(el.id);
                scenarioCount++;
                
                String testValue = generateTestValue(el.id);
                String scenarioName = "Fill " + sanitizeName(el.id) + " field";
                
                feature.append("  @form").append(scenarioCount).append("\n");
                feature.append("  Scenario: ").append(scenarioName).append("\n");
                feature.append("    Given the app is running\n");
                
                // Navigate to screen first
                if (screen.pathFromRoot != null && !screen.pathFromRoot.isEmpty()) {
                    feature.append("    # Navigate to screen: ");
                    for (Explorer.NavigationStep step : screen.pathFromRoot) {
                        feature.append(step.elementTapped).append(" → ");
                    }
                    feature.append("\n");
                    for (Explorer.NavigationStep step : screen.pathFromRoot) {
                        feature.append("    When I tap on \"").append(step.elementTapped).append("\"\n");
                    }
                }
                
                feature.append("    When I enter \"").append(testValue).append("\" in \"").append(el.id).append("\"\n");
                feature.append("    Then the field should contain \"").append(testValue).append("\"\n\n");
                
                generatedScenarios.add(scenarioName);
            }
        }

        if (scenarioCount > 0) {
            writeFeatureFile("Forms.feature", feature.toString());
        }
    }

    private void generateSmokeTestSuite() {
        StringBuilder feature = new StringBuilder();
        feature.append("@generated @smoke-generated\n");
        feature.append("Feature: Smoke Tests\n");
        feature.append("  Critical path tests with full navigation paths from exploration\n\n");

        // Find common UI patterns and elements with their paths
        List<ElementWithPath> menuElements = new ArrayList<>();
        List<ElementWithPath> cartElements = new ArrayList<>();
        List<ElementWithPath> buttonElements = new ArrayList<>();

        for (Explorer.ScreenNode screen : screenMap.values()) {
            for (Explorer.ElementInfo el : screen.elements) {
                if (el.id == null || !el.isClickable) continue;
                String id = el.id.toLowerCase();
                
                ElementWithPath ewp = new ElementWithPath(el, screen.pathFromRoot);
                
                if (id.contains("cart") || id.contains("add")) {
                    cartElements.add(ewp);
                }
                if (id.contains("menu")) {
                    menuElements.add(ewp);
                }
                if (id.contains("btn") || id.contains("button") || id.contains("bt")) {
                    buttonElements.add(ewp);
                }
            }
        }

        int scenarioCount = 0;

        // Generate menu test with path
        if (!menuElements.isEmpty()) {
            scenarioCount++;
            ElementWithPath menu = menuElements.get(0);
            feature.append("  @smoke-gen1\n");
            feature.append("  Scenario: Open and close menu\n");
            feature.append("    Given the app is running\n");
            
            // Navigate to screen where menu is
            if (menu.path != null) {
                for (Explorer.NavigationStep step : menu.path) {
                    feature.append("    When I tap on \"").append(step.elementTapped).append("\"\n");
                }
            }
            
            feature.append("    When I tap on \"").append(menu.element.id).append("\"\n");
            feature.append("    Then I should see a screen\n");
            feature.append("    And I go back\n\n");
            generatedScenarios.add("Open and close menu");
        }

        // Generate cart test with path
        if (!cartElements.isEmpty()) {
            scenarioCount++;
            feature.append("  @smoke-gen2\n");
            feature.append("  Scenario: Add item to cart\n");
            feature.append("    Given the app is running\n");
            
            ElementWithPath cart = cartElements.get(0);
            if (cart.path != null) {
                for (Explorer.NavigationStep step : cart.path) {
                    feature.append("    When I tap on \"").append(step.elementTapped).append("\"\n");
                }
            }
            feature.append("    When I tap on \"").append(cart.element.id).append("\"\n");
            feature.append("    Then the app should not crash\n\n");
            generatedScenarios.add("Add item to cart");
        }

        // Generate full journey test using discovered paths
        feature.append("  @smoke-gen3\n");
        feature.append("  Scenario: Full app journey through discovered paths\n");
        feature.append("    Given the app is running\n");
        
        // Use discovered navigation paths
        Set<String> visitedScreens = new HashSet<>();
        for (Explorer.ScreenNode screen : screenMap.values()) {
            if (screen.pathFromRoot != null && !screen.pathFromRoot.isEmpty() && visitedScreens.size() < 3) {
                String key = screen.id;
                if (visitedScreens.contains(key)) continue;
                visitedScreens.add(key);
                
                feature.append("    # Journey to: ").append(truncatePath(screen.id)).append("\n");
                for (Explorer.NavigationStep step : screen.pathFromRoot) {
                    feature.append("    When I tap on \"").append(step.elementTapped).append("\"\n");
                }
                feature.append("    Then I should see a screen\n");
                // Go back to start
                for (int i = 0; i < screen.pathFromRoot.size(); i++) {
                    feature.append("    And I go back\n");
                }
            }
        }
        feature.append("\n");
        generatedScenarios.add("Full app journey");
        scenarioCount++;

        if (scenarioCount > 0) {
            writeFeatureFile("SmokeGenerated.feature", feature.toString());
        }
    }
    
    private String truncatePath(String path) {
        return path.length() > 40 ? path.substring(0, 40) + "..." : path;
    }
    
    private static class ElementWithPath {
        Explorer.ElementInfo element;
        List<Explorer.NavigationStep> path;
        
        ElementWithPath(Explorer.ElementInfo el, List<Explorer.NavigationStep> path) {
            this.element = el;
            this.path = path;
        }
    }

    private void writeFeatureFile(String filename, String content) {
        try {
            Path path = Paths.get(outputDir + filename);
            Files.writeString(path, content);
            System.out.println("📝 Generated: " + path);
        } catch (IOException e) {
            System.err.println("Failed to write " + filename + ": " + e.getMessage());
        }
    }

    private String sanitizeName(String id) {
        if (id == null) return "unknown";
        // Remove common suffixes and clean up
        return id.replaceAll("(IV|Bt|Btn|Button|RL|TV)$", "")
                 .replaceAll("([a-z])([A-Z])", "$1 $2")
                 .toLowerCase();
    }

    private boolean shouldSkipForNav(String id) {
        if (id == null) return true;
        String lower = id.toLowerCase();
        return lower.contains("back") || lower.contains("close") || 
               lower.contains("cancel") || lower.contains("dismiss");
    }

    private String generateTestValue(String fieldId) {
        if (fieldId == null) return "test123";
        String id = fieldId.toLowerCase();
        
        if (id.contains("email")) return "test@example.com";
        if (id.contains("password")) return "Test123!";
        if (id.contains("phone")) return "1234567890";
        if (id.contains("name")) return "Test User";
        if (id.contains("address")) return "123 Test St";
        if (id.contains("search")) return "test";
        
        return "test123";
    }

    public List<String> getGeneratedScenarios() {
        return generatedScenarios;
    }
}


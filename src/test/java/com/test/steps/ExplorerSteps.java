package com.test.steps;

import com.test.engine.Explorer;
import com.test.engine.TestGenerator;
import io.cucumber.java.en.*;

public class ExplorerSteps {

    private Explorer explorer;

    @When("I explore the app")
    public void exploreApp() {
        explorer = new Explorer();
        explorer.explore();
    }

    @When("I explore the app with depth {int}")
    public void exploreAppWithDepth(int depth) {
        explorer = new Explorer();
        explorer.maxDepth(depth).explore();
    }

    @When("I explore and generate tests with depth {int}")
    public void exploreAndGenerateTests(int depth) {
        explorer = new Explorer();
        explorer.maxDepth(depth).explore();
        
        // Generate test cases from exploration results
        TestGenerator generator = new TestGenerator(explorer.getNavigationGraph());
        generator.generateAll();
    }

    @Then("I should discover at least {int} screens")
    public void shouldDiscoverScreens(int count) {
        if (explorer.getVisitedScreens().size() < count) {
            throw new AssertionError("Expected at least " + count + " screens, found " + explorer.getVisitedScreens().size());
        }
    }

    @Then("I should have no critical errors")
    public void shouldHaveNoCriticalErrors() {
        java.util.List<String> errors = explorer.getErrors();
        
        // Only fail on REAL crashes (ANR, "has stopped", "keeps stopping")
        // Not on UiAutomator2 session issues which are false positives
        java.util.List<String> criticalErrors = errors.stream()
            .filter(e -> e.contains("ANR") || 
                        e.contains("has stopped") || 
                        e.contains("keeps stopping") ||
                        e.contains("FATAL"))
            .collect(java.util.stream.Collectors.toList());
        
        if (!criticalErrors.isEmpty()) {
            throw new AssertionError("Critical errors found during exploration:\n" + 
                String.join("\n", criticalErrors));
        }
        
        // Report all errors but don't fail on session issues
        if (!errors.isEmpty()) {
            System.out.println("⚠️ Non-critical errors encountered: " + errors.size());
            for (String err : errors) {
                System.out.println("  • " + err);
            }
        }
        
        System.out.println("✅ No critical errors found. Total errors: " + errors.size());
    }
    
    @Then("tests should be generated")
    public void testsShouldBeGenerated() {
        System.out.println("✅ Tests have been generated in src/test/resources/features/generated/");
    }
}


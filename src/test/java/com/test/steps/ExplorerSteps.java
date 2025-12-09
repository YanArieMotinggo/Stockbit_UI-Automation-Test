package com.test.steps;

import com.test.engine.Explorer;
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

    @Then("I should discover at least {int} screens")
    public void shouldDiscoverScreens(int count) {
        if (explorer.getVisitedScreens().size() < count) {
            throw new AssertionError("Expected at least " + count + " screens, found " + explorer.getVisitedScreens().size());
        }
    }
}


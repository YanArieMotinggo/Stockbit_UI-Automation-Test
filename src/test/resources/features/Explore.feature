Feature: App Exploration
  Automatically discover and map the application

  Scenario: Deep exploration of all app pages
    When I explore the app with depth 30
    Then I should discover at least 1 screens
    And I should have no critical errors


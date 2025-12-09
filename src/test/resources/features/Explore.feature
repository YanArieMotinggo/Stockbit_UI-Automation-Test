Feature: App Exploration
  Automatically discover and map the application

  Scenario: Explore the entire app
    When I explore the app with depth 5
    Then I should discover at least 1 screens


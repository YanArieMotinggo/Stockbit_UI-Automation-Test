@generate
Feature: Generate Tests from Exploration
  Explores the app and writes test cases

  Scenario: Explore app and generate test cases
    When I explore and generate tests with depth 20
    Then I should discover at least 1 screens
    And I should have no critical errors
    And tests should be generated


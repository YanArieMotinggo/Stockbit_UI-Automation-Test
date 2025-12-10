@generated @navigation
Feature: Navigation Tests
  Verify all discovered screens are accessible with navigation paths

  @nav1
  Scenario: Navigate to .view.activities.main activity|no items|catalog
    # Path: sortIV → nameAscCL → cartRL → menuIV → Screen
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    When I tap on "menuIV"
    Then I should see a screen

  @nav2
  Scenario: Navigate to .view.activities.main activity|sort by:|name - ascending
    # Path: sortIV → Screen
    Given the app is running
    When I tap on "sortIV"
    Then I should see a screen

  @nav3
  Scenario: Tap name asc cl navigates correctly
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    Then I should see a screen
    And I go back

  @nav4
  Scenario: Navigate to .view.activities.main activity|no items
    # Path: sortIV → nameAscCL → cartRL → Screen
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    Then I should see a screen

  @nav5
  Scenario: Tap menu navigates correctly
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    When I tap on "menuIV"
    Then I should see a screen
    And I go back

  @nav6
  Scenario: Tap sort navigates correctly
    Given the app is running
    When I tap on "sortIV"
    Then I should see a screen
    And I go back

  @nav7
  Scenario: Tap cart navigates correctly
    Given the app is running
    When I tap on "cartRL"
    Then I should see a screen
    And I go back


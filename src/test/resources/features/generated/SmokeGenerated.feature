@generated @smoke-generated
Feature: Smoke Tests
  Critical path tests with full navigation paths from exploration

  @smoke-gen1
  Scenario: Open and close menu
    Given the app is running
    When I tap on "menuIV"
    Then I should see a screen
    And I go back

  @smoke-gen2
  Scenario: Add item to cart
    Given the app is running
    When I tap on "cartRL"
    Then the app should not crash

  @smoke-gen3
  Scenario: Full app journey through discovered paths
    Given the app is running


@generated @smoke-generated
Feature: Smoke Tests
  Critical path tests with full navigation paths from exploration

  @smoke-gen1
  Scenario: Open and close menu
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    When I tap on "menuIV"
    When I tap on "menuIV"
    Then I should see a screen
    And I go back

  @smoke-gen2
  Scenario: Add item to cart
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    When I tap on "menuIV"
    When I tap on "cartRL"
    Then the app should not crash

  @smoke-gen3
  Scenario: Full app journey through discovered paths
    Given the app is running
    # Journey to: .view.activities.MainActivity|No Items|C...
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    When I tap on "menuIV"
    Then I should see a screen
    And I go back
    And I go back
    And I go back
    And I go back
    # Journey to: .view.activities.MainActivity|Sort by:|N...
    When I tap on "sortIV"
    Then I should see a screen
    And I go back
    # Journey to: .view.activities.MainActivity|No Items
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    Then I should see a screen
    And I go back
    And I go back
    And I go back


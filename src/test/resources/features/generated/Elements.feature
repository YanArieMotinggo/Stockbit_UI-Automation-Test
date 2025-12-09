@generated @elements
Feature: Element Interaction Tests
  Verify all discovered elements are interactive
  Includes elements that appear after other actions (popups, menus)

  @elem1
  Scenario: Tap menu element
    Given the app is running
    When I tap on "menuIV"
    Then the app should not crash

  @elem2
  Scenario: Tap sort element
    Given the app is running
    When I tap on "sortIV"
    Then the app should not crash

  @elem3
  Scenario: Tap cart element
    Given the app is running
    When I tap on "cartRL"
    Then the app should not crash

  @elem4
  Scenario: Tap product element
    Given the app is running
    When I tap on "productIV"
    Then the app should not crash

  @elem5
  Scenario: Tap start1 element
    Given the app is running
    When I tap on "start1IV"
    Then the app should not crash

  @elem6
  Scenario: Tap start2 element
    Given the app is running
    When I tap on "start2IV"
    Then the app should not crash

  @elem7
  Scenario: Tap start3 element
    Given the app is running
    When I tap on "start3IV"
    Then the app should not crash

  @elem8
  Scenario: Tap start4 element
    Given the app is running
    When I tap on "start4IV"
    Then the app should not crash

  @elem9
  Scenario: Tap start5 element
    Given the app is running
    When I tap on "start5IV"
    Then the app should not crash

  @elem10
  Scenario: Tap null element
    Given the app is running
    When I tap on "null"
    Then the app should not crash


@generated @elements
Feature: Element Interaction Tests
  Verify all discovered elements are interactive
  Includes elements that appear after other actions (popups, menus)

  @elem1
  Scenario: Tap menu element
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    When I tap on "menuIV"
    When I tap on "menuIV"
    Then the app should not crash

  @elem2
  Scenario: Tap cart element
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    When I tap on "menuIV"
    When I tap on "cartRL"
    Then the app should not crash

  @elem3
  Scenario: Tap shopping element
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    When I tap on "menuIV"
    When I tap on "shoppingBt"
    Then the app should not crash

  @elem4
  Scenario: Tap item element
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    When I tap on "cartRL"
    When I tap on "menuIV"
    When I tap on "itemTV"
    Then the app should not crash

  @elem5
  Scenario: Tap name asc cl element
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameAscCL"
    Then the app should not crash

  @elem6
  Scenario: Tap name des cl element
    Given the app is running
    When I tap on "sortIV"
    When I tap on "nameDesCL"
    Then the app should not crash

  @elem7
  Scenario: Tap price asc cl element
    Given the app is running
    When I tap on "sortIV"
    When I tap on "priceAscCL"
    Then the app should not crash

  @elem8
  Scenario: Tap price des cl element
    Given the app is running
    When I tap on "sortIV"
    When I tap on "priceDesCL"
    Then the app should not crash

  @elem9
  Scenario: Tap sort element
    Given the app is running
    When I tap on "sortIV"
    Then the app should not crash

  @elem10
  Scenario: Tap product element
    Given the app is running
    When I tap on "productIV"
    Then the app should not crash

  @elem11
  Scenario: Tap start1 element
    Given the app is running
    When I tap on "start1IV"
    Then the app should not crash

  @elem12
  Scenario: Tap start2 element
    Given the app is running
    When I tap on "start2IV"
    Then the app should not crash

  @elem13
  Scenario: Tap start3 element
    Given the app is running
    When I tap on "start3IV"
    Then the app should not crash

  @elem14
  Scenario: Tap start4 element
    Given the app is running
    When I tap on "start4IV"
    Then the app should not crash

  @elem15
  Scenario: Tap start5 element
    Given the app is running
    When I tap on "start5IV"
    Then the app should not crash


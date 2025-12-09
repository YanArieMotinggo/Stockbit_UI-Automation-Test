Feature: Shopping Flow Tests
  Test the shopping functionality using discovered elements

  @smoke
  Scenario: Add product to cart
    Given I am on the products page
    When I tap on "productIV"
    Then I should see "Sauce Lab Back Packs"
    When I tap on "cartBt"
    Then the cart should have 1 item

  @smoke  
  Scenario: Rate a product
    Given I am on the products page
    When I tap on "productIV"
    And I tap on "start3IV"
    Then the product should have 3 star rating

  @regression
  Scenario: Change product quantity
    Given I am on a product detail page
    When I tap on "plusIV" 3 times
    Then the quantity should be 4
    When I tap on "minusIV" 2 times
    Then the quantity should be 2

  @navigation
  Scenario: Open menu and navigate
    Given I am on the products page
    When I tap on "menuIV"
    Then I should see the menu options
    When I tap on "closeBt"
    Then I should be back on products page


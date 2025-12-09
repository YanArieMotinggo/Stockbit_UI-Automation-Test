package com.test.screens;

import com.test.engine.Session;

public class ProductsScreen {

    private final Session app = Session.current();

    private static final String PRODUCT_ITEM = "id:productIV";
    private static final String MENU_BUTTON = "id:menuIV";
    private static final String SORT_BUTTON = "id:sortIV";
    private static final String CART_ICON = "id:cartIV";
    private static final String PRODUCTS_TITLE = "text:Products";

    public void openProduct(int index) {
        // Tap on product by index
        app.tap(PRODUCT_ITEM);
    }

    public void openMenu() {
        app.tap(MENU_BUTTON);
    }

    public void openSort() {
        app.tap(SORT_BUTTON);
    }

    public void openCart() {
        app.tap(CART_ICON);
    }

    public boolean isDisplayed() {
        return app.exists(PRODUCTS_TITLE);
    }

    public void waitForLoad() {
        app.find(PRODUCTS_TITLE);
    }
}


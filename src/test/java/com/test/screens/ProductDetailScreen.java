package com.test.screens;

import com.test.engine.Session;

public class ProductDetailScreen {

    private final Session app = Session.current();

    private static final String ADD_TO_CART = "id:cartBt";
    private static final String MENU_BUTTON = "id:menuIV";
    private static final String STAR_PREFIX = "id:start";
    private static final String MINUS_QTY = "id:minusIV";
    private static final String PLUS_QTY = "id:plusIV";
    private static final String BACK_BUTTON = "id:backIV";

    public void addToCart() {
        app.tap(ADD_TO_CART);
    }

    public void setRating(int stars) {
        if (stars >= 1 && stars <= 5) {
            app.tap(STAR_PREFIX + stars + "IV");
        }
    }

    public void increaseQuantity() {
        app.tap(PLUS_QTY);
    }

    public void decreaseQuantity() {
        app.tap(MINUS_QTY);
    }

    public void setQuantity(int qty) {
        // Reset to 1, then increase
        for (int i = 1; i < qty; i++) {
            increaseQuantity();
            app.pause(1);
        }
    }

    public void goBack() {
        app.tap(BACK_BUTTON);
    }

    public void openMenu() {
        app.tap(MENU_BUTTON);
    }

    public boolean isDisplayed() {
        return app.exists(ADD_TO_CART);
    }

    public void waitForLoad() {
        app.find(ADD_TO_CART);
    }
}


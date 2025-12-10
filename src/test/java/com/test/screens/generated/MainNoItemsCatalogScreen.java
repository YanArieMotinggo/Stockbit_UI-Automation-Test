package com.test.screens.generated;

import com.test.engine.Session;

public class MainNoItemsCatalogScreen {

    private final Session app = Session.current();

    private static final String MENUIV = "id:menuIV";
    private static final String CARTRL = "id:cartRL";
    private static final String SHOPPINGBT = "id:shoppingBt";
    private static final String ITEMTV = "id:itemTV";
    private static final String NOITEMTITLETV = "id:noItemTitleTV";

    public void tapMenu() {
        app.tap(MENUIV);
    }

    public void tapCart() {
        app.tap(CARTRL);
    }

    public void tapShopping() {
        app.tap(SHOPPINGBT);
    }

    public void tapItem() {
        app.tap(ITEMTV);
    }

    public void navigateHere() {
        app.tap("id:sortIV");
        app.pause(1);
        app.tap("id:nameAscCL");
        app.pause(1);
        app.tap("id:cartRL");
        app.pause(1);
        app.tap("id:menuIV");
        app.pause(1);
    }

    public boolean isDisplayed() {
        return app.exists(CARTRL);
    }
}

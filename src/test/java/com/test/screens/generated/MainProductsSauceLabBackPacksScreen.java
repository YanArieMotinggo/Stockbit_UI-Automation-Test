package com.test.screens.generated;

import com.test.engine.Session;

public class MainProductsSauceLabBackPacksScreen {

    private final Session app = Session.current();

    private static final String MENUIV = "id:menuIV";
    private static final String SORTIV = "id:sortIV";
    private static final String CARTRL = "id:cartRL";
    private static final String PRODUCTIV = "id:productIV";
    private static final String START1IV = "id:start1IV";
    private static final String START2IV = "id:start2IV";
    private static final String START3IV = "id:start3IV";
    private static final String START4IV = "id:start4IV";
    private static final String START5IV = "id:start5IV";
    private static final String PRODUCTTV = "id:productTV";
    private static final String TITLETV = "id:titleTV";
    private static final String PRICETV = "id:priceTV";
    private static final String ITEMTV = "id:itemTV";

    public void tapMenu() {
        app.tap(MENUIV);
    }

    public void tapSort() {
        app.tap(SORTIV);
    }

    public void tapCart() {
        app.tap(CARTRL);
    }

    public void tapProduct() {
        app.tap(PRODUCTIV);
    }

    public void tapStart1() {
        app.tap(START1IV);
    }

    public void tapStart2() {
        app.tap(START2IV);
    }

    public void tapStart3() {
        app.tap(START3IV);
    }

    public void tapStart4() {
        app.tap(START4IV);
    }

    public void tapStart5() {
        app.tap(START5IV);
    }

    public void tapItem() {
        app.tap(ITEMTV);
    }

    public boolean isDisplayed() {
        return app.exists(START1IV);
    }
}

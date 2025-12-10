package com.test.screens.generated;

import com.test.engine.Session;

public class MainSortbyNameAscendingScreen {

    private final Session app = Session.current();

    private static final String NAMEASCCL = "id:nameAscCL";
    private static final String NAMEDESCL = "id:nameDesCL";
    private static final String PRICEASCCL = "id:priceAscCL";
    private static final String PRICEDESCL = "id:priceDesCL";
    private static final String SORTTV = "id:sortTV";

    public void tapNameAscCL() {
        app.tap(NAMEASCCL);
    }

    public void tapNameDesCL() {
        app.tap(NAMEDESCL);
    }

    public void tapPriceAscCL() {
        app.tap(PRICEASCCL);
    }

    public void tapPriceDesCL() {
        app.tap(PRICEDESCL);
    }

    public void navigateHere() {
        app.tap("id:sortIV");
        app.pause(1);
    }

    public boolean isDisplayed() {
        return app.exists(PRICEASCCL);
    }
}

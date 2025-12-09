package com.test.engine;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class Screen {

    protected final Session app = Session.current();

    protected abstract String identifier();

    public boolean isDisplayed() {
        return app.exists(identifier());
    }

    public void verifyVisible() {
        assertThat(isDisplayed())
            .as("Screen should be visible (looking for: %s)", identifier())
            .isTrue();
    }

    public static <T extends Screen> T on(Supplier<T> screenSupplier) {
        return screenSupplier.get();
    }
}


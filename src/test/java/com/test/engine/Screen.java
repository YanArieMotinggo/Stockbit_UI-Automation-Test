package com.test.engine;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight screen representation.
 * Define screens as simple classes with locators + actions.
 */
public abstract class Screen {
    protected final Session app = Session.current();

    /**
     * Override to define what element indicates this screen is visible.
     */
    protected abstract String identifier();

    public boolean isDisplayed() {
        return app.exists(identifier());
    }

    public void verifyVisible() {
        assertThat(isDisplayed())
            .as("Screen should be visible (looking for: %s)", identifier())
            .isTrue();
    }

    // Factory helper for lazy instantiation
    public static <T extends Screen> T on(Supplier<T> screenSupplier) {
        return screenSupplier.get();
    }
}


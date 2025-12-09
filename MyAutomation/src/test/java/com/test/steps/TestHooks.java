package com.test.steps;

import com.test.engine.Session;
import io.cucumber.java.After;
import io.cucumber.java.Before;

public class TestHooks {

    @Before
    public void start() {
        Session.current().open();
        Session.current().pause(2); // wait for app splash
    }

    @After
    public void stop() {
        Session.current().close();
    }
}


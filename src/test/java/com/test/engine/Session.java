package com.test.engine;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class Session {
    private static Session instance;
    private AndroidDriver driver;
    private WebDriverWait waiter;
    private final Properties props;

    private Session() {
        props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            if (in != null) props.load(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Session current() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public void open() {
        if (driver != null) return;
        try {
            var opts = new UiAutomator2Options()
                .setDeviceName(props.getProperty("device"))
                .setPlatformVersion(props.getProperty("version"))
                .setAutoGrantPermissions(true)
                .setNewCommandTimeout(Duration.ofSeconds(300))
                .setAppWaitDuration(Duration.ofSeconds(90))
                .setUiautomator2ServerLaunchTimeout(Duration.ofSeconds(90))
                .setUiautomator2ServerInstallTimeout(Duration.ofSeconds(90))
                .setAdbExecTimeout(Duration.ofSeconds(60));

            // Use APK path if provided, otherwise use package/activity
            String appPath = props.getProperty("app");
            if (appPath != null && !appPath.isEmpty()) {
                opts.setApp(System.getProperty("user.dir") + "/" + appPath);
                // Set app wait activity to handle splash screen
                opts.setAppWaitActivity("*");
            } else {
                opts.setAppPackage(props.getProperty("package"));
                opts.setAppActivity(props.getProperty("activity"));
            }

            driver = new AndroidDriver(new URL(props.getProperty("appium")), opts);
            int wait = Integer.parseInt(props.getProperty("wait", "15"));
            waiter = new WebDriverWait(driver, Duration.ofSeconds(wait));
        } catch (Exception e) {
            throw new RuntimeException("Cannot start session", e);
        }
    }

    public void close() {
        if (driver != null) {
            driver.quit();
            driver = null;
            waiter = null;
        }
    }

    public WebElement find(String locator) {
        return waiter.until(ExpectedConditions.presenceOfElementLocated(parse(locator)));
    }

    public List<WebElement> findAll(String locator) {
        return driver.findElements(parse(locator));
    }

    public boolean exists(String locator) {
        return !findAll(locator).isEmpty();
    }

    public void tap(String locator) {
        waiter.until(ExpectedConditions.elementToBeClickable(parse(locator))).click();
    }

    public void type(String locator, String text) {
        WebElement el = waiter.until(ExpectedConditions.visibilityOfElementLocated(parse(locator)));
        el.clear();
        el.sendKeys(text);
    }

    public String read(String locator) {
        return waiter.until(ExpectedConditions.visibilityOfElementLocated(parse(locator))).getText();
    }

    public void pause(int seconds) {
        try { Thread.sleep(seconds * 1000L); } catch (InterruptedException ignored) {}
    }

    private By parse(String locator) {
        if (!locator.contains(":")) {
            return By.id(locator); // default to ID
        }
        String[] parts = locator.split(":", 2);
        String type = parts[0].toLowerCase();
        String value = parts[1];

        return switch (type) {
            case "id" -> By.id(value);
            case "xpath" -> By.xpath(value);
            case "class" -> By.className(value);
            case "text" -> By.xpath("//*[@text='" + value + "']");
            case "content-desc", "desc" -> By.xpath("//*[@content-desc='" + value + "']");
            default -> By.id(locator);
        };
    }

    public AndroidDriver driver() {
        return driver;
    }
}


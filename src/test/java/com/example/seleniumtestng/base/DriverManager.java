package com.example.seleniumtestng.base;

import org.openqa.selenium.WebDriver;

/**
 * Thread-safe manager for WebDriver instances.
 * Enables parallel test execution across threads.
 */
public final class DriverManager {
    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver getDriver() {
        return DRIVER_THREAD_LOCAL.get();
    }

    public static void setDriver(WebDriver driver) {
        DRIVER_THREAD_LOCAL.set(driver);
    }

    public static boolean hasDriver() {
        return DRIVER_THREAD_LOCAL.get() != null;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER_THREAD_LOCAL.remove();
            }
        }
    }
}


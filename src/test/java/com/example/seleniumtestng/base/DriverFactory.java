package com.example.seleniumtestng.base;

import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public final class DriverFactory {
    private DriverFactory() {
    }

    public static WebDriver create(String browser) {
        String normalized = normalizeBrowser(browser);
        WebDriver driver;
        switch (normalized) {
            case "firefox":
                driver = new FirefoxDriver();
                break;
            case "edge":
                driver = new EdgeDriver();
                break;
            case "chrome":
            default:
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--no-first-run", "--no-default-browser-check");
                driver = new ChromeDriver(options);
                break;
        }
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().window().maximize();
        return driver;
    }

    private static String normalizeBrowser(String browser) {
        if (browser == null || browser.trim().isEmpty()) {
            return "chrome";
        }
        return browser.trim().toLowerCase();
    }
}

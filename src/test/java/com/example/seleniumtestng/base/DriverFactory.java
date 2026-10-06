package com.example.seleniumtestng.base;

import com.example.seleniumtestng.config.ConfigReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public final class DriverFactory {
    private DriverFactory() {
    }

    public static WebDriver create(String browser) {
        String normalized = resolveBrowser(browser);
        boolean headless = ConfigReader.getBoolean("HEADLESS", false);
        configureDriverPath(normalized);
        WebDriver driver;
        switch (normalized) {
            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (headless) {
                    firefoxOptions.addArguments("-headless");
                }
                driver = new FirefoxDriver(firefoxOptions);
                break;
            case "edge":
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--no-first-run", "--no-default-browser-check");
                if (headless) {
                    edgeOptions.addArguments("--headless=new", "--disable-gpu", "--window-size=1920,1080");
                }
                driver = new EdgeDriver(edgeOptions);
                break;
            case "chrome":
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--no-first-run", "--no-default-browser-check");
                if (headless) {
                    options.addArguments("--headless=new", "--disable-gpu", "--window-size=1920,1080");
                }
                driver = new ChromeDriver(options);
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser: " + normalized + ". Use chrome, firefox, or edge.");
        }
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().window().maximize();
        return driver;
    }

    private static String resolveBrowser(String testNgBrowser) {
        return normalizeBrowser(firstNonBlank(
                ConfigReader.get("browser"),
                ConfigReader.get("BROWSER"),
                testNgBrowser,
                "chrome"));
    }

    private static void configureDriverPath(String browser) {
        switch (browser) {
            case "chrome":
                setDriverPathIfConfigured("CHROME_DRIVER_PATH", "webdriver.chrome.driver");
                break;
            case "firefox":
                setDriverPathIfConfigured("FIREFOX_DRIVER_PATH", "webdriver.gecko.driver");
                break;
            case "edge":
                setDriverPathIfConfigured("EDGE_DRIVER_PATH", "webdriver.edge.driver");
                break;
            default:
                break;
        }
    }

    private static void setDriverPathIfConfigured(String configKey, String systemPropertyKey) {
        String configuredPath = ConfigReader.get(configKey);
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            return;
        }
        Path driverPath = Path.of(configuredPath.trim());
        if (!Files.isRegularFile(driverPath)) {
            throw new IllegalStateException(configKey + " does not point to a driver executable: " + driverPath);
        }
        System.setProperty(systemPropertyKey, driverPath.toAbsolutePath().toString());
    }

    private static String normalizeBrowser(String browser) {
        String normalized = browser.trim().toLowerCase();
        switch (normalized) {
            case "googlechrome":
            case "google-chrome":
                return "chrome";
            case "ff":
                return "firefox";
            case "msedge":
            case "microsoftedge":
            case "microsoft-edge":
                return "edge";
            default:
                return normalized;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "chrome";
    }
}

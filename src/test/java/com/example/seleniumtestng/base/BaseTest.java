package com.example.seleniumtestng.base;

import com.example.seleniumtestng.config.ConfigReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

public abstract class BaseTest {
    protected WebDriver driver;

    public WebDriver getDriver() {
        return DriverManager.hasDriver() ? DriverManager.getDriver() : this.driver;
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters("browser")
    public void setUp(@Optional("") String browser) {
        WebDriver createdDriver = DriverFactory.create(browser);
        DriverManager.setDriver(createdDriver);
        this.driver = createdDriver;
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        WebDriver currentDriver = getDriver();
        if (currentDriver == null) {
            return;
        }
        if (!result.isSuccess()) {
            saveScreenshot(currentDriver, result.getMethod().getMethodName());
        }
        if (ConfigReader.getBoolean("KEEP_BROWSER_OPEN", false)) {
            return;
        }
        DriverManager.quitDriver();
        this.driver = null;
    }

    protected String url(String app, String path) {
        String key = app.toUpperCase() + "_BASE_URL";
        String base = ConfigReader.required(key);
        return base + path;
    }

    protected void saveScreenshot(String name) {
        saveScreenshot(getDriver(), name);
    }

    protected void saveScreenshot(WebDriver targetDriver, String name) {
        if (!(targetDriver instanceof TakesScreenshot)) {
            return;
        }
        try {
            Files.createDirectories(Path.of("screenshots"));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path target = Path.of("screenshots", name + "_" + timestamp + ".png");
            byte[] bytes = ((TakesScreenshot) targetDriver).getScreenshotAs(OutputType.BYTES);
            Files.write(target, bytes);
        } catch (IOException | RuntimeException ignored) {
        }
    }
}

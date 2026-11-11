package com.example.seleniumtestng.listeners;

import com.example.seleniumtestng.base.DriverManager;
import com.example.seleniumtestng.utils.Log;
import io.qameta.allure.Attachment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Global TestNG Listener for automated lifecycle logging,
 * automatic failure screenshot capture and Allure report attachment.
 */
public class TestListener implements ITestListener {

    @Override
    public void onStart(ITestContext context) {
        Log.info("====================================================================");
        Log.info("STARTING TEST SUITE: {}", context.getName());
        Log.info("====================================================================");
    }

    @Override
    public void onFinish(ITestContext context) {
        Log.info("====================================================================");
        Log.info("FINISHED TEST SUITE: {}", context.getName());
        Log.info("Summary: Passed={}, Failed={}, Skipped={}",
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
        Log.info("====================================================================");
    }

    @Override
    public void onTestStart(ITestResult result) {
        Log.info("--> [START] {}.{}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        Log.info("--> [PASS]  {}.{} ({} ms)",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                result.getEndMillis() - result.getStartMillis());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Log.error("--> [FAIL]  {}.{} - Error: {}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "Unknown error");

        WebDriver driver = DriverManager.getDriver();
        if (driver != null) {
            byte[] screenshotBytes = captureScreenshot(driver, result.getMethod().getMethodName());
            if (screenshotBytes != null) {
                attachScreenshotToAllure(screenshotBytes);
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Log.warn("--> [SKIP]  {}.{}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName());
    }

    @Attachment(value = "Failure Screenshot", type = "image/png")
    public byte[] attachScreenshotToAllure(byte[] screenshot) {
        return screenshot;
    }

    private byte[] captureScreenshot(WebDriver driver, String testName) {
        if (!(driver instanceof TakesScreenshot)) {
            return null;
        }
        try {
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.createDirectories(Path.of("screenshots"));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path target = Path.of("screenshots", testName + "_" + timestamp + ".png");
            Files.write(target, bytes);
            return bytes;
        } catch (IOException | RuntimeException e) {
            Log.error("Failed to capture failure screenshot: {}", e.getMessage());
            return null;
        }
    }
}


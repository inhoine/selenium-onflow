package com.example.seleniumtestng.listeners;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.utils.Log;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * TestNG Retry Analyzer to automatically rerun flaky tests.
 * Configured via TEST_RETRY_COUNT (default 0 = disabled).
 */
public class RetryAnalyzer implements IRetryAnalyzer {
    private int counter = 0;
    private final int maxRetry = Integer.parseInt(ConfigReader.getOrDefault("TEST_RETRY_COUNT", "0"));

    @Override
    public boolean retry(ITestResult result) {
        if (counter < maxRetry) {
            counter++;
            Log.warn("Retrying flaky test: {}.{} (Attempt {}/{})",
                    result.getTestClass().getRealClass().getSimpleName(),
                    result.getMethod().getMethodName(),
                    counter,
                    maxRetry);
            return true;
        }
        return false;
    }
}


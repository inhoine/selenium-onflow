package com.example.seleniumtestng.utils;

import com.example.seleniumtestng.config.AccountConfig;
import com.example.seleniumtestng.pages.LoginPage;
import java.time.Duration;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

@SuppressWarnings("null")
public final class AuthHelper {
    private AuthHelper() {
    }

    public static void loginOms(WebDriver driver) {
        AccountConfig.Credentials credentials = AccountConfig.oms();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.waitForLoginForm();
        loginPage.login(credentials.email(), credentials.password());
    }

    public static void loginOps(WebDriver driver) {
        AccountConfig.Credentials credentials = AccountConfig.ops();
        LoginPage loginPage = new LoginPage(driver);
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> {
            String currentUrl = String.valueOf(d.getCurrentUrl());
            return !currentUrl.contains("/login") || !d.findElements(org.openqa.selenium.By.cssSelector("input[type='email'], input[placeholder*='email']")).isEmpty();
        });
        if (String.valueOf(driver.getCurrentUrl()).contains("/login")) {
            loginPage.login(credentials.email(), credentials.password());
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> !String.valueOf(d.getCurrentUrl()).contains("/login"));
        }
    }

    public static String loginWms(WebDriver driver) {
        AccountConfig.Credentials credentials = AccountConfig.wms();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.waitForLoginForm();
        loginPage.login(credentials.email(), credentials.password());
        loginPage.continueLoginIfNeeded();
        boolean selectedFc = loginPage.selectFcIfPresent(8000);
        System.out.println(selectedFc ? "Selected WMS FC" : "WMS FC selector not shown; continuing");
        return getWmsToken(driver);
    }

    public static String getWmsToken(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> {
                    Object token = ((JavascriptExecutor) d).executeScript("return localStorage.getItem('token');");
                    String normalized = normalizeToken(token);
                    return normalized.isBlank() ? null : normalized;
                });
    }

    private static String normalizeToken(Object token) {
        if (token == null) {
            return "";
        }
        String value = token.toString().trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}

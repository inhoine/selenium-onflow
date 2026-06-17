package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.config.AccountConfig;
import com.example.seleniumtestng.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {
    @DataProvider(name = "loginScenarios")
    public Object[][] loginScenarios() {
        AccountConfig.Credentials wms = AccountConfig.wms();
        return new Object[][]{
                {"valid WMS login", wms.email(), wms.password(), true, null},
                {"wrong password", wms.email(), "IncorrectPassword123!", false, "mật khẩu"},
                {"invalid email format", "invalid-email-format", wms.password(), false, "email"},
                {"missing password", wms.email(), "", false, "vui lòng"},
                {"missing email", "", wms.password(), false, "vui lòng"},
                {"blank credentials", "", "", false, "vui lòng"}
        };
    }

    @Test(dataProvider = "loginScenarios")
    public void wmsLoginScenarios(String name, String email, String password, boolean shouldSucceed, String expectedErrorContains) {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.openWmsLogin();
        loginPage.login(email, password);
        loginPage.continueLoginIfNeeded();

        if (shouldSucceed) {
            loginPage.selectFc();
            loginPage.waitForLoginSuccess();
            Assert.assertFalse(String.valueOf(driver.getCurrentUrl()).contains("/login"), name);
            return;
        }

        loginPage.waitForLoginFailure();
        Assert.assertTrue(loginPage.isLoginFormVisible(), name + " should stay on login screen");

        String errorText = loginPage.getLoginErrorText();
        if (expectedErrorContains != null && !errorText.isBlank()) {
            Assert.assertTrue(
                    errorText.toLowerCase().contains(expectedErrorContains.toLowerCase()),
                    "Expected error containing '" + expectedErrorContains + "', got '" + errorText + "'");
        }
    }
}

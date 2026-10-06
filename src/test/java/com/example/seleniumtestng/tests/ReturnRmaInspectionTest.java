package com.example.seleniumtestng.tests;
import com.example.seleniumtestng.base.BaseTest;

import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.models.RmaReturnOrder;
import com.example.seleniumtestng.pages.ReturnRmaInspectionPage;
import java.util.List;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ReturnRmaInspectionTest extends BaseTest {
    private ReturnRmaInspectionPage returnRmaInspectionPage;
    private WmsApiClient wmsApiClient;
    private String token;

    @BeforeMethod(alwaysRun = true)
    public void loginToWms() {
        driver.manage().deleteAllCookies();
        driver.get(url("WMS", "/login"));
        clearBrowserStorage();
        token = AuthHelper.loginWms(driver);
        returnRmaInspectionPage = new ReturnRmaInspectionPage(driver);
        wmsApiClient = new WmsApiClient();
    }

    @Test
    public void inspectRmaReturnOrders() {
        String rmaCode = ConfigReader.getOrDefault("RMA_CODE", "RMA-330496");
        String tableCode = ConfigReader.getOrDefault(
                "RMA_INSPECTION_TABLE_CODE",
                ConfigReader.getOrDefault("DEFAULT_PACKING_TABLE_CODE", "PACK02"));

        returnRmaInspectionPage.openOrderRmaList();
        List<RmaReturnOrder> orders = wmsApiClient.getRmaReturnOrders(rmaCode, token);
        Assert.assertFalse(orders.isEmpty(), "RMA API should return at least one tracking_code for " + rmaCode);

        returnRmaInspectionPage.openInspectionPage();
        returnRmaInspectionPage.scanTableIfPresent(tableCode);
        int inspected = returnRmaInspectionPage.inspectOrders(rmaCode, orders);

        Assert.assertEquals(
                inspected,
                orders.size(),
                "Automation should process every RMA tracking_code returned by API");
    }

    private void clearBrowserStorage() {
        ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
    }
}

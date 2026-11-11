package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.flows.ConvertGoodsFlow;
import com.example.seleniumtestng.models.ConvertGoodsRequestData;
import com.example.seleniumtestng.pages.ConvertGoodsPage;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Epic("WMS - Sản Phẩm")
@Feature("Chuyển Đổi Hàng Hóa (Convert Goods)")
public class ConvertGoodsTest extends BaseTest {
    private ConvertGoodsPage convertGoodsPage;

    @BeforeMethod(alwaysRun = true)
    public void loginToWms() {
        driver.manage().deleteAllCookies();
        driver.get(url("WMS", "/login"));
        clearBrowserStorage();
        AuthHelper.loginWms(driver);
        convertGoodsPage = new ConvertGoodsPage(driver);
    }

    @Test(description = "Verify creation of convert goods request in WMS")
    @Story("Tạo yêu cầu chuyển đổi vị trí và phân loại hàng hóa")
    @Severity(SeverityLevel.NORMAL)
    public void createConvertGoodsRequest() {
        ConvertGoodsRequestData data = defaultRequestData();

        new ConvertGoodsFlow(driver).createConvertGoodsRequest(data);

        Assert.assertTrue(convertGoodsPage.isAtDetailPage(),
                "WMS should open convert goods detail after submitting request. Current URL: "
                        + driver.getCurrentUrl());
    }

    private void clearBrowserStorage() {
        ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
    }

    private ConvertGoodsRequestData defaultRequestData() {
        String runId = String.valueOf(System.currentTimeMillis());
        return new ConvertGoodsRequestData(
                ConfigReader.getOrDefault("CONVERT_GOODS_PRODUCT_CODE", "MHMSI"),
                Integer.parseInt(ConfigReader.getOrDefault("CONVERT_GOODS_QUANTITY", "2")),
                ConfigReader.getOrDefault("CONVERT_GOODS_CURRENT_TYPE", "A"),
                ConfigReader.getOrDefault("CONVERT_GOODS_TARGET_TYPE", "D1"),
                ConfigReader.getOrDefault("CONVERT_GOODS_CURRENT_LOCATION", "VI-TRI-CHUA-2"),
                ConfigReader.getOrDefault("CONVERT_GOODS_PENDING_LOCATION", "C-001"),
                ConfigReader.getOrDefault("CONVERT_GOODS_REASON", "Automation convert goods request") + " " + runId,
                ConfigReader.getOrDefault("CONVERT_GOODS_APPROVAL_REASON", "Automation approve convert goods request") + " " + runId,
                ConfigReader.getOrDefault("CONVERT_GOODS_PUTAWAY_LOCATION", "TEST-D1-0300"));
    }
}

package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.flows.PickingPreparationFlow;
import com.example.seleniumtestng.models.CampaignAwbList;
import com.example.seleniumtestng.models.CampaignAwbTracking;
import com.example.seleniumtestng.models.OrderAwbInfo;
import com.example.seleniumtestng.models.PickupDetail;
import com.example.seleniumtestng.pages.PickAndPackOrderPage;
import com.example.seleniumtestng.pages.PackingCampaignPage;
import com.example.seleniumtestng.utils.AwbPdfVerifier;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PackingCampaignTest extends BaseTest {
    private PackingCampaignPage packingCampaignPage;
    private String token;

    @BeforeMethod(alwaysRun = true)
    public void loginToWms() {
        driver.manage().deleteAllCookies();
        driver.get(url("WMS", "/login"));
        clearBrowserStorage();
        token = AuthHelper.loginWms(driver);
        waitForManualPermissionAllow();
        packingCampaignPage = new PackingCampaignPage(driver);
    }

    @Test
    public void scanPackingCampaignPickup() {
        String tableCode = ConfigReader.getOrDefault(
                "PACKING_CAMPAIGN_TABLE_CODE",
                ConfigReader.required("DEFAULT_PACKING_TABLE_CODE"));
        String pickupCode = ConfigReader.required("PACKING_CAMPAIGN_PICKUP_CODE");
        String packingMaterialCode = ConfigReader.getOrDefault(
                "PACKING_CAMPAIGN_PACKING_MATERIAL_CODE",
                ConfigReader.getOrDefault("DEFAULT_PACKING_MATERIAL_CODE", "40x20x20"));

        WmsApiClient wmsApiClient = new WmsApiClient();
        PickupDetail pickupDetail = new PickingPreparationFlow(driver).prepareForPacking(pickupCode, token);
        receivePackingTrolleyIfNeeded(pickupCode, pickupDetail);
        packingCampaignPage.openCampaignForPacking(tableCode, pickupCode);

        Assert.assertTrue(packingCampaignPage.isCampaignContextOpen(pickupCode),
                "WMS should open packing campaign context after scanning pickup "
                        + pickupCode
                        + ". Current URL: "
                        + driver.getCurrentUrl());
        Assert.assertTrue(
                packingCampaignPage.isPackingMaterialScannerOpen()
                        || packingCampaignPage.isOrderConfirmationListOpen(),
                "WMS should show packing material scanner or order confirmation list after scanning pickup "
                        + pickupCode);

        packingCampaignPage.scanPackingMaterialIfPresent(packingMaterialCode);
        CampaignAwbList campaignAwbList = wmsApiClient.getCampaignAwbList(pickupCode, token);
        new AwbPdfVerifier().verifyCampaignAwbPdfs(
                campaignAwbList,
                orderAwbInfos(wmsApiClient, campaignAwbList, token));
        List<String> orderCodes = packingCampaignPage.waitForAllAwbBillsPrintedAndScanOrders();
        Assert.assertFalse(orderCodes.isEmpty(), "Packing campaign should scan at least one order after AWB bill printed");
    }

    private List<OrderAwbInfo> orderAwbInfos(
            WmsApiClient wmsApiClient,
            CampaignAwbList campaignAwbList,
            String token) {
        List<OrderAwbInfo> infos = new ArrayList<>();
        for (CampaignAwbTracking tracking : campaignAwbList.trackings()) {
            infos.add(wmsApiClient.getOrderAwbInfo(tracking.trackingCode(), token));
        }
        return infos;
    }

    private void clearBrowserStorage() {
        ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
    }

    private void receivePackingTrolleyIfNeeded(String pickupCode, PickupDetail pickupDetail) {
        if (isBasketPickup(pickupDetail)) {
            System.out.println("Skip receive packing trolley for basket pickup type: " + pickupDetail.pickupType());
            return;
        }

        PickAndPackOrderPage pickAndPackOrderPage = new PickAndPackOrderPage(driver);
        pickAndPackOrderPage.receivePackingTrolley(pickupCode);
        pickAndPackOrderPage.verifyToastMessageIfPresent("Nhận bảng kê thành công", 5000);
    }

    private boolean isBasketPickup(PickupDetail pickupDetail) {
        String pickupType = pickupDetail.pickupType() == null ? "" : pickupDetail.pickupType().trim().toLowerCase();
        return pickupType.contains("sso") || pickupType.contains("mso");
    }

    private void waitForManualPermissionAllow() {
        long waitMillis = Long.parseLong(ConfigReader.getOrDefault(
                "PACKING_CAMPAIGN_PERMISSION_WAIT_MS",
                "5000"));
        if (waitMillis <= 0) {
            return;
        }
        System.out.println("Waiting " + waitMillis + "ms for manual browser permission allow");
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for browser permission allow", e);
        }
    }
}

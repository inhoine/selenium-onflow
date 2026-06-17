package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.pages.EquipmentPage;
import com.example.seleniumtestng.pages.PickAndPackOrderPage;
import java.util.List;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PickPackTest extends BaseTest {
    @Test
    public void pickAndPackPickupOrder() {
        String pickupId = ConfigReader.required("DEFAULT_PICKUP_ID");
        String packingMaterialCode = ConfigReader.getOrDefault("DEFAULT_PACKING_MATERIAL_CODE", "40x20x20");
        System.out.println("PickPack pickupId=" + pickupId + ", packingMaterialCode=" + packingMaterialCode);

        driver.manage().deleteAllCookies();
        driver.get(url("WMS", "/login"));
        clearBrowserStorage();
        String token = AuthHelper.loginWms(driver);

        WmsApiClient wmsApiClient = new WmsApiClient();
        EquipmentPage equipmentPage = new EquipmentPage(driver);
        PickAndPackOrderPage pickAndPackOrderPage = new PickAndPackOrderPage(driver);

        driver.get(url("WMS", "/equipments?page=1&page_size=50"));
        String equipmentCode = equipmentPage.addEquipment(
                ConfigReader.required("DEFAULT_EQUIPMENT_GROUP"),
                ConfigReader.required("DEFAULT_EQUIPMENT_TYPE"));
        String equipmentToast = equipmentPage.waitForToast("Thêm thiết bị chứa hàng thành công");
        Assert.assertFalse(equipmentToast.isBlank(), "Equipment creation toast is blank");

        driver.get(url("WMS", "/pickup-detail/" + pickupId));
        wmsApiClient.getPickupDetail(pickupId, token);
        preparePickingIfNeeded(wmsApiClient, pickupId, equipmentCode, token);

        pickAndPackOrderPage.receivePackingTrolley(pickupId);
        pickAndPackOrderPage.verifyToastMessageIfPresent("Nhận bảng kê thành công", 5000);

        List<PackingOrder> packingOrders = wmsApiClient.getPickupPackingOrders(pickupId, token);
        Assert.assertFalse(packingOrders.isEmpty(), "No packing orders found for pickup " + pickupId);
        pickAndPackOrderPage.scanTablePacking(ConfigReader.required("DEFAULT_PACKING_TABLE_CODE"));
        pickAndPackOrderPage.scanPickUpOrder(pickupId);
        int processedOrderCount = pickAndPackOrderPage.packBySystemSuggestion(packingOrders, packingMaterialCode);
        Assert.assertTrue(processedOrderCount > 0, "No order was processed for pickup " + pickupId);
    }

    private void clearBrowserStorage() {
        ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
    }

    private void preparePickingIfNeeded(WmsApiClient wmsApiClient, String pickupId, String equipmentCode, String token) {
        runOptionalPrePackingStep("map trolley picking",
                () -> wmsApiClient.mapTrolleyPicking(pickupId, equipmentCode, token));
        runOptionalPrePackingStep("pick all products",
                () -> wmsApiClient.pickAllProductsInPickup(pickupId, token));
        runOptionalPrePackingStep("commit picking",
                () -> wmsApiClient.commitPickingPickup(pickupId, equipmentCode, token));
    }

    private void runOptionalPrePackingStep(String stepName, Runnable step) {
        try {
            step.run();
            System.out.println(stepName + " completed");
        } catch (IllegalStateException error) {
            if (isAlreadyPastPrePackingStep(error)) {
                System.out.println(stepName + " skipped: pickup is already past this step. " + error.getMessage());
                return;
            }
            throw error;
        }
    }

    private boolean isAlreadyPastPrePackingStep(IllegalStateException error) {
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("không") && normalized.contains("phép")
                || normalized.contains("not allowed")
                || normalized.contains("no binset found");
    }
}

package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickOrderBasket;
import com.example.seleniumtestng.models.PickupDetail;
import com.example.seleniumtestng.pages.EquipmentPage;
import com.example.seleniumtestng.pages.PickAndPackOrderPage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        PickupDetail pickupDetail = wmsApiClient.getPickupDetailInfo(pickupId, token);
        int basketCount = basketCountFor(pickupDetail);
        boolean b2bPacking = isB2bPickup(pickupDetail);
        System.out.println("Pickup detail: code=" + pickupDetail.pickupCode()
                + ", internalId=" + pickupDetail.pickupId()
                + ", type=" + pickupDetail.pickupType()
                + ", totalOrder=" + pickupDetail.totalOrder()
                + ", basketCount=" + basketCount
                + ", basketCodes=" + String.join(",", pickupDetail.basketCodes()));

        List<String> basketCodes = new ArrayList<>(pickupDetail.basketCodes());
        List<String> basketCodesToAssign = new ArrayList<>();
        boolean basketAlreadyAssigned = basketCount > 0 && basketCodes.size() >= basketCount;
        String pickingEquipmentCode = null;
        if (basketCount > 0) {
            if (basketAlreadyAssigned) {
                System.out.println("Use existing basket codes from pickup detail: " + String.join(",", basketCodes));
            } else {
                driver.get(url("WMS", "/equipments?page=1&page_size=50"));
                int missingBasketCount = basketCount - basketCodes.size();
                basketCodesToAssign = addEquipments(
                        equipmentPage,
                        missingBasketCount,
                        basketEquipmentGroup(),
                        basketEquipmentType(),
                        basketEquipmentSize());
                basketCodes.addAll(basketCodesToAssign);
            }
            Assert.assertEquals(basketCodes.size(), basketCount,
                    "Basket pickup type requires basket count to match expected pickup orders");
        } else {
            driver.get(url("WMS", "/equipments?page=1&page_size=50"));
            pickingEquipmentCode = addEquipment(equipmentPage, trolleyEquipmentGroup(), trolleyEquipmentType(), null);
        }

        driver.get(url("WMS", "/pickup-detail/" + pickupId));
        if (basketCount > 0) {
            if (basketAlreadyAssigned) {
                System.out.println("Skip assign basket: pickup detail already has basket codes");
            } else {
                String trolleyId = wmsApiClient.getPickingTrolleyId(pickupId, token);
                wmsApiClient.assignBasketsToPickOrder(trolleyId, basketCodesToAssign, token);
                System.out.println("Assigned basket codes to pickup " + pickupId
                        + ", trolleyId=" + trolleyId
                        + ": " + String.join(",", basketCodesToAssign));
            }
            prepareBasketPickingIfNeeded(wmsApiClient, pickupId, token);
        } else {
            preparePickingIfNeeded(wmsApiClient, pickupId, pickingEquipmentCode, token);
        }

        String packingTableCode = ConfigReader.required("DEFAULT_PACKING_TABLE_CODE");
        boolean packingTableScanned = false;
        if (basketCount == 0) {
            pickAndPackOrderPage.receivePackingTrolley(pickupId);
            pickAndPackOrderPage.verifyToastMessageIfPresent("Nhận bảng kê thành công", 5000);
            System.out.println("Packing page: " + (b2bPacking ? "/packing-b2b" : "/packing"));
            scanPackingTable(pickAndPackOrderPage, packingTableCode, b2bPacking);
            packingTableScanned = true;
        } else {
            System.out.println("Skip receive packing trolley for basket pickup type: " + pickupDetail.pickupType());
        }

        List<PackingOrder> packingOrders = wmsApiClient.getPickupPackingOrders(pickupId, token);
        Assert.assertFalse(packingOrders.isEmpty(), "No packing orders found for pickup " + pickupId);
        int processedOrderCount;
        if (isMsoPickup(pickupDetail)) {
            List<PickOrderBasket> pickOrderBaskets = wmsApiClient.getPickOrderBaskets(pickupId, token);
            List<PickOrderBasket> readyBaskets = readyPackingBaskets(
                    pickOrderBaskets,
                    basketCodes);
            if (readyBaskets.isEmpty()) {
                Assert.assertTrue(allExpectedBasketsAreReadyToShip(pickOrderBaskets, basketCodes),
                        "MSO pickup has no basket with status_id=502/503 and not all expected baskets are status_id=504");
                System.out.println("All MSO baskets are already packed and ready to ship. Skip packing.");
                return;
            }
            processedOrderCount = packMsoBaskets(
                    pickAndPackOrderPage,
                    readyBaskets,
                    packingOrders,
                    packingMaterialCode,
                    packingTableCode,
                    b2bPacking);
            Assert.assertEquals(processedOrderCount, readyBaskets.size(),
                    "MSO packing should process one order per basket with status_id=502/503");
        } else {
            if (!packingTableScanned) {
                System.out.println("Packing page: " + (b2bPacking ? "/packing-b2b" : "/packing"));
                scanPackingTable(pickAndPackOrderPage, packingTableCode, b2bPacking);
            }
            String packingScanCode = basketCount > 0 ? basketCodes.get(0) : pickupId;
            pickAndPackOrderPage.scanPickUpOrder(packingScanCode);
            processedOrderCount = pickAndPackOrderPage.packBySystemSuggestion(packingOrders, packingMaterialCode);
        }
        Assert.assertTrue(processedOrderCount > 0, "No order was processed for pickup " + pickupId);
    }

    private int packMsoBaskets(
            PickAndPackOrderPage pickAndPackOrderPage,
            List<PickOrderBasket> baskets,
            List<PackingOrder> packingOrders,
            String packingMaterialCode,
            String packingTableCode,
            boolean b2bPacking) {
        int processedOrderCount = 0;
        for (PickOrderBasket basket : baskets) {
            Assert.assertNotNull(basket.trackingCode(),
                    "MSO basket has no mapped tracking/order code: " + basket.code());
            PackingOrder packingOrder = packingOrderByTrackingCode(packingOrders, basket.trackingCode());
            scanPackingTable(pickAndPackOrderPage, packingTableCode, b2bPacking);
            pickAndPackOrderPage.scanPickUpOrder(basket.code());
            String processedTrackingCode = pickAndPackOrderPage.packOneOrderBySystemSuggestion(
                    packingOrder,
                    packingMaterialCode);
            Assert.assertNotNull(processedTrackingCode,
                    "No packing order was processed for basket " + basket.code());
            System.out.println("Packed MSO basket=" + basket.code() + ", tracking=" + processedTrackingCode);
            processedOrderCount++;
        }
        return processedOrderCount;
    }

    private void scanPackingTable(
            PickAndPackOrderPage pickAndPackOrderPage,
            String packingTableCode,
            boolean b2bPacking) {
        if (b2bPacking) {
            pickAndPackOrderPage.scanTablePackingB2b(packingTableCode);
            return;
        }
        pickAndPackOrderPage.scanTablePacking(packingTableCode);
    }

    private List<PickOrderBasket> readyPackingBaskets(List<PickOrderBasket> baskets, List<String> expectedBasketCodes) {
        Set<String> expectedCodes = new HashSet<>(expectedBasketCodes);
        List<PickOrderBasket> readyBaskets = new ArrayList<>();
        for (PickOrderBasket basket : baskets) {
            if (!expectedCodes.isEmpty() && !expectedCodes.contains(basket.code())) {
                System.out.println("Skip basket not attached to pickup detail: " + basket.code()
                        + ", statusId=" + basket.statusId()
                        + ", statusName=" + basket.statusName());
                continue;
            }
            if (isBasketReadyForPacking(basket)) {
                readyBaskets.add(basket);
            } else {
                System.out.println("Skip basket not ready for packing: " + basket.code()
                        + ", statusId=" + basket.statusId()
                        + ", statusName=" + basket.statusName());
            }
        }
        System.out.println("MSO baskets ready for packing status_id=502/503: " + basketSummary(readyBaskets));
        return readyBaskets;
    }

    private boolean isBasketReadyForPacking(PickOrderBasket basket) {
        return basket.statusId() == 502 || basket.statusId() == 503;
    }

    private PackingOrder packingOrderByTrackingCode(List<PackingOrder> packingOrders, String trackingCode) {
        for (PackingOrder order : packingOrders) {
            if (trackingCode.equals(order.trackingCode())) {
                return order;
            }
        }
        throw new IllegalStateException("No packing order found for basket tracking code " + trackingCode);
    }

    private String basketSummary(List<PickOrderBasket> baskets) {
        List<String> parts = new ArrayList<>();
        for (PickOrderBasket basket : baskets) {
            parts.add(basket.code() + "->" + basket.trackingCode());
        }
        return String.join(",", parts);
    }

    private boolean allExpectedBasketsAreReadyToShip(List<PickOrderBasket> baskets, List<String> expectedBasketCodes) {
        Set<String> expectedCodes = new HashSet<>(expectedBasketCodes);
        int matchedBasketCount = 0;
        for (PickOrderBasket basket : baskets) {
            if (!expectedCodes.isEmpty() && !expectedCodes.contains(basket.code())) {
                continue;
            }
            matchedBasketCount++;
            if (basket.statusId() != 504) {
                System.out.println("MSO basket is not ready to ship yet: " + basket.code()
                        + ", statusId=" + basket.statusId()
                        + ", statusName=" + basket.statusName());
                return false;
            }
        }
        return matchedBasketCount > 0;
    }

    private List<String> addEquipments(
            EquipmentPage equipmentPage,
            int count,
            String groupName,
            String typeName,
            String sizeName) {
        List<String> equipmentCodes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            equipmentCodes.add(addEquipment(equipmentPage, groupName, typeName, sizeName));
        }
        return equipmentCodes;
    }

    private String addEquipment(EquipmentPage equipmentPage, String groupName, String typeName, String sizeName) {
        String equipmentCode = equipmentPage.addEquipment(groupName, typeName, sizeName);
        String equipmentToast = equipmentPage.waitForAnyToast();
        Assert.assertFalse(equipmentToast.isBlank(), "Equipment creation toast is blank");
        System.out.println("Created equipment: group=" + groupName
                + ", type=" + typeName
                + ", size=" + (sizeName == null ? "" : sizeName)
                + ", code=" + equipmentCode
                + ", toast=" + equipmentToast);
        return equipmentCode;
    }

    private int basketCountFor(PickupDetail pickupDetail) {
        String pickupType = pickupDetail.pickupType() == null ? "" : pickupDetail.pickupType().trim().toLowerCase();
        if (pickupType.contains("sso")) {
            return 1;
        }
        if (pickupType.contains("mso")) {
            Assert.assertTrue(pickupDetail.totalOrder() > 0,
                    "MSO pickup must have total_order > 0 to assign baskets");
            return pickupDetail.totalOrder();
        }
        return 0;
    }

    private boolean isMsoPickup(PickupDetail pickupDetail) {
        String pickupType = pickupDetail.pickupType() == null ? "" : pickupDetail.pickupType().trim().toLowerCase();
        return pickupType.contains("mso");
    }

    private boolean isB2bPickup(PickupDetail pickupDetail) {
        String pickupType = pickupDetail.pickupType() == null ? "" : pickupDetail.pickupType().trim().toLowerCase();
        return pickupType.contains("b2b");
    }

    private String trolleyEquipmentGroup() {
        return ConfigReader.getOrDefault(
                "DEFAULT_TROLLEY_EQUIPMENT_GROUP",
                ConfigReader.required("DEFAULT_EQUIPMENT_GROUP"));
    }

    private String trolleyEquipmentType() {
        return ConfigReader.getOrDefault(
                "DEFAULT_TROLLEY_EQUIPMENT_TYPE",
                ConfigReader.required("DEFAULT_EQUIPMENT_TYPE"));
    }

    private String basketEquipmentGroup() {
        return ConfigReader.getOrDefault("DEFAULT_BASKET_EQUIPMENT_GROUP", "Rổ");
    }

    private String basketEquipmentType() {
        return ConfigReader.getOrDefault(
                "DEFAULT_BASKET_EQUIPMENT_TYPE",
                ConfigReader.required("DEFAULT_EQUIPMENT_TYPE"));
    }

    private String basketEquipmentSize() {
        return ConfigReader.getOrDefault(
                "DEFAULT_BASKET_SIZE",
                ConfigReader.getOrDefault("CREATE_ORDER_ORDER_SIZE", "Nhỏ"));
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

    private void prepareBasketPickingIfNeeded(WmsApiClient wmsApiClient, String pickupId, String token) {
        runOptionalPrePackingStep("pick all products",
                () -> wmsApiClient.pickAllProductsInPickup(pickupId, token));
        runOptionalPrePackingStep("commit picking",
                () -> wmsApiClient.commitPickingPickup(pickupId, pickupId, token));
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

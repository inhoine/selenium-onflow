package com.example.seleniumtestng.flows;

import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickOrderBasket;
import com.example.seleniumtestng.models.PickupDetail;
import com.example.seleniumtestng.pages.EquipmentPage;
import com.example.seleniumtestng.pages.PickAndPackOrderPage;
import com.example.seleniumtestng.utils.Log;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

/**
 * Orchestrates Picking and Packing Preparation flows in WMS:
 * - Equipment setup (Baskets / Trolleys)
 * - Assigning baskets to pick order
 * - Picking confirmation via API
 * - Packing MSO baskets & single-order suggestions
 * - Handover confirmation
 */
public class PickingPreparationFlow {
    private final WebDriver driver;
    private final WmsApiClient wmsApiClient;
    private final EquipmentPage equipmentPage;

    public PickingPreparationFlow(WebDriver driver) {
        this.driver = driver;
        this.wmsApiClient = new WmsApiClient();
        this.equipmentPage = new EquipmentPage(driver);
    }

    public PickupDetail prepareForPacking(String pickupCode, String token) {
        PickupDetail pickupDetail = wmsApiClient.getPickupDetailInfo(pickupCode, token);
        int basketCount = basketCountFor(pickupDetail);
        Log.info(
                "Prepare picking for pickup: code={}, internalId={}, type={}, totalOrder={}, basketCount={}, basketCodes={}",
                pickupDetail.pickupCode(),
                pickupDetail.pickupId(),
                pickupDetail.pickupType(),
                pickupDetail.totalOrder(),
                basketCount,
                String.join(",", pickupDetail.basketCodes()));

        List<String> basketCodes = new ArrayList<>(pickupDetail.basketCodes());
        if (basketCount > 0) {
            prepareBasketPickup(pickupCode, token, basketCount, basketCodes);
            pickupDetail = wmsApiClient.getPickupDetailInfo(pickupCode, token);
        } else {
            prepareTrolleyPickup(pickupCode, token);
        }
        driver.get(ConfigReader.required("WMS_BASE_URL") + "/pickup-detail/" + pickupCode);
        return pickupDetail;
    }

    public int packMsoBaskets(
            PickAndPackOrderPage pickAndPackOrderPage,
            List<PickOrderBasket> baskets,
            List<PackingOrder> packingOrders,
            String packingMaterialCode,
            String packingTableCode,
            boolean b2bPacking) {
        int processedOrderCount = 0;
        boolean tableScanned = false;
        Log.info("Start packMsoBaskets: total baskets to process = {}", baskets.size());
        for (int i = 0; i < baskets.size(); i++) {
            PickOrderBasket basket = baskets.get(i);
            Log.info("Processing MSO basket [{}/{}]: code={}, tracking={}, statusId={}",
                    (i + 1), baskets.size(), basket.code(), basket.trackingCode(), basket.statusId());
            Assert.assertNotNull(basket.trackingCode(),
                    "MSO basket has no mapped tracking/order code: " + basket.code());
            PackingOrder packingOrder = packingOrderByTrackingCode(packingOrders, basket.trackingCode());
            if (!tableScanned) {
                scanPackingTable(pickAndPackOrderPage, packingTableCode, b2bPacking);
                tableScanned = true;
            }
            pickAndPackOrderPage.scanPickUpOrder(basket.code());
            String processedTrackingCode = pickAndPackOrderPage.packOneOrderBySystemSuggestion(
                    packingOrder,
                    packingMaterialCode);
            Assert.assertNotNull(processedTrackingCode,
                    "No packing order was processed for basket " + basket.code());
            Log.info("Finished packing MSO basket [{}/{}]: basket={}, tracking={}",
                    (i + 1), baskets.size(), basket.code(), processedTrackingCode);
            processedOrderCount++;
        }
        return processedOrderCount;
    }

    public void scanPackingTable(
            PickAndPackOrderPage pickAndPackOrderPage,
            String packingTableCode,
            boolean b2bPacking) {
        if (b2bPacking) {
            pickAndPackOrderPage.scanTablePackingB2b(packingTableCode);
            return;
        }
        pickAndPackOrderPage.scanTablePacking(packingTableCode);
    }

    public List<PickOrderBasket> readyPackingBaskets(List<PickOrderBasket> baskets, List<String> expectedBasketCodes) {
        Set<String> expectedCodes = new HashSet<>(expectedBasketCodes);
        List<PickOrderBasket> readyBaskets = new ArrayList<>();
        Log.info("Evaluating MSO baskets from API: total={}, expectedFilterCount={}",
                baskets.size(), expectedBasketCodes.size());
        for (PickOrderBasket basket : baskets) {
            // Only filter by expectedCodes if expectedCodes actually contains enough baskets for the pickup,
            // otherwise trust the pick-order baskets API.
            if (!expectedCodes.isEmpty() && expectedCodes.size() >= baskets.size() && !expectedCodes.contains(basket.code())) {
                Log.info("Skip basket not attached to pickup detail: {}", basket.code());
                continue;
            }
            if (isBasketReadyForPacking(basket)) {
                readyBaskets.add(basket);
                Log.info("Basket is ready for packing: code={}, tracking={}, statusId={}, statusName={}",
                        basket.code(), basket.trackingCode(), basket.statusId(), basket.statusName());
            } else {
                Log.info("Skip basket not ready for packing: {}, statusId={}, statusName={}",
                        basket.code(), basket.statusId(), basket.statusName());
            }
        }
        Log.info("MSO baskets ready for packing status_id=502/503: count={}/{}", readyBaskets.size(), baskets.size());
        return readyBaskets;
    }

    public boolean isBasketReadyForPacking(PickOrderBasket basket) {
        return basket.statusId() == 502 || basket.statusId() == 503;
    }

    public PackingOrder packingOrderByTrackingCode(List<PackingOrder> packingOrders, String trackingCode) {
        for (PackingOrder order : packingOrders) {
            if (trackingCode.equals(order.trackingCode())) {
                return order;
            }
        }
        throw new IllegalStateException("No packing order found for basket tracking code " + trackingCode);
    }

    public boolean allExpectedBasketsAreReadyToShip(List<PickOrderBasket> baskets, List<String> expectedBasketCodes) {
        Set<String> expectedCodes = new HashSet<>(expectedBasketCodes);
        int matchedBasketCount = 0;
        for (PickOrderBasket basket : baskets) {
            if (!expectedCodes.isEmpty() && expectedCodes.size() >= baskets.size() && !expectedCodes.contains(basket.code())) {
                continue;
            }
            matchedBasketCount++;
            if (basket.statusId() != 504) {
                Log.info("MSO basket is not ready to ship yet: {}, statusId={}, statusName={}",
                        basket.code(), basket.statusId(), basket.statusName());
                return false;
            }
        }
        return matchedBasketCount > 0;
    }

    public void handoverPackedOrdersIfEnabled(
            WmsApiClient wmsApiClient,
            List<PackingOrder> packingOrders,
            String token) {
        boolean handoverEnabled = Boolean.parseBoolean(
                ConfigReader.getOrDefault("DEFAULT_HANDOVER_ENABLED", "true"));
        if (!handoverEnabled) {
            Log.info("Skip handover: DEFAULT_HANDOVER_ENABLED=false");
            return;
        }

        String handoverCode = wmsApiClient.handoverPackedOrders(
                packingOrders,
                ConfigReader.getOrDefault("DEFAULT_HANDOVER_COURIER_CODE", "DFX"),
                ConfigReader.getOrDefault("DEFAULT_HANDOVER_DRIVER_NAME", "ttt"),
                ConfigReader.getOrDefault("DEFAULT_HANDOVER_DRIVER_PHONE", "555"),
                ConfigReader.getOrDefault("DEFAULT_HANDOVER_DRIVER_LICENSE_NUMBER", "ggg"),
                token);
        Assert.assertFalse(handoverCode.isBlank(), "Handover code should not be blank");
        Log.info("Handover completed: code={}", handoverCode);
    }

    public int basketCountFor(PickupDetail pickupDetail) {
        String pickupType = pickupDetail.pickupType() == null ? "" : pickupDetail.pickupType().trim().toLowerCase();
        if (pickupType.contains("sso")) {
            return 1;
        }
        if (pickupType.contains("mso")) {
            if (pickupDetail.totalOrder() <= 0) {
                throw new IllegalStateException("MSO pickup must have total_order > 0 to assign baskets");
            }
            return pickupDetail.totalOrder();
        }
        return 0;
    }

    public boolean isMsoPickup(PickupDetail pickupDetail) {
        String pickupType = pickupDetail.pickupType() == null ? "" : pickupDetail.pickupType().trim().toLowerCase();
        return pickupType.contains("mso");
    }

    public boolean isB2bPickup(PickupDetail pickupDetail) {
        String pickupType = pickupDetail.pickupType() == null ? "" : pickupDetail.pickupType().trim().toLowerCase();
        return pickupType.contains("b2b");
    }

    private void prepareBasketPickup(String pickupCode, String token, int basketCount, List<String> basketCodes) {
        List<String> basketCodesToAssign = new ArrayList<>();
        boolean basketAlreadyAssigned = basketCodes.size() >= basketCount;
        if (basketAlreadyAssigned) {
            Log.info("Use existing basket codes from pickup detail: {}", String.join(",", basketCodes));
        } else {
            driver.get(ConfigReader.required("WMS_BASE_URL") + "/equipments?page=1&page_size=50");
            int missingBasketCount = basketCount - basketCodes.size();
            basketCodesToAssign = addEquipments(
                    missingBasketCount,
                    basketEquipmentGroup(),
                    basketEquipmentType(),
                    basketEquipmentSize());
            basketCodes.addAll(basketCodesToAssign);
        }

        if (basketCodes.size() != basketCount) {
            throw new IllegalStateException("Basket pickup type requires basket count to match expected pickup orders."
                    + " Expected=" + basketCount
                    + ", actual=" + basketCodes.size()
                    + ", pickup=" + pickupCode);
        }

        if (!basketAlreadyAssigned) {
            String trolleyId = wmsApiClient.getPickingTrolleyId(pickupCode, token);
            wmsApiClient.assignBasketsToPickOrder(trolleyId, basketCodesToAssign, token);
            Log.info("Assigned basket codes to pickup {}, trolleyId={}: {}",
                    pickupCode, trolleyId, String.join(",", basketCodesToAssign));
        }

        prepareBasketPickingIfNeeded(pickupCode, token);
    }

    private void prepareTrolleyPickup(String pickupCode, String token) {
        driver.get(ConfigReader.required("WMS_BASE_URL") + "/equipments?page=1&page_size=50");
        String pickingEquipmentCode = addEquipment(trolleyEquipmentGroup(), trolleyEquipmentType(), null);
        preparePickingIfNeeded(pickupCode, pickingEquipmentCode, token);
    }

    private List<String> addEquipments(int count, String groupName, String typeName, String sizeName) {
        List<String> equipmentCodes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            equipmentCodes.add(addEquipment(groupName, typeName, sizeName));
        }
        return equipmentCodes;
    }

    private String addEquipment(String groupName, String typeName, String sizeName) {
        String equipmentCode = equipmentPage.addEquipment(groupName, typeName, sizeName);
        String equipmentToast = equipmentPage.waitForAnyToast();
        if (equipmentToast.isBlank()) {
            throw new IllegalStateException("Equipment creation toast is blank for " + equipmentCode);
        }
        Log.info("Created equipment: group={}, type={}, size={}, code={}, toast={}",
                groupName, typeName, (sizeName == null ? "" : sizeName), equipmentCode, equipmentToast);
        return equipmentCode;
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
        return ConfigReader.getOrDefault("DEFAULT_BASKET_EQUIPMENT_GROUP", "Ro");
    }

    private String basketEquipmentType() {
        return ConfigReader.getOrDefault(
                "DEFAULT_BASKET_EQUIPMENT_TYPE",
                ConfigReader.required("DEFAULT_EQUIPMENT_TYPE"));
    }

    private String basketEquipmentSize() {
        return ConfigReader.getOrDefault(
                "DEFAULT_BASKET_SIZE",
                ConfigReader.getOrDefault("CREATE_ORDER_ORDER_SIZE", "Nho"));
    }

    private void preparePickingIfNeeded(String pickupCode, String equipmentCode, String token) {
        runOptionalPrePackingStep("map trolley picking",
                () -> wmsApiClient.mapTrolleyPicking(pickupCode, equipmentCode, token));
        runOptionalPrePackingStep("pick all products",
                () -> wmsApiClient.pickAllProductsInPickup(pickupCode, token));
        runOptionalPrePackingStep("commit picking",
                () -> wmsApiClient.commitPickingPickup(pickupCode, equipmentCode, token));
    }

    private void prepareBasketPickingIfNeeded(String pickupCode, String token) {
        runOptionalPrePackingStep("pick all products",
                () -> wmsApiClient.pickAllProductsInPickup(pickupCode, token));
        runOptionalPrePackingStep("commit picking",
                () -> wmsApiClient.commitPickingPickup(pickupCode, pickupCode, token));
    }

    private void runOptionalPrePackingStep(String stepName, Runnable step) {
        try {
            step.run();
            Log.info("{} completed", stepName);
        } catch (IllegalStateException error) {
            if (isAlreadyPastPrePackingStep(error)) {
                Log.info("{} skipped: pickup is already past this step. {}", stepName, error.getMessage());
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
        String normalized = normalizeForMatch(message);
        return normalized.contains("khong") && normalized.contains("phep")
                || normalized.contains("not allowed")
                || normalized.contains("no binset found")
                || normalized.contains("trang thai khong duoc phep");
    }

    private String normalizeForMatch(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT);
    }
}

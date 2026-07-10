package com.example.seleniumtestng.flows;

import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.PickupDetail;
import com.example.seleniumtestng.pages.EquipmentPage;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.openqa.selenium.WebDriver;

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
        System.out.println("Prepare picking for pickup: code=" + pickupDetail.pickupCode()
                + ", internalId=" + pickupDetail.pickupId()
                + ", type=" + pickupDetail.pickupType()
                + ", totalOrder=" + pickupDetail.totalOrder()
                + ", basketCount=" + basketCount
                + ", basketCodes=" + String.join(",", pickupDetail.basketCodes()));

        List<String> basketCodes = new ArrayList<>(pickupDetail.basketCodes());
        if (basketCount > 0) {
            prepareBasketPickup(pickupCode, token, basketCount, basketCodes);
        } else {
            prepareTrolleyPickup(pickupCode, token);
        }
        return pickupDetail;
    }

    private void prepareBasketPickup(String pickupCode, String token, int basketCount, List<String> basketCodes) {
        List<String> basketCodesToAssign = new ArrayList<>();
        boolean basketAlreadyAssigned = basketCodes.size() >= basketCount;
        if (basketAlreadyAssigned) {
            System.out.println("Use existing basket codes from pickup detail: " + String.join(",", basketCodes));
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
            System.out.println("Assigned basket codes to pickup " + pickupCode
                    + ", trolleyId=" + trolleyId
                    + ": " + String.join(",", basketCodesToAssign));
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
            if (pickupDetail.totalOrder() <= 0) {
                throw new IllegalStateException("MSO pickup must have total_order > 0 to assign baskets");
            }
            return pickupDetail.totalOrder();
        }
        return 0;
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

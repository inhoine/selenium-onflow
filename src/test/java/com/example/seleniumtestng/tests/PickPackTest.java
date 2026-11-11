package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.flows.PickingPreparationFlow;
import com.example.seleniumtestng.flows.PickupResolver;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickOrderBasket;
import com.example.seleniumtestng.models.PickupDetail;
import com.example.seleniumtestng.pages.PickAndPackOrderPage;
import com.example.seleniumtestng.utils.Log;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.util.List;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Functional test for WMS Pick & Pack workflows:
 * - Pre-packing preparation (baskets/trolley assignment, picking commit)
 * - Packing execution (MSO baskets packing or single order suggestion)
 * - Handover confirmation
 */
@Epic("WMS - Lấy Hàng & Đóng Gói")
@Feature("Quy Trình Pick & Pack")
public class PickPackTest extends BaseTest {

    @Test(description = "Verify pick and pack workflow for pickup order in WMS")
    @Story("Chuẩn bị rổ/xe hàng, xác nhận lấy hàng và đóng gói theo gợi ý hệ thống")
    @Severity(SeverityLevel.BLOCKER)
    public void pickAndPackPickupOrder() {
        driver.manage().deleteAllCookies();
        driver.get(url("WMS", "/login"));
        clearBrowserStorage();
        String token = AuthHelper.loginWms(driver);

        WmsApiClient wmsApiClient = new WmsApiClient();
        PickingPreparationFlow pickingFlow = new PickingPreparationFlow(driver);
        PickAndPackOrderPage pickAndPackOrderPage = new PickAndPackOrderPage(driver);

        PickupResolver pickupResolver = new PickupResolver(driver, wmsApiClient);
        List<String> pickupIds = pickupResolver.resolvePickupIds(token);
        String packingMaterialCode = ConfigReader.getOrDefault("DEFAULT_PACKING_MATERIAL_CODE", "40x20x20");
        String packingTableCode = ConfigReader.required("DEFAULT_PACKING_TABLE_CODE");
        Log.info("Start PickPack test for {} pickup(s): {}, packingMaterialCode={}, packingTableCode={}",
                pickupIds.size(), pickupIds, packingMaterialCode, packingTableCode);

        for (int i = 0; i < pickupIds.size(); i++) {
            String pickupId = pickupIds.get(i);
            Log.info("====================================================================");
            Log.info(">>> Processing Pickup [{}/{}]: pickupId={}", (i + 1), pickupIds.size(), pickupId);
            Log.info("====================================================================");
            token = AuthHelper.getWmsToken(driver);
            processPickupWorkflow(pickupId, token, wmsApiClient, pickingFlow, pickAndPackOrderPage, packingMaterialCode,
                    packingTableCode);
        }
    }

    private void processPickupWorkflow(
            String pickupId,
            String token,
            WmsApiClient wmsApiClient,
            PickingPreparationFlow pickingFlow,
            PickAndPackOrderPage pickAndPackOrderPage,
            String packingMaterialCode,
            String packingTableCode) {

        // 1. Prepare picking (baskets/trolley)
        PickupDetail pickupDetail = preparePickingStep(pickingFlow, pickupId, token);
        if (ConfigReader.getBoolean("PICKPACK_PREPARE_ONLY", false)) {
            Log.info("PICKPACK_PREPARE_ONLY=true: Đã hoàn tất bước 1 (Chuẩn bị lấy hàng) cho pickup {}", pickupId);
            return;
        }
        int basketCount = pickingFlow.basketCountFor(pickupDetail);
        boolean b2bPacking = pickingFlow.isB2bPickup(pickupDetail);

        // 2. Receive trolley if non-basket pickup
        boolean packingTableScanned = false;
        if (basketCount == 0) {
            receiveTrolleyStep(pickAndPackOrderPage, pickingFlow, pickupId, packingTableCode, b2bPacking);
            packingTableScanned = true;
        }

        // 3. Process packing
        List<PackingOrder> packingOrders = wmsApiClient.getPickupPackingOrders(pickupId, token);
        Assert.assertFalse(packingOrders.isEmpty(), "No packing orders found for pickup " + pickupId);

        int processedOrderCount;
        if (pickingFlow.isMsoPickup(pickupDetail)) {
            processedOrderCount = packMsoOrdersStep(wmsApiClient, pickingFlow, pickAndPackOrderPage,
                    pickupId, pickupDetail, packingOrders, packingMaterialCode, packingTableCode, b2bPacking, token);
            if (processedOrderCount == 0) {
                return;
            }
        } else {
            processedOrderCount = packSingleOrSuggestionOrdersStep(pickingFlow, pickAndPackOrderPage,
                    pickupId, pickupDetail, packingOrders, packingMaterialCode, packingTableCode, b2bPacking,
                    basketCount, packingTableScanned);
        }

        // 4. Handover packed orders if enabled
        handoverStep(pickingFlow, wmsApiClient, packingOrders, token);

        if (pickingFlow.isMsoPickup(pickupDetail)) {
            Assert.assertTrue(processedOrderCount > 0, "No order was processed for pickup " + pickupId);
        }
    }

    @Step("1. Chuẩn bị xe/rổ lấy hàng cho pickup {pickupId}")
    private PickupDetail preparePickingStep(PickingPreparationFlow flow, String pickupId, String token) {
        return flow.prepareForPacking(pickupId, token);
    }

    @Step("2. Nhận bảng kê và quét bàn đóng gói ({packingTableCode})")
    private void receiveTrolleyStep(PickAndPackOrderPage page, PickingPreparationFlow flow, String pickupId,
            String packingTableCode, boolean b2bPacking) {
        page.receivePackingTrolley(pickupId);
        page.verifyToastMessageIfPresent("Nhận bảng kê thành công", 5000);
        flow.scanPackingTable(page, packingTableCode, b2bPacking);
    }

    @Step("3. Đóng gói đơn hàng MSO theo danh sách rổ")
    private int packMsoOrdersStep(WmsApiClient apiClient, PickingPreparationFlow flow, PickAndPackOrderPage page,
            String pickupId, PickupDetail pickupDetail, List<PackingOrder> packingOrders,
            String packingMaterialCode, String packingTableCode, boolean b2bPacking, String token) {
        List<PickOrderBasket> pickOrderBaskets = apiClient.getPickOrderBaskets(pickupId, token);
        List<PickOrderBasket> readyBaskets = flow.readyPackingBaskets(pickOrderBaskets, pickupDetail.basketCodes());
        if (readyBaskets.isEmpty()) {
            Assert.assertTrue(
                    flow.allExpectedBasketsAreReadyToShip(pickOrderBaskets, pickupDetail.basketCodes()),
                    "MSO pickup has no basket with status_id=502/503 and not all expected baskets are status_id=504");
            Log.info("All MSO baskets are already packed and ready to ship. Skip packing.");
            return 0;
        }
        int processedOrderCount = flow.packMsoBaskets(
                page, readyBaskets, packingOrders, packingMaterialCode, packingTableCode, b2bPacking);
        Assert.assertEquals(processedOrderCount, readyBaskets.size(),
                "MSO packing should process one order per basket with status_id=502/503");
        return processedOrderCount;
    }

    @Step("3. Đóng gói đơn hàng theo gợi ý hệ thống (Single / SSO)")
    private int packSingleOrSuggestionOrdersStep(PickingPreparationFlow flow, PickAndPackOrderPage page,
            String pickupId, PickupDetail pickupDetail,
            List<PackingOrder> packingOrders, String packingMaterialCode,
            String packingTableCode, boolean b2bPacking,
            int basketCount, boolean packingTableScanned) {
        if (!packingTableScanned) {
            flow.scanPackingTable(page, packingTableCode, b2bPacking);
        }
        String packingScanCode = basketCount > 0 ? pickupDetail.basketCodes().get(0) : pickupId;
        page.scanPickUpOrder(packingScanCode);
        int processed = page.packBySystemSuggestion(packingOrders, packingMaterialCode);
        page.assertAllOrdersPacked(pickupId);
        return processed;
    }

    @Step("4. Bàn giao đơn vị vận chuyển (Handover)")
    private void handoverStep(PickingPreparationFlow flow, WmsApiClient apiClient, List<PackingOrder> packingOrders,
            String token) {
        flow.handoverPackedOrdersIfEnabled(apiClient, packingOrders, token);
    }

    private void clearBrowserStorage() {
        ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
    }
}

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
import com.example.seleniumtestng.pages.PackingStationB2cPage;
import com.example.seleniumtestng.pages.ReceivePackingTrolleyPage;
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
 * Functional test for WMS Packing V2 (B2C Workstation):
 * URL: /packing/station-b2c
 *
 * Flow:
 * 1. Login WMS
 * 2. Prepare pickup (baskets, picking commit) via API
 * 3. Receive trolley / bảng kê if non-basket pickup
 * 4. Open B2C station page (/packing/station-b2c)
 * 5. Scan mã bàn (table / station code)
 * 6. Scan mã bảng kê / rổ / pickup
 * 7. Scan sản phẩm và scan NVL (gọi API /v1/packing/material/{trackingCode} để
 * lấy mã NVL)
 */
@Epic("WMS - Đóng Gói")
@Feature("Packing V2 - Trạm B2C")
public class PackingV2Test extends BaseTest {

    @Test(description = "Packing V2 B2C station: scan mã bàn → mã bảng kê → sản phẩm → scan NVL từ API")
    @Story("Scan bàn đóng gói, bảng kê/rổ, sản phẩm và nguyên vật liệu đóng gói theo gợi ý API")
    @Severity(SeverityLevel.CRITICAL)
    public void packingStationB2cFlow() {
        // ── 1. Login ──────────────────────────────────────────────────────────
        driver.manage().deleteAllCookies();
        driver.get(url("WMS", "/login"));
        clearBrowserStorage();
        String token = loginStep();

        // ── 2. Prepare pickup ─────────────────────────────────────────────────
        WmsApiClient wmsApiClient = new WmsApiClient();
        PickingPreparationFlow pickingFlow = new PickingPreparationFlow(driver);
        PickupResolver resolver = new PickupResolver(driver, wmsApiClient);

        List<String> pickupIds = resolver.resolvePickupIds(token);
        Assert.assertFalse(pickupIds.isEmpty(), "No pickup IDs resolved from config/resolver");
        String pickupId = pickupIds.get(0);
        Log.info("Processing pickup: {}", pickupId);

        token = AuthHelper.getWmsToken(driver);
        PickupDetail pickupDetail = preparePickupStep(pickingFlow, pickupId, token);
        int basketCount = pickingFlow.basketCountFor(pickupDetail);

        // ── 3. Receive trolley / bảng kê if non-basket pickup ──────────────────
        if (basketCount == 0) {
            receiveTrolleyStep(pickupId);
        }

        // ── 4. Open B2C station ───────────────────────────────────────────────
        PackingStationB2cPage stationPage = new PackingStationB2cPage(driver);
        openStationPageStep(stationPage);

        // ── 5. Scan table / station code ──────────────────────────────────────
        String tableCode = ConfigReader.required("DEFAULT_PACKING_TABLE_CODE");
        scanTableStep(stationPage, tableCode);

        // ── 6 & 7. Scan basket & pack products + packaging materials (NVL) ──
        token = AuthHelper.getWmsToken(driver);
        List<PackingOrder> packingOrders = wmsApiClient.getPickupPackingOrders(pickupId, token);
        Assert.assertFalse(packingOrders.isEmpty(), "No packing orders found for pickup " + pickupId);

        if (basketCount <= 1) {
            String scanCode = (basketCount == 1 && !pickupDetail.basketCodes().isEmpty())
                    ? pickupDetail.basketCodes().get(0)
                    : pickupId;
            scanBasketStep(stationPage, scanCode);
            packOrdersWithMaterialStep(stationPage, packingOrders, wmsApiClient, token);
        } else {
            packMultiBasketPickupStep(stationPage, pickingFlow, wmsApiClient, pickupId, pickupDetail, packingOrders, token);
        }

        // ── 8. Kiểm tra trạng thái đơn hàng sau khi đóng gói ───────────────────
        verifyOrdersPackedStep(wmsApiClient, packingOrders, token);

        Log.info("SUCCESS: Hoàn tất đóng gói và scan NVL cho pickup {}", pickupId);
    }

    // ── Steps ──────────────────────────────────────────────────────────────────

    @Step("1. Đăng nhập WMS")
    private String loginStep() {
        return AuthHelper.loginWms(driver);
    }

    @Step("2. Chuẩn bị pickup {pickupId} (rổ/xe, lấy hàng)")
    private PickupDetail preparePickupStep(PickingPreparationFlow flow, String pickupId, String token) {
        return flow.prepareForPacking(pickupId, token);
    }

    @Step("3. Nhận xe hàng / bảng kê ({pickupId})")
    private void receiveTrolleyStep(String pickupId) {
        ReceivePackingTrolleyPage receivePage = new ReceivePackingTrolleyPage(driver);
        receivePage.receivePackingTrolley(pickupId);
        receivePage.verifyToastMessageIfPresent("Nhận bảng kê thành công", 5000);
    }

    @Step("4. Mở trang Trạm B2C (/packing/station-b2c)")
    private void openStationPageStep(PackingStationB2cPage page) {
        page.open();
    }

    @Step("5. Scan mã bàn đóng gói: {tableCode}")
    private void scanTableStep(PackingStationB2cPage page, String tableCode) {
        page.scanPackingTable(tableCode);
    }

    @Step("6. Scan mã bảng kê / rổ: {scanCode}")
    private void scanBasketStep(PackingStationB2cPage page, String scanCode) {
        page.scanBasketOrPickup(scanCode);
    }

    @Step("7. Scan sản phẩm và scan NVL theo gợi ý API (/v1/packing/material/{tracking_code})")
    private void packOrdersWithMaterialStep(
            PackingStationB2cPage page,
            List<PackingOrder> orders,
            WmsApiClient apiClient,
            String token) {
        page.packOrdersWithMaterial(orders, apiClient, token);
    }

    @Step("6 & 7. Đóng gói cho pickup nhiều rổ ({pickupId})")
    private void packMultiBasketPickupStep(
            PackingStationB2cPage page,
            PickingPreparationFlow pickingFlow,
            WmsApiClient apiClient,
            String pickupId,
            PickupDetail pickupDetail,
            List<PackingOrder> packingOrders,
            String token) {
        List<PickOrderBasket> pickOrderBaskets = apiClient.getPickOrderBaskets(pickupId, token);
        List<PickOrderBasket> readyBaskets = pickingFlow.readyPackingBaskets(pickOrderBaskets, pickupDetail.basketCodes());
        Log.info("Multi-basket packing: found {} ready baskets from API", readyBaskets.size());

        if (readyBaskets.isEmpty()) {
            if (pickingFlow.allExpectedBasketsAreReadyToShip(pickOrderBaskets, pickupDetail.basketCodes())) {
                Log.info("All MSO baskets are already packed and ready to ship. Skip packing.");
                return;
            }
            List<String> basketCodes = pickupDetail.basketCodes();
            Log.warn("No ready baskets filtered by status, falling back to all {} basket codes from pickupDetail", basketCodes.size());
            for (int i = 0; i < basketCodes.size(); i++) {
                String basketCode = basketCodes.get(i);
                Log.info("Processing multi-basket [{}/{}]: basketCode={}", (i + 1), basketCodes.size(), basketCode);
                if (i > 0) {
                    page.resetBasketIfNeeded();
                }
                page.scanBasketOrPickup(basketCode);
                List<PackingOrder> ordersToPack = (i < packingOrders.size())
                        ? List.of(packingOrders.get(i))
                        : packingOrders;
                page.packOrdersWithMaterial(ordersToPack, apiClient, token);
            }
            return;
        }

        for (int i = 0; i < readyBaskets.size(); i++) {
            PickOrderBasket basket = readyBaskets.get(i);
            Log.info("Processing multi-basket [{}/{}]: basketCode={}, trackingCode={}",
                    (i + 1), readyBaskets.size(), basket.code(), basket.trackingCode());
            if (i > 0) {
                page.resetBasketIfNeeded();
            }
            page.scanBasketOrPickup(basket.code());

            PackingOrder targetOrder = null;
            if (basket.trackingCode() != null && !basket.trackingCode().isBlank()) {
                for (PackingOrder order : packingOrders) {
                    if (basket.trackingCode().equals(order.trackingCode())) {
                        targetOrder = order;
                        break;
                    }
                }
            }
            List<PackingOrder> ordersToPack = (targetOrder != null)
                    ? List.of(targetOrder)
                    : (i < packingOrders.size() ? List.of(packingOrders.get(i)) : packingOrders);
            page.packOrdersWithMaterial(ordersToPack, apiClient, token);
        }
    }

    @Step("8. Kiểm tra trạng thái đơn hàng sau khi đóng gói")
    private void verifyOrdersPackedStep(WmsApiClient apiClient, List<PackingOrder> orders, String token) {
        for (PackingOrder order : orders) {
            int statusId = apiClient.getOrderStatusId(order.trackingCode(), token);
            Assert.assertTrue(statusId == 504 || statusId >= 604,
                    "Đơn hàng " + order.trackingCode()
                            + " phải ở trạng thái Đã đóng gói (status 504 hoặc >= 604), hiện tại: "
                            + statusId);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void clearBrowserStorage() {
        ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
    }
}

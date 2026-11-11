package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.flows.InboundCreationFlow;
import com.example.seleniumtestng.flows.InboundInspectionFlow;
import com.example.seleniumtestng.flows.InboundInspectionFlow.InspectionSummary;
import com.example.seleniumtestng.models.InboundPackageData;
import com.example.seleniumtestng.models.InboundProductData;
import com.example.seleniumtestng.utils.Log;
import com.example.seleniumtestng.utils.TestDataReader;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Functional tests for Inbound Product workflows:
 * - Happy path: Full PO creation (OMS) and inspection passes across boxes (WMS)
 * - Negative validations: FIFO dimension/barcode, FEFO expiry dates, Serial
 * missing/duplicates, Batch lot
 */
@Epic("WMS - Quản Lý Nhập Kho")
@Feature("Kiểm Hàng Nhập PO (Inbound Inspection)")
public class InboundProductTest extends BaseTest {

    @Test(description = "Verify end-to-end inbound product creation in OMS and inspection in WMS", groups = { "inbound",
            "smoke", "happy-path" })
    @Story("Tạo PO và kiểm hàng thành công qua các kiện")
    @Severity(SeverityLevel.BLOCKER)
    public void createAndInspectInboundProduct() {
        Log.info("=== Bắt đầu kịch bản: Tạo PO và kiểm hàng End-to-End ===");
        boolean createNewInbound = ConfigReader.getBoolean("INBOUND_CREATE_NEW", true);
        List<InboundPackageData> inboundPackages = createNewInbound
                ? TestDataReader.inboundPackages()
                : Collections.emptyList();
        List<InboundProductData> inboundProducts = createNewInbound
                ? TestDataReader.flattenInboundProducts(inboundPackages)
                : Collections.emptyList();

        // 1. Chuẩn bị hoặc tạo mới PO trên OMS
        String inboundCode = prepareInboundPoStep(createNewInbound, inboundPackages);

        // 2. Đăng nhập WMS
        String token = loginWmsStep();

        // 3. Thực hiện kiểm hàng trên WMS
        InboundInspectionFlow flow = new InboundInspectionFlow(driver);
        InspectionSummary summary = executeInspectionStep(flow, inboundCode, token, inboundPackages, inboundProducts);

        // 4. Xác thực kết quả kiểm hàng
        validateInspectionSummaryStep(summary, inboundCode);

        // 5. Cập nhật vị trí Putaway nếu được cấu hình
        updatePutawayIfEnabledStep(inboundCode, token);
    }

    @Test(description = "Verify WMS inspection rejects invalid FIFO product cases", groups = { "inbound", "regression",
            "negative", "fifo" })
    @Story("Kiểm tra từ chối các trường hợp FIFO không hợp lệ (sai số lượng, barcode, kích thước)")
    @Severity(SeverityLevel.NORMAL)
    public void rejectInvalidFifoInboundInspectionCases() {
        Log.info("=== Bắt đầu kịch bản: Kiểm tra Negative Case cho hàng FIFO ===");
        String inboundCode = createSingleProductInboundStep("BIG_BUBBLE_069921", 1);
        String token = loginWmsStep();

        InboundInspectionFlow flow = new InboundInspectionFlow(driver);
        verifyFifoNegativeCasesStep(flow, inboundCode, token);
    }

    @Test(description = "Verify FEFO inbound happy case then reject invalid expiry/manufacture dates", groups = {
            "inbound", "regression", "fefo" })
    @Story("Kiểm hàng FEFO thành công và từ chối NSX/HSD không hợp lệ")
    @Severity(SeverityLevel.CRITICAL)
    public void inspectFefoInboundHappyThenRejectInvalidDates() {
        Log.info("=== Bắt đầu kịch bản: Kiểm hàng FEFO Happy Case & Negative Dates ===");
        InboundCreationFlow inboundCreationFlow = new InboundCreationFlow(driver);
        String happyInboundCode = createInboundPackagesStep(inboundCreationFlow,
                InboundInspectionFlow.singleProductInboundPackages("FEFO-AT", 1), "FEFO-AT Happy");
        String negativeInboundCode = createInboundPackagesCurrentSessionStep(inboundCreationFlow,
                InboundInspectionFlow.singleProductInboundPackages("FEFO-AT", 1), "FEFO-AT Negative");

        String token = loginWmsStep();
        WmsApiClient wmsApiClient = new WmsApiClient();
        InboundInspectionFlow flow = new InboundInspectionFlow(driver);

        markReceivedStep(flow, wmsApiClient, happyInboundCode, token);
        markReceivedStep(flow, wmsApiClient, negativeInboundCode, token);

        inspectHappyCaseStep(flow, wmsApiClient, happyInboundCode, token, "FEFO");
        verifyFefoNegativeCasesStep(flow, negativeInboundCode, token);
    }

    @Test(description = "Verify WMS inspection rejects invalid serial cases", groups = { "inbound", "regression",
            "negative", "serial" })
    @Story("Kiểm tra từ chối serial trùng lặp hoặc thiếu serial")
    @Severity(SeverityLevel.CRITICAL)
    public void rejectInvalidSerialInboundInspectionCases() {
        Log.info("=== Bắt đầu kịch bản: Kiểm tra Negative Case cho hàng Serial ===");
        String inboundCode = createSingleProductInboundStep("SERIAL-AT", 2);
        String token = loginWmsStep();

        InboundInspectionFlow flow = new InboundInspectionFlow(driver);
        verifySerialNegativeCasesStep(flow, inboundCode, token);
    }

    @Test(description = "Verify WMS inspection rejects batch product without batch code", groups = { "inbound",
            "regression", "negative", "batch" })
    @Story("Kiểm tra từ chối hàng Batch khi thiếu mã Lô")
    @Severity(SeverityLevel.NORMAL)
    public void rejectBatchInboundInspectionWithoutBatchCode() {
        Log.info("=== Bắt đầu kịch bản: Kiểm tra từ chối hàng Batch thiếu mã Lô ===");
        String inboundCode = createSingleProductInboundStep("BATCH-AT", 1);
        String token = loginWmsStep();

        InboundInspectionFlow flow = new InboundInspectionFlow(driver);
        verifyMissingBatchLotRejectedStep(flow, inboundCode, token);
    }

    @Test(description = "Verify WMS inspection happy case for batch product", groups = { "inbound", "regression",
            "happy-path", "batch" })
    @Story("Kiểm hàng thành công cho sản phẩm quản lý theo Batch")
    @Severity(SeverityLevel.CRITICAL)
    public void inspectBatchInboundHappyCase() {
        Log.info("=== Bắt đầu kịch bản: Kiểm hàng thành công cho hàng Batch ===");
        String inboundCode = createSingleProductInboundStep("BATCH-AT", 1);
        String token = loginWmsStep();
        WmsApiClient wmsApiClient = new WmsApiClient();

        InboundInspectionFlow flow = new InboundInspectionFlow(driver);
        markReceivedStep(flow, wmsApiClient, inboundCode, token);
        inspectHappyCaseStep(flow, wmsApiClient, inboundCode, token, "Batch");
    }

    // ==========================================
    // ALLURE STEP DECOMPOSITION HELPERS
    // ==========================================

    @Step("1. Chuẩn bị đơn PO nhập kho (Tạo mới hoặc lấy sẵn)")
    private String prepareInboundPoStep(boolean createNew, List<InboundPackageData> packages) {
        if (createNew) {
            Log.info("Khởi tạo đơn PO mới trên OMS với {} kiện...", packages.size());
            String code = new InboundCreationFlow(driver).createApprovedInboundPackages(packages);
            Log.info("Đã tạo thành công PO mới: {}", code);
            return code;
        }
        String existingCode = ConfigReader.required("INBOUND_PO_CODE");
        Log.info("Sử dụng mã PO sẵn có từ cấu hình: {}", existingCode);
        return existingCode;
    }

    @Step("2. Đăng nhập WMS và lấy token xác thực")
    private String loginWmsStep() {
        driver.get(url("WMS", "/login"));
        String token = AuthHelper.loginWms(driver);
        Assert.assertNotNull(token, "Token đăng nhập WMS không được null");
        Assert.assertFalse(token.isBlank(), "Token đăng nhập WMS không được rỗng");
        return token;
    }

    @Step("3. Thực hiện quy trình kiểm hàng trên WMS cho PO {inboundCode}")
    private InspectionSummary executeInspectionStep(InboundInspectionFlow flow, String inboundCode, String token,
            List<InboundPackageData> packages, List<InboundProductData> products) {
        return flow.executeInspection(inboundCode, token, packages, products);
    }

    @Step("4. Xác thực kết quả kiểm hàng PO {inboundCode}")
    private void validateInspectionSummaryStep(InspectionSummary summary, String inboundCode) {
        if (summary.inspectedProducts() == 0 && summary.remainingBoxes().isEmpty()) {
            Log.info("Không còn kiện nào cần kiểm cho PO {}; quá trình kiểm hàng đã hoàn tất trước đó.", inboundCode);
            return;
        }

        Log.info("Kết quả kiểm hàng: inspected={}, skipped={}", summary.inspectedProducts(), summary.skippedProducts());

        if (summary.inspectionLimit() > 0) {
            Assert.assertTrue(
                    summary.inspectedProducts() >= summary.inspectionLimit(),
                    "Số lượng SKU kiểm hàng dừng lại trước khi đạt hạn mức: inspected="
                            + summary.inspectedProducts() + ", limit=" + summary.inspectionLimit());
        } else {
            Assert.assertTrue(
                    summary.remainingBoxes().isEmpty(),
                    "Vẫn còn kiện chưa được kiểm hàng xong: remaining=" + summary.remainingBoxes());
        }
        Assert.assertTrue(summary.inspectedProducts() > 0, "Không có SKU nào được kiểm hàng cho PO " + inboundCode);
    }

    @Step("5. Cập nhật vị trí lưu kho Putaway cho PO {inboundCode}")
    private void updatePutawayIfEnabledStep(String inboundCode, String token) {
        if (!Boolean.parseBoolean(ConfigReader.getOrDefault("INBOUND_UPDATE_PUTAWAY", "false"))) {
            Log.info("Bỏ qua bước Putaway do INBOUND_UPDATE_PUTAWAY=false");
            return;
        }
        WmsApiClient wmsApiClient = new WmsApiClient();
        int putawayTasksUpdated = wmsApiClient.updatePutaway(inboundCode, token);
        Assert.assertTrue(putawayTasksUpdated > 0, "Không có task Putaway nào được cập nhật cho PO " + inboundCode);
        Log.info("Cập nhật thành công {} task Putaway cho PO {}", putawayTasksUpdated, inboundCode);
    }

    @Step("Tạo đơn PO nhập kho trên OMS cho SKU: {sku} (Số lượng: {quantity})")
    private String createSingleProductInboundStep(String sku, int quantity) {
        InboundCreationFlow inboundCreationFlow = new InboundCreationFlow(driver);
        return inboundCreationFlow.createApprovedInboundPackages(
                InboundInspectionFlow.singleProductInboundPackages(sku, quantity));
    }

    @Step("Tạo đơn PO nhập kho trên OMS: {description}")
    private String createInboundPackagesStep(InboundCreationFlow flow, List<InboundPackageData> packages,
            String description) {
        return flow.createApprovedInboundPackages(packages);
    }

    @Step("Tạo đơn PO nhập kho từ phiên hiện tại: {description}")
    private String createInboundPackagesCurrentSessionStep(InboundCreationFlow flow, List<InboundPackageData> packages,
            String description) {
        return flow.createApprovedInboundPackagesFromCurrentSession(packages);
    }

    @Step("Xác nhận đã nhận hàng vào kho cho PO {inboundCode}")
    private void markReceivedStep(InboundInspectionFlow flow, WmsApiClient apiClient, String inboundCode,
            String token) {
        flow.markReceivedIfAllowed(apiClient, inboundCode, token);
    }

    @Step("Kiểm hàng thành công cho đơn {type} PO: {inboundCode}")
    private void inspectHappyCaseStep(InboundInspectionFlow flow, WmsApiClient apiClient, String inboundCode,
            String token, String type) {
        flow.inspectSingleProductHappyCase(apiClient, inboundCode, token);
    }

    @Step("Kiểm tra từ chối các trường hợp sai lệch FIFO cho PO {inboundCode}")
    private void verifyFifoNegativeCasesStep(InboundInspectionFlow flow, String inboundCode, String token) {
        flow.verifyFifoNegativeCases(inboundCode, token);
    }

    @Step("Kiểm tra từ chối các trường hợp NSX/HSD không hợp lệ (FEFO) cho PO {inboundCode}")
    private void verifyFefoNegativeCasesStep(InboundInspectionFlow flow, String inboundCode, String token) {
        flow.verifyFefoNegativeCases(inboundCode, token);
    }

    @Step("Kiểm tra từ chối các trường hợp Serial không hợp lệ cho PO {inboundCode}")
    private void verifySerialNegativeCasesStep(InboundInspectionFlow flow, String inboundCode, String token) {
        flow.verifySerialNegativeCases(inboundCode, token);
    }

    @Step("Kiểm tra từ chối hàng Batch khi thiếu mã Lô cho PO {inboundCode}")
    private void verifyMissingBatchLotRejectedStep(InboundInspectionFlow flow, String inboundCode, String token) {
        flow.verifyMissingBatchLotRejected(inboundCode, token);
    }
}

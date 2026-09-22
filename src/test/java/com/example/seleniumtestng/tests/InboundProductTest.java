package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.flows.InboundCreationFlow;
import com.example.seleniumtestng.models.InboundPackageData;
import com.example.seleniumtestng.models.InboundProductData;
import com.example.seleniumtestng.models.POSku;
import com.example.seleniumtestng.pages.InboundProductWmsPage;
import com.example.seleniumtestng.pages.InboundProductWmsPage.ScannedInboundProduct;
import com.example.seleniumtestng.utils.ScanTable;
import com.example.seleniumtestng.utils.TestDataReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class InboundProductTest extends BaseTest {
    @Test
    public void createAndInspectInboundProduct() {
        boolean createNewInbound = ConfigReader.getBoolean("INBOUND_CREATE_NEW", true);
        List<InboundPackageData> inboundPackages = createNewInbound
                ? TestDataReader.inboundPackages()
                : Collections.emptyList();
        List<InboundProductData> inboundProducts = createNewInbound
                ? TestDataReader.flattenInboundProducts(inboundPackages)
                : Collections.emptyList();
        String inboundCode = createNewInbound
                ? new InboundCreationFlow(driver).createApprovedInboundPackages(inboundPackages)
                : ConfigReader.required("INBOUND_PO_CODE");

        driver.get(url("WMS", "/login"));
        String token = AuthHelper.loginWms(driver);

        WmsApiClient wmsApiClient = new WmsApiClient();
        markReceivedIfAllowed(wmsApiClient, inboundCode, token);

        int inspectionLimit = Integer.parseInt(ConfigReader.getOrDefault("INBOUND_INSPECTION_LIMIT", "1"));
        List<String> boxCodes = boxCodesToInspect(wmsApiClient, inboundCode, token, inspectionLimit);
        if (boxCodes.isEmpty()) {
            System.out.println("No pending PO boxes to inspect for " + inboundCode + "; inbound inspection is already complete");
            return;
        }
        System.out.println("Read PO boxes from API: " + boxCodes.size());

        int inspectedProducts = 0;
        int skippedProducts = 0;
        InboundProductWmsPage inboundWms = null;
        int fullInspectionPass = 1;
        int maxFullInspectionPasses = Integer.parseInt(ConfigReader.getOrDefault("INBOUND_FULL_MAX_PASSES", "5"));

        while (!boxCodes.isEmpty()) {
            int inspectedBeforePass = inspectedProducts;
            int skippedBeforePass = skippedProducts;
            System.out.println("Inbound inspection pass: pass="
                    + fullInspectionPass
                    + ", boxes="
                    + summarizeBoxCodes(boxCodes));

            inboundWms = openInspectionAndScanPo(inboundCode);

            for (String boxCode : boxCodes) {
                Map<String, POSku> boxProductMetadata = productMetadataForBox(
                        wmsApiClient,
                        inboundCode,
                        boxCode,
                        token);

                int inspectedBeforeBox = inspectedProducts;
                int skippedBeforeBox = skippedProducts;
                while (inspectionLimit <= 0 || inspectedProducts < inspectionLimit) {
                    BoxInspectionResult result = scanBoxAndInspectNextProduct(
                            inboundWms,
                            boxCode,
                            boxProductMetadata,
                            inboundPackages,
                            inboundProducts);
                    inspectedProducts += result.inspectedProducts;
                    skippedProducts += result.skippedProducts;
                    if (!result.shouldContinueBox) {
                        break;
                    }
                    if (inspectionLimit > 0 && inspectedProducts >= inspectionLimit) {
                        break;
                    }
                }
                System.out.println("Inbound box summary: box="
                        + boxCode
                        + ", inspected_box=" + (inspectedProducts - inspectedBeforeBox)
                        + ", skipped_box=" + (skippedProducts - skippedBeforeBox)
                        + ", inspected_total=" + inspectedProducts
                        + ", skipped_total=" + skippedProducts);
                if (inspectionLimit > 0 && inspectedProducts >= inspectionLimit) {
                    break;
                }
            }

            if (inspectionLimit > 0) {
                break;
            }
            boxCodes = wmsApiClient.getPendingPoBoxes(inboundCode, token);
            if (boxCodes.isEmpty()) {
                break;
            }
            if (fullInspectionPass >= maxFullInspectionPasses) {
                System.out.println("Stop full inspection because max passes reached: " + maxFullInspectionPasses);
                break;
            }
            if (inspectedProducts == inspectedBeforePass && skippedProducts == skippedBeforePass) {
                System.out.println("Stop full inspection because pending boxes made no progress: " + boxCodes);
                break;
            }
            fullInspectionPass++;
        }
        System.out.println("Inbound inspection summary: inspected=" + inspectedProducts + ", skipped=" + skippedProducts);
        if (inspectionLimit > 0) {
            Assert.assertTrue(
                    inspectedProducts >= inspectionLimit,
                    "Inbound inspection stopped before reaching limit. inspected="
                            + inspectedProducts
                            + ", limit="
                            + inspectionLimit
                            + ", skipped="
                            + skippedProducts);
        } else {
            if (inboundWms != null && inboundWms.finishInspectionSessionIfPresent()) {
                System.out.println("Finished inbound inspection session for " + inboundCode);
            }
            List<String> remainingBoxCodes = waitForRemainingBoxesAfterInspection(wmsApiClient, inboundCode, token);
            Assert.assertTrue(
                    remainingBoxCodes.isEmpty(),
                    "Inbound inspection stopped while PO still has pending boxes. inspected="
                            + inspectedProducts
                            + ", skipped="
                            + skippedProducts
                            + ", remaining_boxes="
                            + remainingBoxCodes);
        }
        Assert.assertTrue(inspectedProducts > 0, "No SKU was inspected for " + inboundCode);

        if (Boolean.parseBoolean(ConfigReader.getOrDefault("INBOUND_UPDATE_PUTAWAY", "false"))) {
            int putawayTasksUpdated = wmsApiClient.updatePutaway(inboundCode, token);
            Assert.assertTrue(putawayTasksUpdated > 0, "No putaway task updated for " + inboundCode);
        }
    }

    @Test
    public void rejectInvalidFifoInboundInspectionCases() {
        InboundCreationFlow inboundCreationFlow = new InboundCreationFlow(driver);
        String inboundCode = inboundCreationFlow.createApprovedInboundPackages(
                singleProductInboundPackages("AT-FAILED", 1));

        driver.get(url("WMS", "/login"));
        String token = AuthHelper.loginWms(driver);
        WmsApiClient wmsApiClient = new WmsApiClient();

        markReceivedIfAllowed(wmsApiClient, inboundCode, token);

        List<String> boxCodes = boxCodesToInspect(wmsApiClient, inboundCode, token, 1);
        Assert.assertFalse(boxCodes.isEmpty(), "No pending PO boxes to inspect for " + inboundCode);
        String boxCode = boxCodes.get(0);

        InboundProductWmsPage inboundWms = openInspectionAndScanPo(inboundCode);
        inboundWms.scanBoxIfNeeded(boxCode);
        ScannedInboundProduct scannedProduct = inboundWms.openFirstInspectableProduct(boxCode);
        Assert.assertNotNull(scannedProduct, "No inspectable FIFO product found in box " + boxCode);

        POSku product = productForInspection(scannedProduct, Collections.emptyMap());
        String validBarcode = inboundWms.readBarcodeFromProductModal();
        for (FifoNegativeCase negativeCase : FifoNegativeCase.values()) {
            inputNegativeInspectionData(inboundWms, product, negativeCase, validBarcode);
            String validationText = inboundWms.submitInspectExpectingValidation();
            assertFifoValidationMessage(negativeCase, validationText);

            System.out.println("FIFO negative inspection blocked as expected: case="
                    + negativeCase
                    + ", po="
                    + inboundCode
                    + ", validation="
                    + validationText);
            Assert.assertTrue(
                    wmsApiClient.getPendingPoBoxes(inboundCode, token).contains(boxCode),
                    "Invalid FIFO inspection should not complete box " + boxCode + " for case " + negativeCase);
        }
    }

    @Test
    public void inspectFefoInboundHappyThenRejectInvalidDates() {
        InboundCreationFlow inboundCreationFlow = new InboundCreationFlow(driver);
        String happyInboundCode = inboundCreationFlow.createApprovedInboundPackages(
                singleProductInboundPackages("FEFO-AT", 1));
        String negativeInboundCode = inboundCreationFlow.createApprovedInboundPackagesFromCurrentSession(
                singleProductInboundPackages("FEFO-AT", 1));

        driver.get(url("WMS", "/login"));
        String token = AuthHelper.loginWms(driver);
        WmsApiClient wmsApiClient = new WmsApiClient();

        markReceivedIfAllowed(wmsApiClient, happyInboundCode, token);
        markReceivedIfAllowed(wmsApiClient, negativeInboundCode, token);

        inspectSingleProductHappyCase(wmsApiClient, happyInboundCode, token);

        OpenedInboundProduct openedProduct = openFirstProductForInspection(wmsApiClient, negativeInboundCode, token);
        String validBarcode = openedProduct.page.readBarcodeFromProductModal();
        for (FefoNegativeCase negativeCase : FefoNegativeCase.values()) {
            inputFefoNegativeInspectionData(openedProduct.page, openedProduct.product, negativeCase, validBarcode);
            String validationText = openedProduct.page.submitInspectExpectingValidation();
            assertFefoValidationMessage(negativeCase, validationText);

            System.out.println("FEFO negative inspection blocked as expected: case="
                    + negativeCase
                    + ", po="
                    + negativeInboundCode
                    + ", validation="
                    + validationText);
            Assert.assertTrue(
                    wmsApiClient.getPendingPoBoxes(negativeInboundCode, token).contains(openedProduct.boxCode),
                    "Invalid FEFO inspection should not complete box "
                            + openedProduct.boxCode
                            + " for case "
                            + negativeCase);
        }
    }

    @Test
    public void rejectInvalidSerialInboundInspectionCases() {
        InboundCreationFlow inboundCreationFlow = new InboundCreationFlow(driver);
        String inboundCode = inboundCreationFlow.createApprovedInboundPackages(
                singleProductInboundPackages("SERIAL-AT", 2));

        driver.get(url("WMS", "/login"));
        String token = AuthHelper.loginWms(driver);
        WmsApiClient wmsApiClient = new WmsApiClient();

        markReceivedIfAllowed(wmsApiClient, inboundCode, token);

        for (SerialNegativeCase negativeCase : SerialNegativeCase.values()) {
            OpenedInboundProduct openedProduct = openFirstProductForInspection(wmsApiClient, inboundCode, token);
            String validBarcode = openedProduct.page.readBarcodeFromProductModal();
            inputSerialNegativeInspectionData(openedProduct.page, openedProduct.product, negativeCase, validBarcode);
            String validationText = submitSerialNegativeInspection(openedProduct.page);
            assertSerialValidationMessage(negativeCase, validationText);

            System.out.println("SERIAL negative inspection blocked as expected: case="
                    + negativeCase
                    + ", po="
                    + inboundCode
                    + ", validation="
                    + validationText);
            Assert.assertTrue(
                    wmsApiClient.getPendingPoBoxes(inboundCode, token).contains(openedProduct.boxCode),
                    "Invalid SERIAL inspection should not complete box "
                            + openedProduct.boxCode
                            + " for case "
                            + negativeCase);
        }
    }

    @Test
    public void rejectBatchInboundInspectionWithoutBatchCode() {
        InboundCreationFlow inboundCreationFlow = new InboundCreationFlow(driver);
        String inboundCode = inboundCreationFlow.createApprovedInboundPackages(
                singleProductInboundPackages("BATCH-AT", 1));

        driver.get(url("WMS", "/login"));
        String token = AuthHelper.loginWms(driver);
        WmsApiClient wmsApiClient = new WmsApiClient();

        markReceivedIfAllowed(wmsApiClient, inboundCode, token);

        OpenedInboundProduct openedProduct = openFirstProductForInspection(wmsApiClient, inboundCode, token);
        String validBarcode = openedProduct.page.readBarcodeFromProductModal();
        inputValidInspectionData(openedProduct.page, openedProduct.product, validBarcode);
        openedProduct.page.clearBatchLotIfPresent();
        String validationText = openedProduct.page.submitInspectExpectingValidation();
        assertValidationContainsAny("BATCH MISSING_BATCH", validationText, "batch");

        System.out.println("BATCH negative inspection blocked as expected: case=MISSING_BATCH, po="
                + inboundCode
                + ", validation="
                + validationText);
        Assert.assertTrue(
                wmsApiClient.getPendingPoBoxes(inboundCode, token).contains(openedProduct.boxCode),
                "Invalid BATCH inspection should not complete box " + openedProduct.boxCode);
    }

    private List<String> waitForRemainingBoxesAfterInspection(
            WmsApiClient wmsApiClient,
            String inboundCode,
            String token) {
        List<String> remainingBoxCodes = new ArrayList<>();
        for (int attempt = 1; attempt <= 5; attempt++) {
            remainingBoxCodes = wmsApiClient.getPendingPoBoxes(inboundCode, token);
            if (remainingBoxCodes.isEmpty()) {
                return remainingBoxCodes;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return remainingBoxCodes;
            }
        }
        return remainingBoxCodes;
    }

    private InboundProductWmsPage openInspectionAndScanPo(String inboundCode) {
        driver.get(url("WMS", "/inspection"));
        new ScanTable(driver).scanIfPresent(ConfigReader.required("INBOUND_PACKING_TABLE_CODE"));
        InboundProductWmsPage inboundWms = new InboundProductWmsPage(driver);
        inboundWms.scanPo(inboundCode);
        return inboundWms;
    }

    private BoxInspectionResult scanBoxAndInspectNextProduct(
            InboundProductWmsPage inboundWms,
            String boxCode,
            Map<String, POSku> productMetadata,
            List<InboundPackageData> inboundPackages,
            List<InboundProductData> inboundProducts) {
        inboundWms.scanBoxIfNeeded(boxCode);
        List<ScannedInboundProduct> visibleProducts = inboundWms.scannedProductsIfPresent(boxCode);
        if (Boolean.parseBoolean(ConfigReader.getOrDefault("INBOUND_ASSERT_TESTDATA", "false"))) {
            assertScannedProductsReadable(visibleProducts, boxCode);
            assertInboundProductsVisible(
                    expectedProductsForBox(inboundPackages, boxCode, inboundProducts),
                    visibleProducts,
                    boxCode);
        }

        ScannedInboundProduct scannedProduct = inboundWms.openFirstInspectableProduct(boxCode);
        if (scannedProduct == null) {
            System.out.println("Box has no actionable product on current screen: box="
                    + boxCode
                    + ", reason=" + inboundWms.lastInspectOpenFailure());
            return BoxInspectionResult.stop(0);
        }

        inspectOpenedProduct(inboundWms, productForInspection(scannedProduct, productMetadata));
        if (productMetadata.size() == 1 || visibleProducts.size() == 1) {
            return BoxInspectionResult.stopAfterInspect(1, 0);
        }
        return BoxInspectionResult.continueBox(1, 0);
    }

    private void markReceivedIfAllowed(WmsApiClient wmsApiClient, String inboundCode, String token) {
        try {
            wmsApiClient.receivedPoAtWarehouse(inboundCode, token);
        } catch (IllegalStateException e) {
            if (!e.getMessage().contains("không được phép nhận vào kho")) {
                throw e;
            }
            System.out.println("Skip receiving PO because current status is not receivable: " + inboundCode);
        }
    }

    private void assertInboundProductsVisible(
            List<InboundProductData> expectedProducts,
            List<ScannedInboundProduct> actualProducts,
            String boxCode) {
        for (InboundProductData expected : expectedProducts) {
            ScannedInboundProduct actual = actualProducts.stream()
                    .filter(product -> normalized(expected.getSku()).equals(normalized(product.sku())))
                    .findFirst()
                    .orElse(null);
            Assert.assertNotNull(actual, "SKU not found after scanning box " + boxCode + ": " + expected.getSku());
            Assert.assertEquals(actual.boxCode(), boxCode, "Unexpected box code for SKU " + expected.getSku());
            Assert.assertEquals(
                    actual.quantityTotalInbound(),
                    expected.getQuantity(),
                    "Unexpected inbound quantity for SKU " + expected.getSku() + " after scanning box " + boxCode);
        }
    }

    private List<InboundProductData> expectedProductsForBox(
            List<InboundPackageData> inboundPackages,
            String boxCode,
            List<InboundProductData> fallbackProducts) {
        int packageIndex = packageIndexFromBoxCode(boxCode);
        if (packageIndex >= 0 && packageIndex < inboundPackages.size()) {
            return inboundPackages.get(packageIndex).getProducts();
        }
        return fallbackProducts;
    }

    private int packageIndexFromBoxCode(String boxCode) {
        if (boxCode == null) {
            return -1;
        }
        String digits = boxCode.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(digits) - 1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void assertScannedProductsReadable(List<ScannedInboundProduct> products, String boxCode) {
        for (ScannedInboundProduct product : products) {
            Assert.assertEquals(product.boxCode(), boxCode, "Unexpected box code for scanned product");
            Assert.assertFalse(product.sku().isBlank(), "Scanned product SKU must not be blank");
            Assert.assertTrue(product.quantityTotalInbound() > 0, "Scanned product quantity must be positive: " + product.sku());
            Assert.assertTrue(product.quantityInbound() >= 0, "Remaining scanned product quantity must not be negative: " + product.sku());
        }
    }

    private Map<String, List<POSku>> skusByBox(List<POSku> shipmentSkus) {
        Map<String, List<POSku>> grouped = new LinkedHashMap<>();
        for (POSku sku : shipmentSkus) {
            if (sku.boxCode() == null || sku.boxCode().isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(sku.boxCode(), ignored -> new ArrayList<>()).add(sku);
        }
        return grouped;
    }

    private List<String> boxCodesToInspect(
            WmsApiClient wmsApiClient,
            String inboundCode,
            String token,
            int inspectionLimit) {
        List<String> boxCodes = inspectionLimit > 0
                ? wmsApiClient.getPendingPoBoxes(inboundCode, token, inspectionLimit)
                : wmsApiClient.getPendingPoBoxes(inboundCode, token);
        if (boxCodes.isEmpty()) {
            return boxCodes;
        }
        String preferredBoxCode = ConfigReader.get("INBOUND_BOX_CODE");
        if (preferredBoxCode == null || preferredBoxCode.isBlank() || !boxCodes.remove(preferredBoxCode)) {
            if (preferredBoxCode != null && !preferredBoxCode.isBlank()) {
                System.out.println("Skip preferred inbound box because it is already received or not found: " + preferredBoxCode);
            }
            return boxCodes;
        }
        boxCodes.add(0, preferredBoxCode);
        return boxCodes;
    }

    private void assertShipmentSkusReadable(List<POSku> products, String boxCode) {
        for (POSku product : products) {
            Assert.assertEquals(product.boxCode(), boxCode, "Unexpected box code from shipment DS SKU");
            Assert.assertNotNull(product.partnerCode(), "Shipment DS SKU must not be null");
            Assert.assertFalse(product.partnerCode().isBlank(), "Shipment DS SKU must not be blank");
            Assert.assertTrue(product.quantityInbound() > 0, "Shipment DS SKU inbound quantity must be positive: " + product.partnerCode());
        }
    }

    private Map<String, POSku> productMetadataBySku(List<POSku> products, String boxCode) {
        Map<String, POSku> metadata = new LinkedHashMap<>();
        if (products.isEmpty()) {
            System.out.println("Inspection API has no product metadata for box " + boxCode + "; using UI quantity and default dimensions");
            return metadata;
        }
        assertShipmentSkusReadable(products, boxCode);
        for (POSku product : products) {
            metadata.put(normalized(product.partnerCode()), product);
        }
        return metadata;
    }

    private Map<String, POSku> productMetadataForBox(
            WmsApiClient wmsApiClient,
            String inboundCode,
            String boxCode,
            String token) {
        if (!Boolean.parseBoolean(ConfigReader.getOrDefault("INBOUND_USE_INSPECTION_API_METADATA", "false"))) {
            return Collections.emptyMap();
        }
        return productMetadataBySku(wmsApiClient.findInspectionProducts(inboundCode, boxCode, token), boxCode);
    }

    private POSku productForInspection(ScannedInboundProduct scannedProduct, Map<String, POSku> productMetadata) {
        POSku metadata = productMetadata.get(normalized(scannedProduct.sku()));
        if (metadata == null) {
            return new POSku(
                    scannedProduct.boxCode(),
                    scannedProduct.quantityInbound(),
                    scannedProduct.sku(),
                    null,
                    null,
                    null,
                    null);
        }
        return new POSku(
                scannedProduct.boxCode(),
                scannedProduct.quantityInbound(),
                scannedProduct.sku(),
                metadata.goodsW(),
                metadata.goodsD(),
                metadata.goodsH(),
                metadata.goodsWeight());
    }

    private void inspectOpenedProduct(InboundProductWmsPage inboundWms, POSku product) {
        inboundWms.inputGoodQuantity(product.quantityInbound());
        inboundWms.inputBarcodeFromProductModal();
        inboundWms.inputBatchLotIfPresent();
        inboundWms.inputSerialsIfPresent(product.quantityInbound(), product.partnerCode());
        inboundWms.inputShelfLifeDatesIfPresent();
        inboundWms.inputProductDimensions(product);
        inboundWms.confirmInspectWithRedlineRetry(product);
    }

    private void inspectSingleProductHappyCase(WmsApiClient wmsApiClient, String inboundCode, String token) {
        OpenedInboundProduct openedProduct = openFirstProductForInspection(wmsApiClient, inboundCode, token);
        inspectOpenedProduct(openedProduct.page, openedProduct.product);
        List<String> remainingBoxes = waitForRemainingBoxesAfterInspection(wmsApiClient, inboundCode, token);
        Assert.assertFalse(
                remainingBoxes.contains(openedProduct.boxCode),
                "Happy FEFO inspection should complete box " + openedProduct.boxCode);
    }

    private OpenedInboundProduct openFirstProductForInspection(
            WmsApiClient wmsApiClient,
            String inboundCode,
            String token) {
        List<String> boxCodes = boxCodesToInspect(wmsApiClient, inboundCode, token, 1);
        Assert.assertFalse(boxCodes.isEmpty(), "No pending PO boxes to inspect for " + inboundCode);
        String boxCode = boxCodes.get(0);

        InboundProductWmsPage inboundWms = openInspectionAndScanPo(inboundCode);
        inboundWms.scanBoxIfNeeded(boxCode);
        ScannedInboundProduct scannedProduct = inboundWms.openFirstInspectableProduct(boxCode);
        Assert.assertNotNull(scannedProduct, "No inspectable product found in box " + boxCode);

        POSku product = productForInspection(scannedProduct, Collections.emptyMap());
        return new OpenedInboundProduct(inboundWms, boxCode, product);
    }

    private void inputNegativeInspectionData(
            InboundProductWmsPage inboundWms,
            POSku product,
            FifoNegativeCase negativeCase,
            String validBarcode) {
        inputValidInspectionData(inboundWms, product, validBarcode);
        if (negativeCase == FifoNegativeCase.QUANTITY_TOO_LARGE) {
            inboundWms.inputGoodQuantity(product.quantityInbound() + 1);
        } else if (negativeCase == FifoNegativeCase.WRONG_BARCODE) {
            inboundWms.inputBarcode("WRONG-BARCODE-" + System.currentTimeMillis());
        } else {
            inboundWms.clearProductDimension(negativeCase.omittedDimensionField());
        }
    }

    private void inputFefoNegativeInspectionData(
            InboundProductWmsPage inboundWms,
            POSku product,
            FefoNegativeCase negativeCase,
            String validBarcode) {
        inputValidInspectionData(inboundWms, product, validBarcode);
        if (negativeCase == FefoNegativeCase.MISSING_SHELF_LIFE_DATES) {
            inboundWms.clearShelfLifeDatesIfPresent();
        } else {
            inboundWms.inputMismatchedShelfLifeDatesIfPresent();
        }
    }

    private void inputSerialNegativeInspectionData(
            InboundProductWmsPage inboundWms,
            POSku product,
            SerialNegativeCase negativeCase,
            String validBarcode) {
        inputValidInspectionData(inboundWms, product, validBarcode);
        if (negativeCase == SerialNegativeCase.MISSING_SERIAL) {
            inboundWms.inputSerialsIfPresent(
                    product.quantityInbound(),
                    product.partnerCode(),
                    Math.max(0, product.quantityInbound() - 1));
        } else if (negativeCase == SerialNegativeCase.DUPLICATE_SERIAL) {
            inboundWms.inputDuplicateSerialsIfPresent(product.partnerCode());
        }
    }

    private String submitSerialNegativeInspection(InboundProductWmsPage inboundWms) {
        if (inboundWms.isSerialModalVisible()) {
            String validationText = inboundWms.visibleValidationText();
            if (!validationText.isBlank()) {
                return validationText;
            }
        }
        if (inboundWms.isConfirmInspectDisabled()) {
            String validationText = inboundWms.visibleValidationText();
            return validationText.isBlank()
                    ? "Inspect submit is disabled because required serials are not complete"
                    : validationText;
        }
        return inboundWms.submitInspectExpectingValidation();
    }

    private void inputValidInspectionData(
            InboundProductWmsPage inboundWms,
            POSku product,
            String validBarcode) {
        inboundWms.inputGoodQuantity(product.quantityInbound());
        inboundWms.inputBarcode(validBarcode);
        inboundWms.inputShelfLifeDatesIfPresent();
        inboundWms.inputProductDimensions(product);
    }

    private List<InboundPackageData> singleProductInboundPackages(String sku, int quantity) {
        InboundProductData product = new InboundProductData();
        product.setSku(sku);
        product.setQuantity(quantity);
        return List.of(new InboundPackageData(List.of(product)));
    }

    private String normalized(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private void assertFifoValidationMessage(FifoNegativeCase negativeCase, String validationText) {
        if (negativeCase == FifoNegativeCase.QUANTITY_TOO_LARGE) {
            assertValidationContainsAny("FIFO " + negativeCase, validationText, "so luong", "quantity");
        } else if (negativeCase == FifoNegativeCase.WRONG_BARCODE) {
            assertValidationContainsAny("FIFO " + negativeCase, validationText, "barcode");
        } else {
            String field = negativeCase.omittedDimensionField();
            if ("weight".equals(field)) {
                assertValidationContainsAny("FIFO " + negativeCase, validationText, "can nang", "khoi luong", "gram");
            } else if ("length".equals(field)) {
                assertValidationContainsAny("FIFO " + negativeCase, validationText, "chieu dai", "the tich");
            } else if ("width".equals(field)) {
                assertValidationContainsAny("FIFO " + negativeCase, validationText, "chieu rong", "the tich");
            } else {
                assertValidationContainsAny("FIFO " + negativeCase, validationText, "chieu cao", "the tich");
            }
        }
    }

    private void assertFefoValidationMessage(FefoNegativeCase negativeCase, String validationText) {
        if (negativeCase == FefoNegativeCase.MISSING_SHELF_LIFE_DATES) {
            assertValidationContainsAny("FEFO " + negativeCase, validationText, "ngay san xuat", "han su dung", "ngay het han");
        } else {
            assertValidationContainsAny("FEFO " + negativeCase, validationText, "shelf life", "khong khop");
        }
    }

    private void assertSerialValidationMessage(SerialNegativeCase negativeCase, String validationText) {
        if (negativeCase == SerialNegativeCase.DUPLICATE_SERIAL) {
            assertValidationContainsAny("SERIAL " + negativeCase, validationText, "serial", "ton tai", "trung");
        } else {
            assertValidationContainsAny("SERIAL " + negativeCase, validationText, "serial", "chua du", "0/2", "1/2");
        }
    }

    private void assertValidationContainsAny(String caseName, String validationText, String... expectedKeywords) {
        String normalizedText = normalizedMessage(validationText);
        for (String keyword : expectedKeywords) {
            if (normalizedText.contains(normalizedMessage(keyword))) {
                return;
            }
        }
        Assert.fail(caseName
                + " validation did not contain expected keywords "
                + List.of(expectedKeywords)
                + ". Actual validation="
                + validationText);
    }

    private String normalizedMessage(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replace('Ä', 'A')
                .replace('�', ' ')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String summarizeBoxCodes(List<String> boxCodes) {
        if (boxCodes.size() <= 10) {
            return boxCodes.toString();
        }
        List<String> firstBoxes = boxCodes.subList(0, 5);
        List<String> lastBoxes = boxCodes.subList(boxCodes.size() - 3, boxCodes.size());
        return firstBoxes + " ... " + lastBoxes + " (total=" + boxCodes.size() + ")";
    }

    private static final class BoxInspectionResult {
        private final boolean shouldContinueBox;
        private final int inspectedProducts;
        private final int skippedProducts;

        private BoxInspectionResult(boolean shouldContinueBox, int inspectedProducts, int skippedProducts) {
            this.shouldContinueBox = shouldContinueBox;
            this.inspectedProducts = inspectedProducts;
            this.skippedProducts = skippedProducts;
        }

        private static BoxInspectionResult continueBox(int inspectedProducts, int skippedProducts) {
            return new BoxInspectionResult(true, inspectedProducts, skippedProducts);
        }

        private static BoxInspectionResult stop(int skippedProducts) {
            return new BoxInspectionResult(false, 0, skippedProducts);
        }

        private static BoxInspectionResult stopAfterInspect(int inspectedProducts, int skippedProducts) {
            return new BoxInspectionResult(false, inspectedProducts, skippedProducts);
        }
    }

    private static final class OpenedInboundProduct {
        private final InboundProductWmsPage page;
        private final String boxCode;
        private final POSku product;

        private OpenedInboundProduct(InboundProductWmsPage page, String boxCode, POSku product) {
            this.page = page;
            this.boxCode = boxCode;
            this.product = product;
        }
    }

    private enum FifoNegativeCase {
        QUANTITY_TOO_LARGE(null),
        WRONG_BARCODE(null),
        MISSING_LENGTH("length"),
        MISSING_WIDTH("width"),
        MISSING_HEIGHT("height"),
        MISSING_WEIGHT("weight");

        private final String omittedDimensionField;

        FifoNegativeCase(String omittedDimensionField) {
            this.omittedDimensionField = omittedDimensionField;
        }

        private String omittedDimensionField() {
            return omittedDimensionField;
        }
    }

    private enum FefoNegativeCase {
        MISSING_SHELF_LIFE_DATES,
        MISMATCHED_SHELF_LIFE_DATES
    }

    private enum SerialNegativeCase {
        NO_SERIAL,
        MISSING_SERIAL,
        DUPLICATE_SERIAL
    }
}

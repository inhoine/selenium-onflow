package com.example.seleniumtestng.flows;

import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.InboundPackageData;
import com.example.seleniumtestng.models.InboundProductData;
import com.example.seleniumtestng.models.POSku;
import com.example.seleniumtestng.pages.inbound.InboundProductWmsPage;
import com.example.seleniumtestng.pages.inbound.InboundProductWmsPage.ScannedInboundProduct;
import com.example.seleniumtestng.utils.Log;
import com.example.seleniumtestng.utils.ScanTable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

/**
 * Orchestrates WMS Inbound Inspection workflows, including:
 * - Full inspection passes across PO boxes
 * - Single product inspections (Happy case)
 * - Negative test data injection and validation assertion for FIFO, FEFO, Serial, and Batch
 */
public class InboundInspectionFlow {
    private final WebDriver driver;

    public InboundInspectionFlow(WebDriver driver) {
        this.driver = driver;
    }

    public InboundProductWmsPage openInspectionAndScanPo(String inboundCode) {
        driver.get(url("WMS", "/inspection"));
        new ScanTable(driver).scanIfPresent(ConfigReader.required("INBOUND_PACKING_TABLE_CODE"));
        InboundProductWmsPage inboundWms = new InboundProductWmsPage(driver);
        inboundWms.scanPo(inboundCode);
        return inboundWms;
    }

    public void markReceivedIfAllowed(WmsApiClient wmsApiClient, String inboundCode, String token) {
        try {
            wmsApiClient.receivedPoAtWarehouse(inboundCode, token);
            Log.info("PO received at warehouse: {}", inboundCode);
        } catch (IllegalStateException e) {
            if (!e.getMessage().contains("không được phép nhận vào kho")) {
                throw e;
            }
            Log.info("Skip receiving PO because current status is not receivable: {}", inboundCode);
        }
    }

    public List<String> boxCodesToInspect(
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
                Log.info("Skip preferred inbound box because it is already received or not found: {}", preferredBoxCode);
            }
            return boxCodes;
        }
        boxCodes.add(0, preferredBoxCode);
        return boxCodes;
    }

    public InspectionSummary executeInspection(
            String inboundCode,
            String token,
            List<InboundPackageData> inboundPackages,
            List<InboundProductData> inboundProducts) {
        WmsApiClient wmsApiClient = new WmsApiClient();
        markReceivedIfAllowed(wmsApiClient, inboundCode, token);

        int inspectionLimit = Integer.parseInt(ConfigReader.getOrDefault("INBOUND_INSPECTION_LIMIT", "1"));
        List<String> boxCodes = boxCodesToInspect(wmsApiClient, inboundCode, token, inspectionLimit);
        if (boxCodes.isEmpty()) {
            Log.info("No pending PO boxes to inspect for {}; inbound inspection is already complete", inboundCode);
            return new InspectionSummary(0, 0, inspectionLimit, Collections.emptyList());
        }

        int inspectedProducts = 0;
        int skippedProducts = 0;
        InboundProductWmsPage inboundWms = null;
        int fullInspectionPass = 1;
        int maxFullInspectionPasses = Integer.parseInt(ConfigReader.getOrDefault("INBOUND_FULL_MAX_PASSES", "5"));

        while (!boxCodes.isEmpty()) {
            int inspectedBeforePass = inspectedProducts;
            int skippedBeforePass = skippedProducts;
            Log.info("Inbound inspection pass: pass={}, boxes={}", fullInspectionPass, summarizeBoxCodes(boxCodes));

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
                    inspectedProducts += result.inspectedProducts();
                    skippedProducts += result.skippedProducts();
                    if (!result.shouldContinueBox()) {
                        break;
                    }
                    if (inspectionLimit > 0 && inspectedProducts >= inspectionLimit) {
                        break;
                    }
                }
                Log.info("Inbound box summary: box={}, inspected_box={}, skipped_box={}, inspected_total={}, skipped_total={}",
                        boxCode,
                        (inspectedProducts - inspectedBeforeBox),
                        (skippedProducts - skippedBeforeBox),
                        inspectedProducts,
                        skippedProducts);

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
                Log.warn("Stop full inspection because max passes reached: {}", maxFullInspectionPasses);
                break;
            }
            if (inspectedProducts == inspectedBeforePass && skippedProducts == skippedBeforePass) {
                Log.warn("Stop full inspection because pending boxes made no progress: {}", boxCodes);
                break;
            }
            fullInspectionPass++;
        }

        List<String> remainingBoxes = Collections.emptyList();
        if (inspectionLimit <= 0) {
            if (inboundWms != null && inboundWms.finishInspectionSessionIfPresent()) {
                Log.info("Finished inbound inspection session for {}", inboundCode);
            }
            remainingBoxes = waitForRemainingBoxesAfterInspection(wmsApiClient, inboundCode, token);
        }

        return new InspectionSummary(inspectedProducts, skippedProducts, inspectionLimit, remainingBoxes);
    }

    public BoxInspectionResult scanBoxAndInspectNextProduct(
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
            Log.warn("Box has no actionable product on current screen: box={}, reason={}",
                    boxCode, inboundWms.lastInspectOpenFailure());
            return BoxInspectionResult.stop(0);
        }

        inspectOpenedProduct(inboundWms, productForInspection(scannedProduct, productMetadata));
        if (productMetadata.size() == 1 || visibleProducts.size() == 1) {
            return BoxInspectionResult.stopAfterInspect(1, 0);
        }
        return BoxInspectionResult.continueBox(1, 0);
    }

    public void inspectOpenedProduct(InboundProductWmsPage inboundWms, POSku product) {
        inboundWms.inputGoodQuantity(product.quantityInbound());
        inboundWms.inputBarcodeFromProductModal();
        inboundWms.inputBatchLotIfPresent();
        inboundWms.inputSerialsIfPresent(product.quantityInbound(), product.partnerCode());
        inboundWms.inputShelfLifeDatesIfPresent();
        inboundWms.inputProductDimensions(product);
        inboundWms.confirmInspectWithRedlineRetry(product);
    }

    public void inspectSingleProductHappyCase(WmsApiClient wmsApiClient, String inboundCode, String token) {
        OpenedInboundProduct openedProduct = openFirstProductForInspection(wmsApiClient, inboundCode, token);
        inspectOpenedProduct(openedProduct.page(), openedProduct.product());
        List<String> remainingBoxes = waitForRemainingBoxesAfterInspection(wmsApiClient, inboundCode, token);
        Assert.assertFalse(
                remainingBoxes.contains(openedProduct.boxCode()),
                "Happy inbound inspection should complete box " + openedProduct.boxCode());
    }

    public OpenedInboundProduct openFirstProductForInspection(
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

    public void verifyFifoNegativeCases(String inboundCode, String token) {
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

            Log.info("FIFO negative inspection blocked as expected: case={}, po={}, validation={}",
                    negativeCase, inboundCode, validationText);
            Assert.assertTrue(
                    wmsApiClient.getPendingPoBoxes(inboundCode, token).contains(boxCode),
                    "Invalid FIFO inspection should not complete box " + boxCode + " for case " + negativeCase);
        }
    }

    public void verifyFefoNegativeCases(String negativeInboundCode, String token) {
        WmsApiClient wmsApiClient = new WmsApiClient();
        markReceivedIfAllowed(wmsApiClient, negativeInboundCode, token);

        OpenedInboundProduct openedProduct = openFirstProductForInspection(wmsApiClient, negativeInboundCode, token);
        String validBarcode = openedProduct.page().readBarcodeFromProductModal();
        for (FefoNegativeCase negativeCase : FefoNegativeCase.values()) {
            inputFefoNegativeInspectionData(openedProduct.page(), openedProduct.product(), negativeCase, validBarcode);
            String validationText = openedProduct.page().submitInspectExpectingValidation();
            assertFefoValidationMessage(negativeCase, validationText);

            Log.info("FEFO negative inspection blocked as expected: case={}, po={}, validation={}",
                    negativeCase, negativeInboundCode, validationText);
            Assert.assertTrue(
                    wmsApiClient.getPendingPoBoxes(negativeInboundCode, token).contains(openedProduct.boxCode()),
                    "Invalid FEFO inspection should not complete box "
                            + openedProduct.boxCode()
                            + " for case "
                            + negativeCase);
        }
    }

    public void verifySerialNegativeCases(String inboundCode, String token) {
        WmsApiClient wmsApiClient = new WmsApiClient();
        markReceivedIfAllowed(wmsApiClient, inboundCode, token);

        for (SerialNegativeCase negativeCase : SerialNegativeCase.values()) {
            OpenedInboundProduct openedProduct = openFirstProductForInspection(wmsApiClient, inboundCode, token);
            String validBarcode = openedProduct.page().readBarcodeFromProductModal();
            inputSerialNegativeInspectionData(openedProduct.page(), openedProduct.product(), negativeCase, validBarcode);
            String validationText = submitSerialNegativeInspection(openedProduct.page());
            assertSerialValidationMessage(negativeCase, validationText);

            Log.info("SERIAL negative inspection blocked as expected: case={}, po={}, validation={}",
                    negativeCase, inboundCode, validationText);
            Assert.assertTrue(
                    wmsApiClient.getPendingPoBoxes(inboundCode, token).contains(openedProduct.boxCode()),
                    "Invalid SERIAL inspection should not complete box "
                            + openedProduct.boxCode()
                            + " for case "
                            + negativeCase);
        }
    }

    public void verifyMissingBatchLotRejected(String inboundCode, String token) {
        WmsApiClient wmsApiClient = new WmsApiClient();
        markReceivedIfAllowed(wmsApiClient, inboundCode, token);

        OpenedInboundProduct openedProduct = openFirstProductForInspection(wmsApiClient, inboundCode, token);
        String validBarcode = openedProduct.page().readBarcodeFromProductModal();
        inputValidInspectionData(openedProduct.page(), openedProduct.product(), validBarcode);
        openedProduct.page().clearBatchLotIfPresent();
        String validationText = openedProduct.page().submitInspectExpectingValidation();
        assertValidationContainsAny("BATCH MISSING_BATCH", validationText, "batch");

        Log.info("BATCH negative inspection blocked as expected: case=MISSING_BATCH, po={}, validation={}",
                inboundCode, validationText);
        Assert.assertTrue(
                wmsApiClient.getPendingPoBoxes(inboundCode, token).contains(openedProduct.boxCode()),
                "Invalid BATCH inspection should not complete box " + openedProduct.boxCode());
    }

    public void inputNegativeInspectionData(
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

    public void inputFefoNegativeInspectionData(
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

    public void inputSerialNegativeInspectionData(
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

    public String submitSerialNegativeInspection(InboundProductWmsPage inboundWms) {
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

    public void inputValidInspectionData(
            InboundProductWmsPage inboundWms,
            POSku product,
            String validBarcode) {
        inboundWms.inputGoodQuantity(product.quantityInbound());
        inboundWms.inputBarcode(validBarcode);
        inboundWms.inputShelfLifeDatesIfPresent();
        inboundWms.inputProductDimensions(product);
    }

    public List<String> waitForRemainingBoxesAfterInspection(
            WmsApiClient wmsApiClient,
            String inboundCode,
            String token) {
        List<String> remainingBoxCodes = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() <= deadline) {
            remainingBoxCodes = wmsApiClient.getPendingPoBoxes(inboundCode, token);
            if (remainingBoxCodes.isEmpty()) {
                return remainingBoxCodes;
            }
            if (!sleepBriefly(250)) {
                return remainingBoxCodes;
            }
        }
        return remainingBoxCodes;
    }

    public static List<InboundPackageData> singleProductInboundPackages(String sku, int quantity) {
        InboundProductData product = new InboundProductData();
        product.setSku(sku);
        product.setQuantity(quantity);
        return List.of(new InboundPackageData(List.of(product)));
    }

    public void assertFifoValidationMessage(FifoNegativeCase negativeCase, String validationText) {
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

    public void assertFefoValidationMessage(FefoNegativeCase negativeCase, String validationText) {
        if (negativeCase == FefoNegativeCase.MISSING_SHELF_LIFE_DATES) {
            assertValidationContainsAny("FEFO " + negativeCase, validationText, "ngay san xuat", "han su dung", "ngay het han");
        } else {
            assertValidationContainsAny("FEFO " + negativeCase, validationText, "shelf life", "khong khop");
        }
    }

    public void assertSerialValidationMessage(SerialNegativeCase negativeCase, String validationText) {
        if (negativeCase == SerialNegativeCase.DUPLICATE_SERIAL) {
            assertValidationContainsAny("SERIAL " + negativeCase, validationText, "serial", "ton tai", "trung");
        } else {
            assertValidationContainsAny("SERIAL " + negativeCase, validationText, "serial", "chua du", "0/2", "1/2");
        }
    }

    public void assertValidationContainsAny(String caseName, String validationText, String... expectedKeywords) {
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

    public String normalizedMessage(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replace('Ä', 'A')
                .replace('\uFFFD', ' ')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
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

    private Map<String, POSku> productMetadataBySku(List<POSku> products, String boxCode) {
        Map<String, POSku> metadata = new LinkedHashMap<>();
        if (products.isEmpty()) {
            Log.info("Inspection API has no product metadata for box {}; using UI quantity and default dimensions", boxCode);
            return metadata;
        }
        assertShipmentSkusReadable(products, boxCode);
        for (POSku product : products) {
            metadata.put(normalized(product.partnerCode()), product);
        }
        return metadata;
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

    private void assertShipmentSkusReadable(List<POSku> products, String boxCode) {
        for (POSku product : products) {
            Assert.assertEquals(product.boxCode(), boxCode, "Unexpected box code from shipment DS SKU");
            Assert.assertNotNull(product.partnerCode(), "Shipment DS SKU must not be null");
            Assert.assertFalse(product.partnerCode().isBlank(), "Shipment DS SKU must not be blank");
            Assert.assertTrue(product.quantityInbound() > 0, "Shipment DS SKU inbound quantity must be positive: " + product.partnerCode());
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

    private String normalized(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String summarizeBoxCodes(List<String> boxCodes) {
        if (boxCodes.size() <= 10) {
            return boxCodes.toString();
        }
        List<String> firstBoxes = boxCodes.subList(0, 5);
        List<String> lastBoxes = boxCodes.subList(boxCodes.size() - 3, boxCodes.size());
        return firstBoxes + " ... " + lastBoxes + " (total=" + boxCodes.size() + ")";
    }

    private boolean sleepBriefly(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String url(String app, String path) {
        return ConfigReader.required(app.toUpperCase() + "_BASE_URL") + path;
    }

    // --- Inner Records / Models for Inbound Flow ---

    public record BoxInspectionResult(boolean shouldContinueBox, int inspectedProducts, int skippedProducts) {
        public static BoxInspectionResult continueBox(int inspected, int skipped) {
            return new BoxInspectionResult(true, inspected, skipped);
        }

        public static BoxInspectionResult stop(int skipped) {
            return new BoxInspectionResult(false, 0, skipped);
        }

        public static BoxInspectionResult stopAfterInspect(int inspected, int skipped) {
            return new BoxInspectionResult(false, inspected, skipped);
        }
    }

    public record OpenedInboundProduct(InboundProductWmsPage page, String boxCode, POSku product) {}

    public record InspectionSummary(
            int inspectedProducts,
            int skippedProducts,
            int inspectionLimit,
            List<String> remainingBoxes) {}

    public enum FifoNegativeCase {
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

        public String omittedDimensionField() {
            return omittedDimensionField;
        }
    }

    public enum FefoNegativeCase {
        MISSING_SHELF_LIFE_DATES,
        MISMATCHED_SHELF_LIFE_DATES
    }

    public enum SerialNegativeCase {
        NO_SERIAL,
        MISSING_SERIAL,
        DUPLICATE_SERIAL
    }
}

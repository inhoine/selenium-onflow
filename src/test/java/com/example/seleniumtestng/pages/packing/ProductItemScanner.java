package com.example.seleniumtestng.pages.packing;

import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickupItem;
import com.example.seleniumtestng.pages.BasePage;
import com.example.seleniumtestng.utils.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ProductItemScanner extends BasePage {
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    private final By scanSkuField = By.xpath("//input[contains(@placeholder,'Sản phẩm')"
            + " or contains(@placeholder,'sản phẩm')"
            + " or contains(@placeholder,'San pham')"
            + " or contains(@placeholder,'san pham')]");
    private final By productRows = By.xpath("//tr[.//div[contains(@id,'barcode_')]]");
    private final Map<String, String> lastScannedSerialByBarcode = new HashMap<>();
    private final Set<String> confirmedScannedSerialCodes = new HashSet<>();

    private final PackagingMaterialHelper materialHelper;
    private final PackingProgressTracker progressTracker;
    private final AttachedDocumentModal attachedDocumentModal;

    public ProductItemScanner(
            WebDriver driver,
            PackagingMaterialHelper materialHelper,
            PackingProgressTracker progressTracker,
            AttachedDocumentModal attachedDocumentModal) {
        super(driver);
        this.materialHelper = materialHelper;
        this.progressTracker = progressTracker;
        this.attachedDocumentModal = attachedDocumentModal;
    }

    public boolean scanProductBarcode(String barcode) {
        WebElement input = findVisibleEnabled(scanSkuField, 1000);
        if (input == null) {
            if (progressTracker.isPackingCompleteNow() || materialHelper.isPackagingMaterialPromptVisibleNow() || !hasPendingUiItemsNow()) {
                Log.info("Product scan is complete; switch to material scan or stop.");
                return false;
            }
            try {
                input = wait.until(driver -> findVisibleEnabledNow(scanSkuField));
            } catch (RuntimeException error) {
                if (progressTracker.isPackingCompleteNow() || materialHelper.isPackagingMaterialPromptVisibleNow() || !hasPendingUiItemsNow()) {
                    Log.info("Product scan is complete; switch to material scan or stop.");
                    return false;
                }
                throw new IllegalStateException("Product scan input is not available. barcode="
                        + barcode
                        + ", screen="
                        + progressTracker.summarizeScreenText(), error);
            }
        }
        try {
            scanIntoProductInput(input, barcode);
            return true;
        } catch (RuntimeException error) {
            if (materialHelper.isPackagingMaterialPromptVisibleNow() || !hasPendingUiItemsNow()) {
                Log.info("Product scan is complete; switch to material scan or stop.");
                return false;
            }
            throw error;
        }
    }

    public void scanIntoProductInput(WebElement input, String barcode) {
        try {
            clearAndEnter(input, barcode);
        } catch (ElementClickInterceptedException e) {
            setInputValue(input, barcode);
            input.sendKeys(Keys.ENTER);
        }
    }

    public boolean scanProductBarcodeForSuggestion(String barcode) {
        attachedDocumentModal.closeBlockingModalIfPresent();
        WebElement input = findVisibleEnabled(scanSkuField, 2500);
        if (input == null) {
            Log.warn("Product scan input is not available for order suggestion: barcode={}", barcode);
            return false;
        }
        try {
            scanIntoProductInput(input, barcode);
            return true;
        } catch (RuntimeException error) {
            Log.warn("Failed to scan barcode for order suggestion: barcode={}, error={}", barcode, error.getMessage());
            return false;
        }
    }

    public void packCurrentSuggestedOrder(
            String materialCode,
            PackingOrder order,
            String initiallyScannedCode) {
        boolean initialScanAccounted = false;
        for (PickupItem item : order.items()) {
            int quantityToScan = getQuantityNeedScan(item);
            if (quantityToScan <= 0 || getFirstItemScanCode(item) == null) {
                continue;
            }
            int startIndex = 0;
            if (!initialScanAccounted && hasScanCode(item, initiallyScannedCode)) {
                startIndex = indexAfterScanCode(item, initiallyScannedCode);
                quantityToScan = Math.max(0, quantityToScan - 1);
                initialScanAccounted = true;
                markSerialScanConfirmedIfNeeded(item, initiallyScannedCode);
            }
            for (int index = startIndex; index < startIndex + quantityToScan; index++) {
                String scanCode = getItemScanCode(item, index);
                if (materialHelper.isPackagingMaterialPromptVisibleNow() && !hasPendingUiItemsNow()) {
                    break;
                }
                int pendingBefore = item.isSerialCode() ? totalPendingUiItemCount() : -1;
                logSerialScanAttemptIfNeeded(item, scanCode);
                if (!scanProductBarcode(scanCode)) {
                    assertNoPendingUiItemsBeforeMaterial(order.trackingCode());
                    break;
                }
                if (item.isSerialCode()) {
                    waitForPendingUiCountToDecrease(pendingBefore, scanCode);
                    markSerialScanConfirmedIfNeeded(item, scanCode);
                }
            }
            if (materialHelper.isPackagingMaterialPromptVisibleNow() && !hasPendingUiItemsNow()) {
                break;
            }
        }
        scanRemainingUiItemsFromScreen(order);
        assertNoPendingUiItemsBeforeMaterial(order.trackingCode());
        PackingProgressTracker.PackingProgress progressBeforeMaterial = progressTracker.readPackingProgressNow();
        materialHelper.scanPackagingMaterial(materialCode);
        progressTracker.waitForPackingProgressToAdvance(order.trackingCode(), progressBeforeMaterial);
    }

    public void scanRemainingUiItemsFromScreen(PackingOrder order) {
        for (int attempt = 1; attempt <= 100; attempt++) {
            PackingUiItem pendingItem = firstPendingUiItem();
            if (pendingItem == null) {
                return;
            }

            PickupItem serialItem = serialItemForBarcode(order, pendingItem.barcode());
            String scanCode = remainingUiScanCode(order, pendingItem, serialItem);
            int pendingBefore = totalPendingUiItemCount();
            Log.info("Scan remaining UI product: tracking={}, barcode={}{}, needScan={}",
                    order.trackingCode(),
                    pendingItem.barcode(),
                    (serialItem == null ? "" : ", serial=" + scanCode),
                    pendingItem.needScan());
            if (!scanProductBarcode(scanCode)) {
                assertNoPendingUiItemsBeforeMaterial(order.trackingCode());
                return;
            }
            waitForPendingUiCountToDecrease(pendingBefore, scanCode);
            if (serialItem != null) {
                lastScannedSerialByBarcode.put(pendingItem.barcode(), scanCode);
                confirmedScannedSerialCodes.add(scanCode);
            }
        }
        throw new IllegalStateException("Too many attempts while scanning remaining UI products. tracking="
                + order.trackingCode()
                + ", pending="
                + pendingUiItemsSummary(pendingUiItemsNow()));
    }

    public boolean hasPendingUiItemsNow() {
        List<WebElement> rows = all(productRows);
        if (rows.isEmpty()) {
            return true;
        }
        for (WebElement row : rows) {
            try {
                Matcher quantityMatcher = QUANTITY_PATTERN.matcher(row.getText());
                if (!quantityMatcher.find()) {
                    return true;
                }
                int packed = Integer.parseInt(quantityMatcher.group(1));
                int total = Integer.parseInt(quantityMatcher.group(2));
                if (total - packed > 0) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                return true;
            }
        }
        return false;
    }

    public PickupItem firstScannableItem(PackingOrder order) {
        for (PickupItem item : order.items()) {
            if (item.quantitySold() > 0 && hasAvailableScanCode(item)) {
                return item;
            }
        }
        return null;
    }

    public String getFirstItemScanCode(PickupItem item) {
        return getItemScanCode(item, 0);
    }

    public String getItemScanCode(PickupItem item, int index) {
        if (item.isSerialCode()) {
            if (item.serialCodes() == null || item.serialCodes().isEmpty()) {
                throw new IllegalStateException("Serial item has no allocated serial code. tracking="
                        + item.trackingCode()
                        + ", partnerCode="
                        + item.partnerCode()
                        + ", goodsCode="
                        + item.goodsCode());
            }
            if (index >= item.serialCodes().size()) {
                throw new IllegalStateException("Not enough allocated serial codes for tracking="
                        + item.trackingCode()
                        + ", partnerCode="
                        + item.partnerCode()
                        + ", requestedIndex="
                        + index
                        + ", available="
                        + item.serialCodes().size());
            }
            return item.serialCodes().get(index);
        }
        return getItemBarcode(item);
    }

    public String getItemBarcode(PickupItem item) {
        if (item.barcodes() != null && !item.barcodes().isEmpty()) {
            return item.barcodes().get(0);
        }
        return item.partnerCode();
    }

    public int getQuantityNeedScan(PickupItem item) {
        return item.quantitySold() - item.quantityPick();
    }

    public boolean hasScanCode(PickupItem item, String scanCode) {
        if (scanCode == null) {
            return false;
        }
        if (item.isSerialCode()) {
            return item.serialCodes() != null && item.serialCodes().contains(scanCode);
        }
        return scanCode.equals(getItemBarcode(item));
    }

    public int indexAfterScanCode(PickupItem item, String scanCode) {
        if (!item.isSerialCode() || item.serialCodes() == null) {
            return 0;
        }
        int index = item.serialCodes().indexOf(scanCode);
        return index < 0 ? 0 : index + 1;
    }

    public boolean isSkuInputVisibleNow() {
        for (WebElement element : driver.findElements(scanSkuField)) {
            try {
                if (element.isDisplayed() && element.isEnabled()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    public boolean isProductRowsVisibleNow() {
        for (WebElement row : driver.findElements(productRows)) {
            try {
                if (row.isDisplayed()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private boolean hasAvailableScanCode(PickupItem item) {
        if (item.isSerialCode()) {
            return item.serialCodes() != null && !item.serialCodes().isEmpty();
        }
        String barcode = getItemBarcode(item);
        return barcode != null && !barcode.isBlank();
    }

    private String remainingUiScanCode(PackingOrder order, PackingUiItem pendingItem, PickupItem serialItem) {
        if (serialItem == null) {
            return pendingItem.barcode();
        }

        for (String serialCode : serialItem.serialCodes()) {
            if (!confirmedScannedSerialCodes.contains(serialCode)) {
                return serialCode;
            }
        }
        throw new IllegalStateException("No remaining allocated serial available for pending serial product. tracking="
                + order.trackingCode()
                + ", barcode="
                + pendingItem.barcode()
                + ", pending="
                + pendingItem.needScan()
                + ", allocatedSerials="
                + serialItem.serialCodes()
                + ", confirmedSerials="
                + confirmedScannedSerialCodes);
    }

    private PickupItem serialItemForBarcode(PackingOrder order, String barcode) {
        for (PickupItem item : order.items()) {
            if (item.isSerialCode() && barcode.equals(getItemBarcode(item))) {
                return item;
            }
        }
        return null;
    }

    private void logSerialScanAttemptIfNeeded(PickupItem item, String serialCode) {
        if (!item.isSerialCode()) {
            return;
        }
        Log.info("Scan serial product: tracking={}, barcode={}, serial={}",
                item.trackingCode(), getItemBarcode(item), serialCode);
    }

    private void markSerialScanConfirmedIfNeeded(PickupItem item, String serialCode) {
        if (!item.isSerialCode()) {
            return;
        }
        String itemBarcode = getItemBarcode(item);
        if (itemBarcode != null && !itemBarcode.isBlank()) {
            lastScannedSerialByBarcode.put(itemBarcode, serialCode);
        }
        confirmedScannedSerialCodes.add(serialCode);
    }

    private void assertNoPendingUiItemsBeforeMaterial(String trackingCode) {
        List<PackingUiItem> pendingItems = pendingUiItemsNow();
        if (!pendingItems.isEmpty()) {
            throw new IllegalStateException("Packing order still has pending products before material scan. tracking="
                    + trackingCode
                    + ", pending="
                    + pendingUiItemsSummary(pendingItems)
                    + ", screen="
                    + progressTracker.summarizeScreenText());
        }
    }

    private List<PackingUiItem> pendingUiItemsNow() {
        List<PackingUiItem> pendingItems = new ArrayList<>();
        for (PackingUiItem item : getCurrentPackingItemsFromUi()) {
            if (item.needScan() > 0) {
                pendingItems.add(item);
            }
        }
        return pendingItems;
    }

    private int totalPendingUiItemCount() {
        int total = 0;
        for (PackingUiItem item : getCurrentPackingItemsFromUi(false)) {
            total += Math.max(0, item.needScan());
        }
        return total;
    }

    private void waitForPendingUiCountToDecrease(int previousPendingCount, String scanCode) {
        try {
            shortWait(3000).until(driver -> totalPendingUiItemCount() < previousPendingCount ? true : null);
        } catch (RuntimeException error) {
            throw new IllegalStateException("Pending UI quantity did not decrease after scanning " + scanCode
                    + ". pendingBefore="
                    + previousPendingCount
                    + ", pendingAfter="
                    + totalPendingUiItemCount()
                    + ", screen="
                    + progressTracker.summarizeScreenText(), error);
        }
    }

    private String pendingUiItemsSummary(List<PackingUiItem> items) {
        List<String> parts = new ArrayList<>();
        for (PackingUiItem item : items) {
            parts.add(item.barcode() + ":needScan=" + item.needScan());
        }
        return parts.toString();
    }

    private PackingUiItem firstPendingUiItem() {
        for (PackingUiItem item : getCurrentPackingItemsFromUi()) {
            if (item.needScan() > 0) {
                return item;
            }
        }
        return null;
    }

    public List<PackingUiItem> getCurrentPackingItemsFromUi() {
        return getCurrentPackingItemsFromUi(true);
    }

    public List<PackingUiItem> getCurrentPackingItemsFromUi(boolean logRows) {
        List<WebElement> rows = wait.until(driver -> {
            List<WebElement> elements = all(productRows);
            return elements.isEmpty() ? null : elements;
        });
        List<PackingUiItem> items = new ArrayList<>();
        for (WebElement row : rows) {
            String rowText = row.getText();
            String barcodeText = row.findElement(By.xpath(".//div[contains(@id,'barcode_')]")).getText();
            String barcode = barcodeText == null ? "" : barcodeText.trim();
            if (barcode.isBlank()) {
                continue;
            }
            Matcher quantityMatcher = QUANTITY_PATTERN.matcher(rowText);
            if (!quantityMatcher.find()) {
                throw new IllegalStateException("Unable to read packing quantity from row: " + rowText);
            }
            String qtyText = quantityMatcher.group(0);
            int packed = Integer.parseInt(quantityMatcher.group(1));
            int total = Integer.parseInt(quantityMatcher.group(2));
            items.add(new PackingUiItem(barcode, total - packed));
            if (logRows) {
                Log.debug("Packing UI row: barcode={}, qty={}, needScan={}{}, row={}",
                        barcode, qtyText, (total - packed), lastScannedSerialLog(barcode), rowText.replaceAll("\\s+", " "));
            }
        }
        return items;
    }

    private String lastScannedSerialLog(String barcode) {
        String serial = lastScannedSerialByBarcode.get(barcode);
        return serial == null || serial.isBlank() ? "" : ", lastScannedSerial=" + serial;
    }

    private WebElement findVisibleEnabled(By locator, long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> findVisibleEnabledNow(locator));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private WebElement findVisibleEnabledNow(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed() && element.isEnabled()) {
                    return element;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    public static final class PackingUiItem {
        private final String barcode;
        private final int needScan;

        public PackingUiItem(String barcode, int needScan) {
            this.barcode = barcode;
            this.needScan = needScan;
        }

        public String barcode() {
            return barcode;
        }

        public int needScan() {
            return needScan;
        }
    }
}


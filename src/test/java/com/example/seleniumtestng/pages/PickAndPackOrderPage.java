package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickupItem;
import com.example.seleniumtestng.pages.packing.AttachedDocumentModal;
import com.example.seleniumtestng.pages.packing.PackagingMaterialHelper;
import com.example.seleniumtestng.pages.packing.PackingProgressTracker;
import com.example.seleniumtestng.pages.packing.ProductItemScanner;
import com.example.seleniumtestng.utils.Log;
import com.example.seleniumtestng.utils.ScanTable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Main Page Object for WMS Packing workflows:
 * Orchestrates receiving trolleys, scanning tables, and packing orders.
 * Delegated to modular sub-components for single responsibility.
 */
public class PickAndPackOrderPage extends BasePage {
    private final By scanPickUpField = By.xpath("//input[contains(@placeholder,'Xe')"
            + " or contains(@placeholder,'Bảng kê')"
            + " or contains(@placeholder,'bảng kê')"
            + " or contains(@placeholder,'Bang ke')"
            + " or contains(@placeholder,'bang ke')"
            + " or contains(@placeholder,'Rổ')"
            + " or contains(@placeholder,'Ro')]");
    private final By confirmScanPickupButton = By
            .xpath("//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");
    private final By packingDetailToggle = By
            .xpath("//*[normalize-space()='(Chi tiết)' or normalize-space()='(Chi tiet)']");

    private final ReceivePackingTrolleyPage receivePackingTrolleyPage;
    private final AttachedDocumentModal attachedDocumentModal;
    private final PackagingMaterialHelper packagingMaterialHelper;
    private final PackingProgressTracker progressTracker;
    private final ProductItemScanner productItemScanner;

    public PickAndPackOrderPage(WebDriver driver) {
        super(driver);
        this.receivePackingTrolleyPage = new ReceivePackingTrolleyPage(driver);
        this.attachedDocumentModal = new AttachedDocumentModal(driver);
        this.packagingMaterialHelper = new PackagingMaterialHelper(driver, attachedDocumentModal);
        this.progressTracker = new PackingProgressTracker(driver, scanPickUpField);
        this.productItemScanner = new ProductItemScanner(driver, packagingMaterialHelper, progressTracker,
                attachedDocumentModal);
    }


    // --- Sub-component getters if caller needs direct access ---
    public ReceivePackingTrolleyPage receiveTrolleyPage() {
        return receivePackingTrolleyPage;
    }

    public AttachedDocumentModal attachedDocumentModal() {
        return attachedDocumentModal;
    }

    public PackagingMaterialHelper materialHelper() {
        return packagingMaterialHelper;
    }

    public PackingProgressTracker progressTracker() {
        return progressTracker;
    }

    public ProductItemScanner productScanner() {
        return productItemScanner;
    }

    // --- Delegated Trolley Reception ---
    public void receivePackingTrolley(String pickupId) {
        receivePackingTrolleyPage.receivePackingTrolley(pickupId);
    }

    public boolean verifyToastMessageIfPresent(String message, long timeoutMillis) {
        return receivePackingTrolleyPage.verifyToastMessageIfPresent(message, timeoutMillis);
    }

    // --- Table Scanning ---
    public void scanTablePacking(String tableCode) {
        scanTablePacking(tableCode, "/packing");
    }

    public void scanTablePackingB2b(String tableCode) {
        scanTablePacking(tableCode, "/packing-b2b");
    }

    private void scanTablePacking(String tableCode, String packingPath) {
        String targetUrl = ConfigReader.required("WMS_BASE_URL") + packingPath;
        if (!driver.getCurrentUrl().contains(packingPath)) {
            driver.get(targetUrl);
        }
        new ScanTable(driver).scanIfPresent(tableCode);
        waitForScanPickupInput();
    }

    // --- Basket / Pickup Scan ---
    public void scanPickUpOrder(String pickupId) {
        attachedDocumentModal.closeBlockingModalIfPresent();
        WebElement input = waitForScanPickupInput();
        try {
            clearAndEnter(input, pickupId);
        } catch (ElementClickInterceptedException error) {
            attachedDocumentModal.closeBlockingModalIfPresent();
            setInputValue(input, pickupId);
            input.sendKeys(Keys.ENTER);
        }
        if (!isPackingContextOpenNow()) {
            clickConfirmScanPickupButtonIfPresent();
        }
        waitForBasketScanToOpenPackingContext(pickupId);
        Log.info("Scanned pickup/basket code: {}", pickupId);
    }

    // --- Packing by System Suggestion (Single / SSO) ---
    public int packBySystemSuggestion(List<PackingOrder> packingOrders, String materialCode) {
        Set<String> visitedTrackingCodes = new HashSet<>();
        int packedOrderCount = 0;
        int maxAttempts = Math.max(15, packingOrders.size() * 3);
        int attempts = 0;

        while (!progressTracker.isPackingCompleteNow() && visitedTrackingCodes.size() < packingOrders.size() && attempts < maxAttempts) {
            attempts++;
            boolean orderPackedInThisIteration = false;

            for (PackingOrder order : packingOrders) {
                if (progressTracker.isPackingCompleteNow()) {
                    break;
                }
                if (visitedTrackingCodes.contains(order.trackingCode())) {
                    continue;
                }

                PickupItem firstScannable = productItemScanner.firstScannableItem(order);
                String firstScanCode = firstScannable != null ? productItemScanner.getFirstItemScanCode(firstScannable) : null;
                String currentTracking = null;
                String initiallyScannedCode = null;

                // 1. Try scanning first product barcode to let system suggest order
                if (firstScanCode != null) {
                    Log.info("Scan first product to let system suggest order: tracking={}, scanCode={}",
                            order.trackingCode(), firstScanCode);
                    if (productItemScanner.scanProductBarcodeForSuggestion(firstScanCode)) {
                        currentTracking = progressTracker.getNewPackingTrackingCodeIfPresent(visitedTrackingCodes, 2500);
                        if (currentTracking != null) {
                            initiallyScannedCode = firstScanCode;
                        }
                    }
                }

                // 2. If system suggestion didn't open a new unvisited order, open directly from details list
                if (currentTracking == null) {
                    Log.info("System suggestion did not open new order; opening from details list: tracking={}",
                            order.trackingCode());
                    currentTracking = openPackingOrderFromDetails(order.trackingCode());
                    initiallyScannedCode = null;
                    if (currentTracking == null && isTrackingAlreadyPackedInDetails(order.trackingCode())) {
                        Log.info("Tracking is already packed according to details: {}", order.trackingCode());
                        visitedTrackingCodes.add(order.trackingCode());
                        attachedDocumentModal.closeBlockingModalIfPresent();
                        continue;
                    }
                    attachedDocumentModal.closeBlockingModalIfPresent();
                }

                if (currentTracking == null) {
                    Log.warn("Unable to open order: tracking={}, will retry", order.trackingCode());
                    continue;
                }

                if (visitedTrackingCodes.contains(currentTracking)) {
                    Log.info("Skip already processed tracking: {}", currentTracking);
                    continue;
                }

                Log.info("Packing suggested tracking: {}", currentTracking);
                PackingOrder suggestedOrder = requirePackingOrderByTrackingCode(packingOrders, currentTracking);
                productItemScanner.packCurrentSuggestedOrder(materialCode, suggestedOrder, initiallyScannedCode);
                visitedTrackingCodes.add(currentTracking);
                packedOrderCount++;
                orderPackedInThisIteration = true;
                break;
            }

            if (!orderPackedInThisIteration && !progressTracker.isPackingCompleteNow()) {
                Log.info("No order was packed in pass {}; checking progress and retrying...", attempts);
                if (progressTracker.isPackingCompleteNow()) {
                    break;
                }
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return packedOrderCount;
    }

    // --- MSO Basket Packing ---
    public String packOneOrderBySystemSuggestion(PackingOrder order, String materialCode) {
        String currentTracking = progressTracker.getCurrentPackingTrackingCodeIfPresent(2000);
        String initiallyScannedCode = null;

        if (currentTracking == null) {
            PickupItem firstScannable = productItemScanner.firstScannableItem(order);
            if (firstScannable == null) {
                Log.warn("Skip {}: no scannable packing item by API data", order.trackingCode());
                return null;
            }
            String firstScanCode = productItemScanner.getFirstItemScanCode(firstScannable);
            Log.info("Order not open yet; scan first product to open: tracking={}, scanCode={}",
                    order.trackingCode(), firstScanCode);
            productItemScanner.scanProductBarcode(firstScanCode);
            currentTracking = openCurrentOrSuggestedOrder(order.trackingCode());
            initiallyScannedCode = firstScanCode;
        }

        if (!order.trackingCode().equalsIgnoreCase(currentTracking)) {
            currentTracking = openPackingOrderFromDetails(order.trackingCode());
        }

        if (!order.trackingCode().equalsIgnoreCase(currentTracking)) {
            throw new IllegalStateException("Basket opened unexpected packing order. Expected tracking="
                    + order.trackingCode()
                    + ", actual tracking="
                    + currentTracking
                    + ", screen="
                    + progressTracker.summarizeScreenText());
        }

        Log.info("Packing mapped basket tracking: {}", currentTracking);
        productItemScanner.packCurrentSuggestedOrder(materialCode, order, initiallyScannedCode);
        return currentTracking;
    }


    public void assertAllOrdersPacked(String pickupId) {
        PackingProgressTracker.PackingProgress progress = progressTracker.getPackingProgress(5000);
        if (progress == null) {
            throw new IllegalStateException("Unable to read packing progress for pickup " + pickupId
                    + ". Screen="
                    + progressTracker.summarizeScreenText());
        }

        Log.info("Packing progress: {}/{}", progress.packed(), progress.total());
        if (progress.packed() != progress.total()) {
            throw new IllegalStateException("Pickup " + pickupId
                    + " is not fully packed. packed="
                    + progress.packed()
                    + "/"
                    + progress.total()
                    + ". Screen="
                    + progressTracker.summarizeScreenText());
        }
    }


    public boolean scanProductBarcodeForSuggestion(String barcode) {
        return productItemScanner.scanProductBarcodeForSuggestion(barcode);
    }

    // --- Private Context & Navigation Helpers ---
    private boolean isPackingContextOpenNow() {
        if (productItemScanner.isSkuInputVisibleNow()) {
            return true;
        }
        if (progressTracker.getCurrentPackingTrackingCodeIfPresent(500) != null) {
            return true;
        }
        return productItemScanner.isProductRowsVisibleNow();
    }

    private void waitForBasketScanToOpenPackingContext(String pickupId) {
        try {
            shortWait(3000).until(driver -> isPackingContextOpenNow() ? true : null);
            return;
        } catch (RuntimeException ignored) {
            Log.info("Packing context was not ready after scanning {}; retry scan once", pickupId);
        }

        WebElement input = findScanPickupInput(2000);
        if (input == null) {
            throw new IllegalStateException("Packing context did not open after scanning " + pickupId
                    + ", and pickup scan input is not available. Screen=" + progressTracker.summarizeScreenText());
        }
        setInputValue(input, pickupId);
        input.sendKeys(Keys.ENTER);
        clickConfirmScanPickupButtonIfPresent();
        try {
            shortWait(5000).until(driver -> isPackingContextOpenNow() ? true : null);
        } catch (RuntimeException error) {
            WebElement confirmButton = findVisibleEnabled(confirmScanPickupButton, 1000);
            if (confirmButton == null) {
                throw new IllegalStateException("Packing context did not open after retrying pickup/basket scan "
                        + pickupId
                        + ". Screen=" + progressTracker.summarizeScreenText(), error);
            }
            jsClick(confirmButton);
            try {
                shortWait(5000).until(driver -> isPackingContextOpenNow() ? true : null);
            } catch (RuntimeException retryError) {
                throw new IllegalStateException("Packing context did not open after confirming pickup/basket scan "
                        + pickupId
                        + ". Screen=" + progressTracker.summarizeScreenText(), retryError);
            }
        }
    }

    private WebElement waitForScanPickupInput() {
        return wait.until(driver -> findScanPickupInputNow());
    }

    private WebElement findScanPickupInput(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> findScanPickupInputNow());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private WebElement findScanPickupInputNow() {
        WebElement matchedInput = findVisibleEnabledNow(scanPickUpField);
        if (matchedInput != null) {
            return matchedInput;
        }

        for (WebElement input : driver.findElements(By.cssSelector("input"))) {
            try {
                if (!input.isDisplayed() || !input.isEnabled()) {
                    continue;
                }
                String placeholder = progressTracker.normalizeScanText(input.getAttribute("placeholder"));
                if (placeholder.contains("bang ke") || placeholder.contains("xe") || placeholder.contains("ro")) {
                    return input;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private void clickConfirmScanPickupButtonIfPresent() {
        WebElement confirmButton = findVisibleEnabled(confirmScanPickupButton, 500);
        if (confirmButton != null) {
            jsClick(confirmButton);
        }
    }

    private String openCurrentOrSuggestedOrder(String expectedTrackingCode) {
        String currentTracking = progressTracker.getCurrentPackingTrackingCodeIfPresent(2500);
        if (currentTracking != null) {
            return currentTracking;
        }

        currentTracking = openPackingOrderFromDetails(expectedTrackingCode);
        if (currentTracking != null) {
            return currentTracking;
        }

        throw new IllegalStateException("Unable to open packing order after scanning product. Expected tracking="
                + expectedTrackingCode
                + ", screen="
                + progressTracker.summarizeScreenText());
    }

    private String openPackingOrderFromDetails(String trackingCode) {
        WebElement packingButton = findPackingButtonForTracking(trackingCode, 1000);
        if (packingButton == null) {
            WebElement detailToggle = findVisible(packingDetailToggle, 1000);
            if (detailToggle != null) {
                jsClick(detailToggle);
            }
            packingButton = findPackingButtonForTracking(trackingCode, 3000);
        }

        if (packingButton == null) {
            return null;
        }

        jsClick(packingButton);
        attachedDocumentModal.closeBlockingModalIfPresent();
        try {
            return shortWait(5000).until(driver -> {
                String current = progressTracker.getCurrentPackingTrackingCodeIfPresent(500);
                return trackingCode.equalsIgnoreCase(current) ? current : null;
            });
        } catch (RuntimeException ignored) {
            return progressTracker.getCurrentPackingTrackingCodeIfPresent(2000);
        }
    }

    private boolean isTrackingAlreadyPackedInDetails(String trackingCode) {
        for (WebElement row : driver.findElements(By.xpath("//tr[contains(.,'" + trackingCode + "')]"))) {
            try {
                String text = progressTracker.normalizeScanText(row.getText());
                if (text.contains("da dong goi") || text.contains("hoan thanh")) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private WebElement findPackingButtonForTracking(String trackingCode, long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement row : driver.findElements(By.xpath("//tr[contains(.,'" + trackingCode + "')]"))) {
                    for (WebElement button : row.findElements(
                            By.xpath(".//button[normalize-space()='Đóng gói' or normalize-space()='Dong goi']"))) {
                        try {
                            if (button.isDisplayed()
                                    && button.isEnabled()
                                    && !button.getAttribute("class").contains("disabled")) {
                                return button;
                            }
                        } catch (RuntimeException ignored) {
                        }
                    }
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private PackingOrder requirePackingOrderByTrackingCode(List<PackingOrder> packingOrders, String trackingCode) {
        for (PackingOrder order : packingOrders) {
            if (trackingCode.equals(order.trackingCode())) {
                return order;
            }
        }
        throw new IllegalStateException("Suggested packing order is not present in pickup API response. tracking="
                + trackingCode
                + ", screen="
                + progressTracker.summarizeScreenText());
    }

    private WebElement findVisible(By locator, long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(locator)) {
                    try {
                        if (element.isDisplayed()) {
                            return element;
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
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
}

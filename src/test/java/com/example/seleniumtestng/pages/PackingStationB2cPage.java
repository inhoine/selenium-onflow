package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickupItem;
import com.example.seleniumtestng.utils.Log;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Page Object for WMS Packing V2 (B2C Workstation):
 * URL: /packing/station-b2c
 * Supports:
 * - Scanning station / table code
 * - Scanning basket / trolley / pickup order code
 * - Scanning product items / serial codes up to the NVL (Packaging Material)
 * step
 */
public class PackingStationB2cPage extends BasePage {
    public static final String PATH = "/packing/station-b2c";

    // --- Table / Station Locators ---
    private final By stationCodeInput = By.id("packing-table-code");
    private final By connectStationButton = By.xpath(
            "//button[contains(normalize-space(),'Kết nối trạm đóng hàng') "
                    + "or contains(normalize-space(),'Ket noi tram dong hang') "
                    + "or (ancestor::div[contains(@class,'modal')] and contains(normalize-space(),'Kết nối'))]");
    private final By stationHeaderButton = By.cssSelector(".packing-workstation__table-button");

    // --- Basket / Trolley / Pickup Locators ---
    private final By basketInput = By.id("b2c-workstation-basket");
    private final By basketConfirmButton = By.xpath(
            "//div[input[@id='b2c-workstation-basket']]//button[normalize-space()='Nhập']");
    private final By resetBasketButton = By.xpath(
            "//button[contains(@class,'packing-workstation__reset-basket') "
                    + "or contains(normalize-space(),'Quét rổ mới') "
                    + "or contains(normalize-space(),'Scan new trolley') "
                    + "or contains(normalize-space(),'Quet ro moi')]");

    // --- Product / SKU / Serial Locators ---
    private final By productScanInput = By.id("b2c-workstation-scan");
    private final By productSubmitButton = By.cssSelector(".packing-workstation__scan-action button[type='submit']");

    // --- Additional Controls & Modals ---
    private final By printerModalCloseButton = By.xpath(
            "//div[@aria-labelledby='qz-printer-error-title' or contains(.,'Kết nối máy in') or contains(.,'Chưa thể kết nối')]//button[normalize-space()='Đóng' or contains(@class,'btn-close') or @aria-label='Close']");

    // --- NVL (Packaging Material) Locators ---
    private final By stationMaterialScanInput = By.cssSelector(
            "#station-b2c-material-scan, input[name='station-b2c-material-scan'], .packing-workstation__material-scan input");
    private final By materialPromptModal = By.cssSelector(
            ".packing-workstation__material-scan, #station-b2c-main-materials-heading, .packing-workstation__material-scan-input");
    private final By activeOrderTracking = By.cssSelector(
            ".packing-workstation__order-code strong, .packing-workstation__material-scan strong.link-secondary, .packing-workstation__order-progress strong");
    private final By attachedDocumentsConfirmButton = By.xpath(
            "//button[@id='confirm-had-print-all' or contains(normalize-space(),'Xác nhận đã in hết') or contains(normalize-space(),'xac nhan da in het')]");

    public PackingStationB2cPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Navigates to /packing/station-b2c and waits for page load.
     */
    public void open() {
        String targetUrl = ConfigReader.required("WMS_BASE_URL") + PATH;
        if (!driver.getCurrentUrl().contains(PATH)) {
            driver.get(targetUrl);
        }
        waitForPageLoaded();
    }

    public void waitForPageLoaded() {
        wait.withTimeout(Duration.ofSeconds(20))
                .until(ExpectedConditions.urlContains(PATH));
        // Allow React SPA components to mount and trigger station checks
        pause(1000);
        dismissPrinterModalIfPresent();
    }

    /**
     * Step 1: Scan Station / Table Code.
     * After clicking "Kết nối", QZ Tray may fire a printer error modal.
     * We dismiss that, then close the still-open station modal via its X button.
     */
    public void scanPackingTable(String tableCode) {
        dismissPrinterModalIfPresent();
        openStationModalIfNeeded();

        WebElement input = shortWait(10000).until(driver -> findVisibleInput(stationCodeInput));
        if (input == null) {
            throw new org.openqa.selenium.TimeoutException(
                    "Station modal input (id=packing-table-code) not visible after openStationModalIfNeeded()");
        }
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(tableCode);
        } catch (ElementClickInterceptedException e) {
            dismissPrinterModalIfPresent();
            setInputValue(input, tableCode);
        }

        pause(300);
        // Ensure input value is synchronized in React
        if (!tableCode.equals(input.getAttribute("value"))) {
            setInputValue(input, tableCode);
            pause(200);
        }

        // Click connect button or press ENTER
        WebElement connectBtn = findVisibleEnabled(connectStationButton, 1500);
        if (connectBtn != null) {
            try {
                connectBtn.click();
            } catch (ElementClickInterceptedException e) {
                jsClick(connectBtn);
            }
            Log.info("Clicked 'Kết nối trạm đóng hàng' for tableCode={}", tableCode);
        } else {
            input.sendKeys(Keys.ENTER);
            Log.info("Sent ENTER to station input for tableCode={}", tableCode);
        }

        // Wait for station modal to close automatically after connecting
        boolean closed = false;
        try {
            shortWait(3000).until(driver -> !isStationModalVisible());
            closed = true;
            Log.info("Station modal closed automatically after connecting");
        } catch (Exception e) {
            dismissPrinterModalIfPresent();
        }

        // Fallback: If modal still hasn't closed, retry pressing ENTER
        if (!closed && isStationModalVisible()) {
            Log.warn("Station modal still visible after first submit attempt, retrying with ENTER...");
            try {
                input.sendKeys(Keys.ENTER);
                shortWait(3000).until(driver -> !isStationModalVisible());
                closed = true;
            } catch (Exception ignored) {
                dismissPrinterModalIfPresent();
            }
        }

        if (!closed && isStationModalVisible()) {
            dismissPrinterModalIfPresent();
            try {
                shortWait(2000).until(driver -> !isStationModalVisible());
            } catch (Exception ignored) {
            }
        }

        dismissPrinterModalIfPresent();
        Log.info("Completed scanning packing station: {}", tableCode);
    }

    /**
     * Step 2: Scan Basket / Trolley / Pickup Order Code
     */
    public void scanBasketOrPickup(String scanCode) {
        dismissPrinterModalIfPresent();
        resetBasketIfNeeded();
        WebElement input = wait.until(driver -> findVisibleInput(basketInput));
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(scanCode);
        } catch (ElementClickInterceptedException e) {
            dismissPrinterModalIfPresent();
            setInputValue(input, scanCode);
        }

        pause(200);
        if (!scanCode.equals(input.getAttribute("value"))) {
            setInputValue(input, scanCode);
            pause(200);
        }

        WebElement confirmBtn = findVisibleEnabled(basketConfirmButton, 1000);
        if (confirmBtn != null) {
            try {
                confirmBtn.click();
            } catch (ElementClickInterceptedException e) {
                jsClick(confirmBtn);
            }
        } else {
            input.sendKeys(Keys.ENTER);
        }

        dismissPrinterModalIfPresent();
        // Wait dynamically for product scan input (b2c-workstation-scan) to appear
        try {
            shortWait(5000).until(driver -> findVisibleInput(productScanInput) != null);
        } catch (Exception ignored) {
            pause(500);
        }
        dismissPrinterModalIfPresent();
        Log.info("Completed scanning basket/pickup code: {}. Visible inputs after scan: [{}]",
                scanCode, getVisibleInputsSummary());
    }

    public boolean isBasketInputVisible() {
        return findVisibleInput(basketInput) != null;
    }

    public void resetBasketIfNeeded() {
        dismissPrinterModalIfPresent();
        if (isBasketInputVisible()) {
            return;
        }
        try {
            shortWait(3000).until(driver -> isBasketInputVisible() || !driver.findElements(resetBasketButton).isEmpty());
        } catch (Exception ignored) {
        }
        if (isBasketInputVisible()) {
            return;
        }
        for (WebElement btn : driver.findElements(resetBasketButton)) {
            try {
                if (btn.isDisplayed() && btn.isEnabled()) {
                    jsClick(btn);
                    Log.info("Clicked reset basket button (.packing-workstation__reset-basket)");
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        try {
            shortWait(3000).until(driver -> isBasketInputVisible());
        } catch (Exception ignored) {
        }
    }

    /**
     * Step 3: Scan Product Item (Barcode / SKU / Serial).
     * Falls back to JavaScript value-set when direct sendKeys is not interactable.
     */
    public void scanProduct(String scanCode) {
        if (isPackagingMaterialPromptVisible()) {
            Log.info("NVL step already visible when entering scanProduct({}) — aborting product scan", scanCode);
            return;
        }

        WebElement input = visibleWithin(productScanInput, 5000);
        if (input == null) {
            if (isPackagingMaterialPromptVisible()) {
                Log.info("NVL step already visible when trying to scan product {} — stopping scan loop", scanCode);
                return;
            }
            String visibleInputs = getVisibleInputsSummary();
            Log.warn("Product scan input (id=b2c-workstation-scan) not visible after 5s. Visible inputs: [{}]",
                    visibleInputs);
            throw new org.openqa.selenium.TimeoutException(
                    "Product scan input (b2c-workstation-scan) not found. Visible inputs on page: " + visibleInputs);
        }

        boolean typed = false;
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(scanCode, Keys.ENTER);
            typed = true;
        } catch (ElementClickInterceptedException e) {
            dismissPrinterModalIfPresent();
            if (isPackagingMaterialPromptVisible()) {
                Log.info("Packaging material modal intercepted product scan click — stopping product scan for {}", scanCode);
                return;
            }
        } catch (ElementNotInteractableException e) {
            if (isPackagingMaterialPromptVisible()) {
                Log.info("Packaging material modal open (product scan not interactable) — stopping product scan for {}", scanCode);
                return;
            }
            Log.warn("Product scan input not interactable via sendKeys — falling back to JS setInputValue");
        }

        if (!typed) {
            if (isPackagingMaterialPromptVisible()) {
                Log.info("NVL modal visible, aborting JS setInputValue for product {}", scanCode);
                return;
            }
            setInputValue(input, scanCode);
            List<WebElement> submitBtns = driver.findElements(productSubmitButton);
            if (!submitBtns.isEmpty() && submitBtns.get(0).isDisplayed()) {
                jsClick(submitBtns.get(0));
            } else {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].dispatchEvent(new KeyboardEvent('keydown', {key:'Enter',keyCode:13,bubbles:true}));",
                        input);
            }
        }

        // Wait dynamically for scan to complete: input value cleared by React or NVL modal appears
        try {
            shortWait(800).until(driver -> isPackagingMaterialPromptVisible()
                    || (input.getAttribute("value") == null || input.getAttribute("value").isEmpty()));
        } catch (Exception ignored) {
        }

        Log.info("Completed scanning product code: {}", scanCode);
    }

    /**
     * Orchestrates product scanning for orders until the NVL step is reached.
     */
    public void packProductsUntilMaterialStep(List<PackingOrder> packingOrders) {
        for (int i = 0; i < packingOrders.size(); i++) {
            PackingOrder order = packingOrders.get(i);
            Log.info("Packing order [{}/{}]: trackingCode={}", (i + 1), packingOrders.size(), order.trackingCode());

            for (PickupItem item : order.items()) {
                int quantity = item.quantitySold() > 0 ? item.quantitySold() : 1;
                for (int q = 0; q < quantity; q++) {
                    if (isPackagingMaterialPromptVisible()) {
                        Log.info("Packaging material step reached for trackingCode={}! Stopping before scan NVL.",
                                order.trackingCode());
                        return;
                    }

                    String scanCode = getItemScanCode(item, q);
                    Log.info("Scan product item [{}/{}]: code={}", (q + 1), quantity, scanCode);
                    scanProduct(scanCode);
                }
            }

            if (isPackagingMaterialPromptVisible()) {
                Log.info("Packaging material step reached after order items! Stopping before scan NVL.");
                return;
            }
        }
    }

    /**
     * Checks whether the flow has reached the Packaging Material (NVL) step.
     */
    public boolean isPackagingMaterialPromptVisible() {
        dismissPrinterModalIfPresent();
        if (findVisibleInput(stationMaterialScanInput) != null) {
            return true;
        }
        for (WebElement element : driver.findElements(materialPromptModal)) {
            try {
                if (element.isDisplayed()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    public boolean waitForPackagingMaterialPrompt(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> isPackagingMaterialPromptVisible() ? true : null);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Step 4: Scan Packaging Material (NVL).
     * Enters the material code into station-b2c-material-scan and submits with ENTER.
     */
    public void scanPackagingMaterial(String materialCode) {
        scanPackagingMaterial(materialCode, true);
    }

    public void scanPackagingMaterial(String materialCode, boolean isLastMaterial) {
        dismissPrinterModalIfPresent();
        WebElement input = wait.until(driver -> findVisibleInput(stationMaterialScanInput));
        if (input == null) {
            throw new org.openqa.selenium.TimeoutException(
                    "Packaging material scan input (#station-b2c-material-scan) not found");
        }

        boolean typed = false;
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(materialCode);
            typed = true;
        } catch (ElementClickInterceptedException e) {
            dismissPrinterModalIfPresent();
        } catch (ElementNotInteractableException e) {
            Log.warn("Material scan input not interactable via sendKeys — falling back to JS setInputValue");
        }

        if (!typed) {
            setInputValue(input, materialCode);
        }

        pause(200);
        if (!materialCode.equals(input.getAttribute("value"))) {
            setInputValue(input, materialCode);
            pause(200);
        }

        // B2C material scan input submits on ENTER keydown
        input.sendKeys(Keys.ENTER);
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new KeyboardEvent('keydown', {key:'Enter',keyCode:13,which:13,bubbles:true}));",
                    input);
        } catch (Exception ignored) {
        }

        dismissPrinterModalIfPresent();
        if (isLastMaterial) {
            confirmAttachedDocumentsIfPresent();
            dismissPrinterModalIfPresent();
        } else {
            // Wait dynamically for input to be cleared/ready for the next material code
            try {
                shortWait(2000).until(driver -> {
                    WebElement mInput = findVisibleInput(stationMaterialScanInput);
                    return mInput != null && (mInput.getAttribute("value") == null || mInput.getAttribute("value").isEmpty());
                });
            } catch (Exception ignored) {
                pause(250);
            }
        }
        Log.info("Completed scanning packaging material code: {} (isLastMaterial={})", materialCode, isLastMaterial);
    }

    public void confirmAttachedDocumentsIfPresent() {
        try {
            WebElement confirmBtn = shortWait(2000).until(driver -> {
                dismissPrinterModalIfPresent();
                for (WebElement btn : driver.findElements(attachedDocumentsConfirmButton)) {
                    if (btn.isDisplayed() && btn.isEnabled()) {
                        return btn;
                    }
                }
                return null;
            });
            if (confirmBtn != null) {
                jsClick(confirmBtn);
                Log.info("Confirmed attached documents print");
                pause(300);
            }
        } catch (Exception ignored) {
        }
        dismissPrinterModalIfPresent();
    }

    /**
     * Reads the tracking code of the order currently active/displayed on the B2C workstation screen,
     * either from the order card (.packing-workstation__order-code strong) or the NVL modal header.
     */
    public String getCurrentActiveOrderTrackingCode() {
        try {
            for (WebElement el : driver.findElements(activeOrderTracking)) {
                if (el.isDisplayed() && !el.getText().trim().isEmpty()) {
                    String text = el.getText().trim();
                    if (text.length() >= 5) {
                        return text;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Orchestrates complete packing for orders:
     * Actively tracks which order is on screen, scans its products until the NVL step,
     * resolves packaging materials from API, and scans the NVL codes.
     */
    public void packOrdersWithMaterial(
            List<PackingOrder> packingOrders,
            com.example.seleniumtestng.clients.WmsApiClient apiClient,
            String token) {
        java.util.Set<String> completedTrackingCodes = new java.util.HashSet<>();
        int maxRounds = packingOrders.size() * 2;

        for (int round = 0; round < maxRounds && completedTrackingCodes.size() < packingOrders.size(); round++) {
            dismissPrinterModalIfPresent();

            // 1. Check if the workstation has an active order on screen
            String activeTracking = getCurrentActiveOrderTrackingCode();
            PackingOrder order = null;
            if (activeTracking != null && !activeTracking.isBlank()) {
                for (PackingOrder po : packingOrders) {
                    if (activeTracking.equals(po.trackingCode())) {
                        order = po;
                        break;
                    }
                }
            }
            if (order == null) {
                // Fallback: pick the first unpacked order
                for (PackingOrder po : packingOrders) {
                    if (!completedTrackingCodes.contains(po.trackingCode())) {
                        order = po;
                        break;
                    }
                }
            }
            if (order == null) {
                Log.info("All packing orders have been processed");
                break;
            }

            Log.info("Packing order [{}/{}]: trackingCode={} (active on screen={})",
                    (completedTrackingCodes.size() + 1), packingOrders.size(), order.trackingCode(), activeTracking);

            // 2. Scan products only if NVL prompt is not already showing
            if (!isPackagingMaterialPromptVisible()) {
                for (PickupItem item : order.items()) {
                    if (isPackagingMaterialPromptVisible()) {
                        Log.info("Packaging material step already appeared for trackingCode={}", order.trackingCode());
                        break;
                    }
                    int quantity = item.quantitySold() > 0 ? item.quantitySold() : 1;
                    for (int q = 0; q < quantity; q++) {
                        if (isPackagingMaterialPromptVisible()) {
                            Log.info("Packaging material step already appeared for trackingCode={}", order.trackingCode());
                            break;
                        }
                        String scanCode = getItemScanCode(item, q);
                        Log.info("Scan product item [{}/{}]: code={}", (q + 1), quantity, scanCode);
                        scanProduct(scanCode);
                    }
                }
            } else {
                Log.info("NVL modal already open for trackingCode={} — proceeding directly to scan NVL", order.trackingCode());
            }

            // 3. Resolve packaging materials to scan for this order
            List<String> materialCodes = apiClient.resolveMaterialScanCodes(order.trackingCode(), token);
            if (materialCodes.isEmpty()) {
                Log.info("Order {}: Tất cả NVL đều có auto_scan=true (hoặc không có NVL cần scan). Bỏ qua bước scan NVL.",
                        order.trackingCode());
                pause(300);
                dismissPrinterModalIfPresent();
                confirmAttachedDocumentsIfPresent();
                dismissPrinterModalIfPresent();
            } else {
                // Wait for NVL prompt to appear for this order
                if (waitForPackagingMaterialPrompt(10000)) {
                    Log.info("Resolved {} packaging material code(s) to scan for order {}: {}",
                            materialCodes.size(), order.trackingCode(), materialCodes);

                    for (int m = 0; m < materialCodes.size(); m++) {
                        String materialCode = materialCodes.get(m);
                        boolean isLast = (m == materialCodes.size() - 1);
                        Log.info("Scanning packaging material [{}/{}]: code={} for order={}",
                                (m + 1), materialCodes.size(), materialCode, order.trackingCode());
                        scanPackagingMaterial(materialCode, isLast);
                    }
                } else {
                    Log.warn("Packaging material prompt did not appear for order: {}", order.trackingCode());
                }
            }

            completedTrackingCodes.add(order.trackingCode());

            if (completedTrackingCodes.size() < packingOrders.size()) {
                // Wait dynamically for workstation to be ready for the next order or next basket
                try {
                    shortWait(4000).until(driver -> findVisibleInput(productScanInput) != null
                            || findVisibleInput(basketInput) != null);
                } catch (Exception ignored) {
                    pause(500);
                }
            }
        }
    }

    public void dismissPrinterModalIfPresent() {
        try {
            List<WebElement> closeBtns = driver.findElements(printerModalCloseButton);
            for (WebElement btn : closeBtns) {
                if (btn.isDisplayed()) {
                    jsClick(btn);
                    Log.info("Dismissed printer connection modal");
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void openStationModalIfNeeded() {
        dismissPrinterModalIfPresent();
        if (isStationModalVisible()) {
            Log.info("Station modal already visible, skip opening");
            return;
        }

        // Wait briefly for potential auto-open on page load
        try {
            shortWait(2000).until(driver -> isStationModalVisible());
            Log.info("Station modal auto-opened on page load");
            return;
        } catch (Exception ignored) {
        }

        // Click the station header button to open the modal
        List<WebElement> stationBtns = driver.findElements(stationHeaderButton);
        if (!stationBtns.isEmpty() && stationBtns.get(0).isDisplayed()) {
            try {
                stationBtns.get(0).click();
            } catch (Exception e) {
                jsClick(stationBtns.get(0));
            }
            Log.info("Clicked station header button to open station modal");
            pause(400);
        } else {
            Log.warn("Station header button not found — station modal may auto-open on page load");
        }
        // Wait up to 5 seconds for packing-table-code input to appear
        try {
            shortWait(5000).until(driver -> findVisibleInput(stationCodeInput) != null);
            Log.info("Station modal input appeared");
        } catch (RuntimeException e) {
            Log.warn("Station modal input (id=packing-table-code) did not appear within 5s");
        }
    }

    /**
     * Checks if the station code input (packing-table-code) is visible — meaning
     * the station modal is open.
     */
    private boolean isStationModalVisible() {
        return findVisibleInput(stationCodeInput) != null;
    }

    private WebElement findVisibleInput(By locator) {
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

    private WebElement findVisibleEnabled(By locator, long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(locator)) {
                    try {
                        if (element.isDisplayed() && element.isEnabled()) {
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

    private String getItemScanCode(PickupItem item, int index) {
        if (item.isSerialCode() && item.serialCodes() != null && index < item.serialCodes().size()) {
            return item.serialCodes().get(index);
        }
        if (item.barcodes() != null && !item.barcodes().isEmpty()) {
            return item.barcodes().get(0);
        }
        return item.partnerCode();
    }

    /**
     * Returns a summary of all currently visible inputs on the page (for
     * diagnostics).
     */
    private String getVisibleInputsSummary() {
        try {
            return driver.findElements(By.tagName("input")).stream()
                    .filter(e -> {
                        try {
                            return e.isDisplayed();
                        } catch (Exception ex) {
                            return false;
                        }
                    })
                    .map(e -> {
                        String id = e.getAttribute("id");
                        String ph = e.getAttribute("placeholder");
                        String type = e.getAttribute("type");
                        return "[id=" + id + " ph=\"" + ph + "\" type=" + type + "]";
                    })
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "(error reading inputs: " + e.getMessage() + ")";
        }
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

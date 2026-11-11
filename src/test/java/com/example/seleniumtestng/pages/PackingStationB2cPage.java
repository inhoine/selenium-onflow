package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickupItem;
import com.example.seleniumtestng.utils.Log;
import com.example.seleniumtestng.utils.ScanTable;
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
            "//button[contains(normalize-space(),'Kết nối trạm đóng hàng') or contains(normalize-space(),'Ket noi tram dong hang')]");
    private final By stationHeaderButton = By.cssSelector(".packing-workstation__table-button");
    private final By stationModal = By.cssSelector(".scan-packing-table-modal, .modal.show");

    // --- Basket / Trolley / Pickup Locators ---
    private final By basketInput = By.id("b2c-workstation-basket");
    private final By basketConfirmButton = By.xpath(
            "//div[input[@id='b2c-workstation-basket']]//button[normalize-space()='Nhập']");
    private final By resetBasketButton = By.cssSelector(".packing-workstation__reset-basket");

    // --- Product / SKU / Serial Locators ---
    private final By productScanInput = By.id("b2c-workstation-scan");
    private final By productConfirmButton = By.xpath(
            "//div[input[@id='b2c-workstation-scan']]//button[normalize-space()='Nhập']");

    // --- Additional Controls & Modals ---
    private final By detailButton = By.xpath(
            "//button[contains(normalize-space(),'Chi tiết') or contains(normalize-space(),'Chi tiet')]");
    private final By printerErrorModal = By.xpath(
            "//div[@aria-labelledby='qz-printer-error-title' or contains(.,'Kết nối máy in') or contains(.,'Chưa thể kết nối')][contains(@class,'show')]");
    private final By printerModalCloseButton = By.xpath(
            "//div[@aria-labelledby='qz-printer-error-title' or contains(.,'Kết nối máy in') or contains(.,'Chưa thể kết nối')]//button[normalize-space()='Đóng' or contains(@class,'btn-close') or @aria-label='Close']");

    // --- NVL (Packaging Material) Locators ---
    private final By stationMaterialScanInput = By.id("station-b2c-material-scan");
    private final By materialInput = By.xpath("//input[@placeholder='Quét hoặc nhập mã vật liệu đóng gói']");
    private final By materialPromptModal = By.xpath(
            "//div[contains(@class,'modal') and contains(@class,'show')]//*[contains(normalize-space(),'vật liệu') "
                    + "or contains(normalize-space(),'Vật liệu') or contains(normalize-space(),'NVL') "
                    + "or contains(normalize-space(),'bao bì') or contains(normalize-space(),'Bao bì') "
                    + "or contains(@class,'packing-workstation__material-scan')]");

    private final ScanTable scanTableHelper;

    public PackingStationB2cPage(WebDriver driver) {
        super(driver);
        this.scanTableHelper = new ScanTable(driver);
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
        // Allow React page to fully render before interacting with modals
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
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
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        input.sendKeys(tableCode);

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
        try {
            shortWait(5000).until(driver -> !isStationModalVisible());
            Log.info("Station modal closed automatically after connecting");
        } catch (Exception e) {
            // If a printer modal appeared, dismiss it
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
     * Closes the station scan modal using its X button or cancel button if still
     * visible.
     */
    private void closeStationModalIfPresent() {
        try {
            // Try X (btn-close) inside the station modal header
            List<WebElement> closeBtns = driver.findElements(By.xpath(
                    "//div[contains(@class,'scan-packing-table-modal')]//button[contains(@class,'btn-close') or @aria-label='Close']"));
            for (WebElement btn : closeBtns) {
                if (btn.isDisplayed()) {
                    jsClick(btn);
                    Log.info("Closed station modal via X button");
                    Thread.sleep(500);
                    return;
                }
            }
            // Fallback: "Huỷ bỏ" cancel button in station modal
            List<WebElement> cancelBtns = driver.findElements(By.xpath(
                    "//div[contains(@class,'scan-packing-table-modal')]//button[contains(normalize-space(),'Huỷ') or contains(normalize-space(),'Huy')]"));
            for (WebElement btn : cancelBtns) {
                if (btn.isDisplayed()) {
                    jsClick(btn);
                    Log.info("Closed station modal via cancel button");
                    Thread.sleep(500);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Step 2: Scan Basket / Trolley / Pickup Order Code
     */
    public void scanBasketOrPickup(String scanCode) {
        dismissPrinterModalIfPresent();
        WebElement input = wait.until(driver -> findVisibleInput(basketInput));
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(scanCode);
        } catch (ElementClickInterceptedException e) {
            dismissPrinterModalIfPresent();
            setInputValue(input, scanCode);
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
        // Allow page time to process the basket scan and render order/product area
        try {
            Thread.sleep(2500);
        } catch (InterruptedException ignored) {
        }
        dismissPrinterModalIfPresent();
        Log.info("Completed scanning basket/pickup code: {}. Visible inputs after scan: [{}]",
                scanCode, getVisibleInputsSummary());
    }

    /**
     * Step 3: Scan Product Item (Barcode / SKU / Serial).
     * Falls back to JavaScript value-set when direct sendKeys is not interactable.
     */
    public void scanProduct(String scanCode) {
        dismissPrinterModalIfPresent();

        // Use visibleWithin (null-safe) so we can log diagnostics on failure instead of
        // raw timeout
        WebElement input = visibleWithin(productScanInput, 15000);
        if (input == null) {
            // Check if NVL step already appeared (all products already scanned)
            if (isPackagingMaterialPromptVisible()) {
                Log.info("NVL step already visible when trying to scan product {} — stopping scan loop", scanCode);
                return;
            }
            String visibleInputs = getVisibleInputsSummary();
            Log.warn("Product scan input (id=b2c-workstation-scan) not visible after 15s. Visible inputs: [{}]",
                    visibleInputs);
            throw new org.openqa.selenium.TimeoutException(
                    "Product scan input (b2c-workstation-scan) not found. Visible inputs on page: " + visibleInputs);
        }

        boolean typed = false;
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(scanCode);
            typed = true;
        } catch (ElementClickInterceptedException e) {
            dismissPrinterModalIfPresent();
        } catch (ElementNotInteractableException e) {
            Log.warn("Product scan input not interactable via sendKeys — falling back to JS setInputValue");
        }

        if (!typed) {
            // JavaScript fallback: set value and dispatch React-compatible events
            setInputValue(input, scanCode);
        }

        // Press ENTER or click the confirm button to submit
        WebElement confirmBtn = findVisibleEnabled(productConfirmButton, 1000);
        if (confirmBtn != null) {
            try {
                confirmBtn.click();
            } catch (ElementNotInteractableException e) {
                jsClick(confirmBtn);
            }
        } else {
            try {
                input.sendKeys(Keys.ENTER);
            } catch (ElementNotInteractableException e) {
                // Input lost focus after JS set — dispatch Enter via JS
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].dispatchEvent(new KeyboardEvent('keydown', {key:'Enter',keyCode:13,bubbles:true}));",
                        input);
            }
        }

        dismissPrinterModalIfPresent();
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
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException ignored) {
                    }
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
        if (findVisibleInput(stationMaterialScanInput) != null) {
            return true;
        }
        for (WebElement element : driver.findElements(materialInput)) {
            try {
                if (element.isDisplayed() && element.isEnabled()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
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
     * Enters the material code into station-b2c-material-scan and submits with
     * ENTER.
     */
    public void scanPackagingMaterial(String materialCode) {
        scanPackagingMaterial(materialCode, true);
    }

    public void scanPackagingMaterial(String materialCode, boolean isLastMaterial) {
        dismissPrinterModalIfPresent();
        WebElement input = wait.until(driver -> findVisibleInput(materialInput));
        if (input == null) {
            throw new org.openqa.selenium.TimeoutException(
                    "Packaging material scan input (id=station-b2c-material-scan) not found");
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

        // Try confirm button next to input if present, otherwise send ENTER
        WebElement confirmBtn = findVisibleEnabled(By.xpath(
                "//div[input[@id='station-b2c-material-scan']]//button[normalize-space()='Nhập' or contains(@class,'btn-primary')]"),
                1000);
        if (confirmBtn != null) {
            try {
                confirmBtn.click();
            } catch (Exception e) {
                jsClick(confirmBtn);
            }
        } else {
            try {
                input.sendKeys(Keys.ENTER);
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].dispatchEvent(new KeyboardEvent('keydown', {key:'Enter',keyCode:13,which:13,bubbles:true}));"
                                + "arguments[0].dispatchEvent(new KeyboardEvent('keypress', {key:'Enter',keyCode:13,which:13,bubbles:true}));"
                                + "arguments[0].dispatchEvent(new KeyboardEvent('keyup', {key:'Enter',keyCode:13,which:13,bubbles:true}));",
                        input);
            }
        }

        dismissPrinterModalIfPresent();
        if (isLastMaterial) {
            // Wait briefly for material scan to process and confirm attached documents if modal appears
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            confirmAttachedDocumentsIfPresent();
            dismissPrinterModalIfPresent();
        } else {
            try {
                Thread.sleep(600);
            } catch (InterruptedException ignored) {
            }
        }
        Log.info("Completed scanning packaging material code: {} (isLastMaterial={})", materialCode, isLastMaterial);
    }

    public void confirmAttachedDocumentsIfPresent() {
        try {
            for (int i = 0; i < 6; i++) {
                List<WebElement> confirmButtons = driver.findElements(By.xpath(
                        "//button[@id='confirm-had-print-all' or contains(normalize-space(),'Xác nhận đã in hết') or contains(normalize-space(),'xac nhan da in het')]"));
                boolean confirmed = false;
                for (WebElement btn : confirmButtons) {
                    if (btn.isDisplayed() && btn.isEnabled()) {
                        jsClick(btn);
                        Log.info("Confirmed attached documents print");
                        confirmed = true;
                        break;
                    }
                }
                if (confirmed) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                    break;
                }
                dismissPrinterModalIfPresent();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Orchestrates complete packing for orders:
     * For each order, scans all product items, waits for the NVL step,
     * calls API to resolve packaging material codes, and scans each resolved
     * material code.
     */
    public void packOrdersWithMaterial(
            List<PackingOrder> packingOrders,
            com.example.seleniumtestng.clients.WmsApiClient apiClient,
            String token) {
        for (int i = 0; i < packingOrders.size(); i++) {
            PackingOrder order = packingOrders.get(i);
            Log.info("Packing order [{}/{}]: trackingCode={}", (i + 1), packingOrders.size(), order.trackingCode());

            boolean nvlAppeared = false;
            for (PickupItem item : order.items()) {
                if (nvlAppeared) {
                    break;
                }
                int quantity = item.quantitySold() > 0 ? item.quantitySold() : 1;
                for (int q = 0; q < quantity; q++) {
                    if (isPackagingMaterialPromptVisible()) {
                        Log.info("Packaging material step already appeared for trackingCode={}", order.trackingCode());
                        nvlAppeared = true;
                        break;
                    }

                    String scanCode = getItemScanCode(item, q);
                    Log.info("Scan product item [{}/{}]: code={}", (q + 1), quantity, scanCode);
                    scanProduct(scanCode);
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            // Resolve packaging materials to scan for this order
            List<String> materialCodes = apiClient.resolveMaterialScanCodes(order.trackingCode(), token);
            if (materialCodes.isEmpty()) {
                Log.info("Order {}: Tất cả NVL đều có auto_scan=true (hoặc không có NVL cần scan). Bỏ qua bước scan NVL.",
                        order.trackingCode());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
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
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    Log.warn("Packaging material prompt did not appear for order: {}", order.trackingCode());
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
        if (isStationModalVisible()) {
            Log.info("Station modal already visible, skip opening");
            return;
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

    private void waitForStationModalToClose() {
        try {
            shortWait(3000).until(driver -> {
                List<WebElement> inputs = driver.findElements(stationCodeInput);
                return inputs.isEmpty() || !inputs.get(0).isDisplayed();
            });
        } catch (RuntimeException ignored) {
            Log.warn("Station modal did not close within timeout");
        }
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
}

package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickupItem;
import com.example.seleniumtestng.utils.ScanTable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PickAndPackOrderPage extends BasePage {
    private static final long MATERIAL_DELAY_MS = 500;
    private static final String CUSTOMER_PACKING_MATERIAL_FALLBACK_CODE = "10x10x10";
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    private final By scanPackingTrolleyField = By.xpath("//input[@placeholder='Quét mã XE/ bảng kê cần đóng gói']");
    private final By receivePackingTrolleyBtn = By.xpath("//button[normalize-space()='Nhận bảng kê' or normalize-space()='Nhan bang ke']");
    private final By scanPickUpField = By.xpath("//input[contains(@placeholder,'Xe')"
            + " or contains(@placeholder,'Bảng kê')"
            + " or contains(@placeholder,'bảng kê')"
            + " or contains(@placeholder,'Bang ke')"
            + " or contains(@placeholder,'bang ke')"
            + " or contains(@placeholder,'Rổ')"
            + " or contains(@placeholder,'Ro')]");
    private final By scanSkuField = By.xpath("//input[contains(@placeholder,'Sản phẩm')"
            + " or contains(@placeholder,'sản phẩm')"
            + " or contains(@placeholder,'San pham')"
            + " or contains(@placeholder,'san pham')]");
    private final By packagingMaterialsField = By.xpath("//input[@placeholder='Quét hoặc nhập mã vật liệu đóng gói']");
    private final By fallbackPackagingMaterial = By.xpath("//*[contains(normalize-space(.),'Băng keo') or contains(normalize-space(.),'Bang keo')]");
    private final By productRows = By.xpath("//tr[.//div[contains(@id,'barcode_')]]");
    private final By currentTrackingCode = By.xpath("//*[contains(normalize-space(.),'Bạn đang đóng gói cho đơn hàng')"
            + " or contains(normalize-space(.),'Ban dang dong goi cho don hang')]");
    private final By packingDetailToggle = By.xpath("//*[normalize-space()='(Chi tiết)' or normalize-space()='(Chi tiet)']");
    private final By visibleModal = By.cssSelector(".modal.show");
    private final By visibleModalCloseButton = By.xpath("//*[contains(@class,'modal') and contains(@class,'show')]//button[contains(@class,'btn-close') or @aria-label='Close' or normalize-space()='×' or normalize-space()='Đóng' or normalize-space()='Dong']");
    private final By confirmScanPickupButton = By.xpath("//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");

    public PickAndPackOrderPage(WebDriver driver) {
        super(driver);
    }

    public boolean verifyToastMessageIfPresent(String message, long timeoutMillis) {
        try {
            String text = shortWait(timeoutMillis)
                    .until(driver -> {
                        List<WebElement> toasts = driver.findElements(By.xpath("//div[contains(@class,'Toastify__toast-body')]//div[contains(normalize-space(.),'" + message + "')]"));
                        for (WebElement toast : toasts) {
                            try {
                                if (toast.isDisplayed()) {
                                    return toast.getText();
                                }
                            } catch (RuntimeException ignored) {
                            }
                        }
                        return null;
                    });
            System.out.println("Toast: " + text);
            return true;
        } catch (RuntimeException ignored) {
            System.out.println("Toast not found, continue: " + message);
            return false;
        }
    }

    public void receivePackingTrolley(String pickupId) {
        driver.get(ConfigReader.required("WMS_BASE_URL") + "/receive-packing-trolley");
        WebElement input = visible(scanPackingTrolleyField);
        input.click();
        input.sendKeys(pickupId, Keys.ENTER);
        click(receivePackingTrolleyBtn);
    }

    public void scanTablePacking(String tableCode) {
        scanTablePacking(tableCode, "/packing");
    }

    public void scanTablePackingB2b(String tableCode) {
        scanTablePacking(tableCode, "/packing-b2b");
    }

    private void scanTablePacking(String tableCode, String packingPath) {
        driver.get(ConfigReader.required("WMS_BASE_URL") + packingPath);
        new ScanTable(driver).scanIfPresent(tableCode);
        waitForScanPickupInput();
    }

    public void scanPickUpOrder(String pickupId) {
        closeBlockingModalIfPresent();
        WebElement input = waitForScanPickupInput();
        try {
            clearAndEnter(input, pickupId);
        } catch (ElementClickInterceptedException error) {
            closeBlockingModalIfPresent();
            setInputValue(input, pickupId);
            input.sendKeys(Keys.ENTER);
        }
        clickConfirmScanPickupButtonIfPresent();
        waitForBasketScanToOpenPackingContext(pickupId);
        System.out.println("Scanned pickup/basket code: " + pickupId);
    }

    public int packBySystemSuggestion(List<PackingOrder> packingOrders, String materialCode) {
        Set<String> visitedTrackingCodes = new HashSet<>();
        int packedOrderCount = 0;
        for (PackingOrder order : packingOrders) {
            if (visitedTrackingCodes.contains(order.trackingCode())) {
                continue;
            }

            PickupItem firstPending = firstPendingItem(order);
            if (firstPending == null) {
                System.out.println("Skip " + order.trackingCode() + ": no pending item by API quantities");
                visitedTrackingCodes.add(order.trackingCode());
                continue;
            }

            String firstBarcode = getItemBarcode(firstPending);
            System.out.println("Scan first product to let system suggest order: tracking="
                    + order.trackingCode() + ", barcode=" + firstBarcode);
            scanProductBarcode(firstBarcode);
            String currentTracking = openCurrentOrSuggestedOrder(order.trackingCode());
            if (!visitedTrackingCodes.contains(currentTracking)) {
                System.out.println("Packing suggested tracking: " + currentTracking);
                packCurrentSuggestedOrder(materialCode, order, firstBarcode);
                visitedTrackingCodes.add(currentTracking);
                packedOrderCount++;
            } else {
                System.out.println("Skip already processed tracking: " + currentTracking);
            }
        }
        return packedOrderCount;
    }

    public String packNextBySystemSuggestion(
            List<PackingOrder> packingOrders,
            Set<String> visitedTrackingCodes,
            String materialCode) {
        for (PackingOrder order : packingOrders) {
            if (visitedTrackingCodes.contains(order.trackingCode())) {
                continue;
            }

            PickupItem firstPending = firstPendingItem(order);
            if (firstPending == null) {
                System.out.println("Skip " + order.trackingCode() + ": no pending item by API quantities");
                visitedTrackingCodes.add(order.trackingCode());
                continue;
            }

            String firstBarcode = getItemBarcode(firstPending);
            System.out.println("Scan first product to let system suggest order: tracking="
                    + order.trackingCode() + ", barcode=" + firstBarcode);
            scanProductBarcode(firstBarcode);
            String currentTracking = openCurrentOrSuggestedOrder(order.trackingCode());
            if (visitedTrackingCodes.contains(currentTracking)) {
                System.out.println("Skip already processed tracking: " + currentTracking);
                continue;
            }

            System.out.println("Packing suggested tracking: " + currentTracking);
            packCurrentSuggestedOrder(materialCode, order, firstBarcode);
            visitedTrackingCodes.add(currentTracking);
            return currentTracking;
        }
        return null;
    }

    public String packOneOrderBySystemSuggestion(PackingOrder order, String materialCode) {
        PickupItem firstPending = firstPendingItem(order);
        if (firstPending == null) {
            System.out.println("Skip " + order.trackingCode() + ": no pending item by API quantities");
            return null;
        }

        String firstBarcode = getItemBarcode(firstPending);
        System.out.println("Scan first product for mapped basket order: tracking="
                + order.trackingCode() + ", barcode=" + firstBarcode);
        scanProductBarcode(firstBarcode);
        String currentTracking = openCurrentOrSuggestedOrder(order.trackingCode());
        if (!order.trackingCode().equals(currentTracking)) {
            throw new IllegalStateException("Basket opened unexpected packing order. Expected tracking="
                    + order.trackingCode()
                    + ", actual tracking="
                    + currentTracking
                    + ", screen="
                    + summarizeScreenText());
        }

        System.out.println("Packing mapped basket tracking: " + currentTracking);
        packCurrentSuggestedOrder(materialCode, order, firstBarcode);
        return currentTracking;
    }

    public void packOrdersByTrackingCode(List<PackingOrder> packingOrders, String materialCode) {
        for (PackingOrder order : packingOrders) {
            boolean scanned = false;
            for (PickupItem item : order.items()) {
                String barcode = getItemBarcode(item);
                int quantityNeedScan = getQuantityNeedScan(item);
                if (barcode == null || quantityNeedScan <= 0) {
                    continue;
                }
                for (int i = 0; i < quantityNeedScan; i++) {
                    if (scanProductBarcode(barcode)) {
                        scanned = true;
                    } else {
                        break;
                    }
                }
            }
            if (scanned) {
                scanPackagingMaterial(materialCode);
                // sleep(MATERIAL_DELAY_MS);
            }
        }
    }

    private PickupItem firstPendingItem(PackingOrder order) {
        for (PickupItem item : order.items()) {
            if (getItemBarcode(item) != null && getQuantityNeedScan(item) > 0) {
                return item;
            }
        }
        return null;
    }

    private void packCurrentSuggestedOrder(
            String materialCode,
            PackingOrder order,
            String initiallyScannedBarcode) {
        boolean initialScanAccounted = false;
        for (PickupItem item : order.items()) {
            String barcode = getItemBarcode(item);
            int quantityToScan = getQuantityNeedScan(item);
            if (barcode == null || quantityToScan <= 0) {
                continue;
            }
            if (!initialScanAccounted && barcode.equals(initiallyScannedBarcode)) {
                quantityToScan = Math.max(0, quantityToScan - 1);
                initialScanAccounted = true;
            }
            for (int index = 0; index < quantityToScan; index++) {
                if (isPackagingMaterialPromptVisibleNow() || !scanProductBarcode(barcode)) {
                    break;
                }
            }
            if (isPackagingMaterialPromptVisibleNow()) {
                break;
            }
        }
        scanPackagingMaterial(materialCode);
    }

    private PackingUiItem firstPendingUiItem() {
        for (PackingUiItem item : getCurrentPackingItemsFromUi()) {
            if (item.needScan() > 0) {
                return item;
            }
        }
        return null;
    }

    private List<PackingUiItem> getCurrentPackingItemsFromUi() {
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
            System.out.println("Packing UI row: barcode=" + barcode
                    + ", qty=" + qtyText
                    + ", needScan=" + (total - packed)
                    + ", row=" + rowText.replaceAll("\\s+", " "));
        }
        return items;
    }

    private boolean scanProductBarcode(String barcode) {
        WebElement input = wait.until(driver -> {
            for (WebElement element : all(scanSkuField)) {
                try {
                    if (element.isDisplayed() && element.isEnabled()) {
                        return element;
                    }
                } catch (RuntimeException ignored) {
                }
            }
            return null;
        });
        try {
            scanIntoProductInput(input, barcode);
            return true;
        } catch (RuntimeException error) {
            if (isPackagingMaterialPromptVisibleNow() || !hasPendingUiItemsNow()) {
                System.out.println("Product scan is complete; switch to material scan.");
                return false;
            }
            throw error;
        }
    }

    private void scanIntoProductInput(WebElement input, String barcode) {
        try {
            clearAndEnter(input, barcode);
        } catch (ElementClickInterceptedException e) {
            setInputValue(input, barcode);
            input.sendKeys(Keys.ENTER);
        }
    }

    private void scanPackagingMaterial(String materialCode) {
        scanOrClickPackagingMaterial(materialCode);
        sleep(MATERIAL_DELAY_MS);
        if (isCustomerPackingMaterialToastPresent()
                && !CUSTOMER_PACKING_MATERIAL_FALLBACK_CODE.equalsIgnoreCase(materialCode)) {
            System.out.println("Packing material " + materialCode
                    + " is rejected because the order requires customer packing material. Retry with "
                    + CUSTOMER_PACKING_MATERIAL_FALLBACK_CODE);
            scanOrClickPackagingMaterial(CUSTOMER_PACKING_MATERIAL_FALLBACK_CODE);
            sleep(MATERIAL_DELAY_MS);
        }
        confirmAttachedDocumentsPrintedIfPresent();
    }

    private void scanOrClickPackagingMaterial(String materialCode) {
        WebElement input = findVisiblePackagingMaterialInput(5000);
        if (input == null) {
            clickFallbackPackagingMaterial(materialCode);
            return;
        }
        clearAndEnter(input, materialCode);
    }

    private WebElement findVisiblePackagingMaterialInput(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : all(packagingMaterialsField)) {
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

    private void clickFallbackPackagingMaterial(String materialCode) {
        WebElement material = findVisible(By.xpath("//*[contains(normalize-space(.),'" + materialCode + "')]"), 1000);
        if (material == null) {
            material = visible(fallbackPackagingMaterial);
        }
        jsClick(material);
    }

    private boolean isCustomerPackingMaterialToastPresent() {
        try {
            return shortWait(2000).until(driver -> {
                for (WebElement toast : driver.findElements(By.xpath("//div[contains(@class,'Toastify__toast-body')]"))) {
                    try {
                        if (!toast.isDisplayed()) {
                            continue;
                        }
                        String normalizedText = normalizedToastText(toast.getText());
                        if (normalizedText.contains("don hang yeu cau nvl dong goi cua khach hang")
                                || (normalizedText.contains("nvl")
                                && normalizedText.contains("dong goi")
                                && normalizedText.contains("khach hang"))) {
                            return true;
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void confirmAttachedDocumentsPrintedIfPresent() {
        WebElement confirmButton = findAttachedDocumentsPrintedConfirmButton(3000);
        if (confirmButton == null) {
            return;
        }

        jsClick(confirmButton);
        System.out.println("Confirmed all attached documents were printed");
        try {
            shortWait(5000).until(driver -> !isAttachedDocumentsModalVisibleNow() ? true : null);
        } catch (RuntimeException ignored) {
            System.out.println("Attached documents modal is still visible after confirming printed documents");
        }
    }

    private WebElement findAttachedDocumentsPrintedConfirmButton(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> findAttachedDocumentsPrintedConfirmButtonNow());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private WebElement findAttachedDocumentsPrintedConfirmButtonNow() {
        for (WebElement modal : driver.findElements(visibleModal)) {
            try {
                if (!modal.isDisplayed() || !isAttachedDocumentsModal(modal)) {
                    continue;
                }
                for (WebElement button : modal.findElements(By.cssSelector("button"))) {
                    if (!button.isDisplayed() || !button.isEnabled()) {
                        continue;
                    }
                    if (normalizeScanText(button.getText()).contains("xac nhan da in het")) {
                        return button;
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private boolean isAttachedDocumentsModalVisibleNow() {
        for (WebElement modal : driver.findElements(visibleModal)) {
            try {
                if (modal.isDisplayed() && isAttachedDocumentsModal(modal)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private boolean isAttachedDocumentsModal(WebElement modal) {
        String text = normalizeScanText(modal.getText());
        return text.contains("danh sach tai lieu dinh kem")
                || text.contains("vui long in tat ca cac tai lieu dinh kem");
    }

    private String normalizedToastText(String text) {
        if (text == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private boolean hasPendingUiItemsNow() {
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

    private void waitForBasketScanToOpenPackingContext(String pickupId) {
        try {
            shortWait(3000).until(driver -> isPackingContextOpenNow() ? true : null);
            return;
        } catch (RuntimeException ignored) {
            System.out.println("Packing context was not ready after scanning " + pickupId + "; retry scan once");
        }

        WebElement input = findScanPickupInput(2000);
        if (input == null) {
            throw new IllegalStateException("Packing context did not open after scanning " + pickupId
                    + ", and pickup scan input is not available. Screen=" + summarizeScreenText());
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
                        + ". Screen=" + summarizeScreenText(), error);
            }
            jsClick(confirmButton);
            try {
                shortWait(5000).until(driver -> isPackingContextOpenNow() ? true : null);
            } catch (RuntimeException retryError) {
                throw new IllegalStateException("Packing context did not open after confirming pickup/basket scan "
                        + pickupId
                        + ". Screen=" + summarizeScreenText(), retryError);
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
                String placeholder = normalizeScanText(input.getAttribute("placeholder"));
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

    private String normalizeScanText(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isPackingContextOpenNow() {
        if (findVisibleEnabledNow(scanSkuField) != null) {
            return true;
        }
        if (getVisibleElementTextNow(currentTrackingCode) != null) {
            return true;
        }
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

    private boolean isPackagingMaterialPromptVisibleNow() {
        for (WebElement element : driver.findElements(packagingMaterialsField)) {
            try {
                if (element.isDisplayed() && element.isEnabled()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private String openCurrentOrSuggestedOrder(String expectedTrackingCode) {
        String currentTracking = getCurrentPackingTrackingCodeIfPresent(2500);
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
                + summarizeScreenText());
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
        return getCurrentPackingTrackingCodeIfPresent(5000);
    }

    private WebElement findPackingButtonForTracking(String trackingCode, long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement row : driver.findElements(By.xpath("//tr[contains(.,'" + trackingCode + "')]"))) {
                    for (WebElement button : row.findElements(By.xpath(".//button[normalize-space()='Đóng gói' or normalize-space()='Dong goi']"))) {
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

    private void closeBlockingModalIfPresent() {
        if (findVisible(visibleModal, 500) == null) {
            return;
        }

        if (!clickVisibleModalCloseButton()) {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        }

        try {
            shortWait(3000).until(driver -> driver.findElements(visibleModal).stream()
                    .noneMatch(element -> {
                        try {
                            return element.isDisplayed();
                        } catch (RuntimeException ignored) {
                            return false;
                        }
                    }));
        } catch (RuntimeException ignored) {
            System.out.println("Modal still visible after close attempt; continue with input wait");
        }
    }

    private boolean clickVisibleModalCloseButton() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            WebElement closeButton = findVisible(visibleModalCloseButton, 1000);
            if (closeButton == null) {
                return false;
            }
            try {
                jsClick(closeButton);
                return true;
            } catch (StaleElementReferenceException ignored) {
                if (findVisible(visibleModal, 200) == null) {
                    return true;
                }
            }
        }
        return false;
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

    private String getVisibleElementTextNow(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed() && !element.getText().trim().isBlank()) {
                    return element.getText().trim();
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private String getCurrentPackingTrackingCodeIfPresent(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(currentTrackingCode)) {
                    try {
                        if (element.isDisplayed() && !element.getText().trim().isBlank()) {
                            String text = element.getText().trim();
                            String trackingCode = extractCurrentTrackingCode(text);
                            if (trackingCode != null) {
                                return trackingCode;
                            }
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

    private String extractCurrentTrackingCode(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?i)(?:đơn hàng|don hang)\\s+([A-Z0-9][A-Z0-9-]+)").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String summarizeScreenText() {
        String text = driver.findElement(By.tagName("body")).getText().replaceAll("\\s+", " ").trim();
        return text.length() <= 600 ? text : text.substring(0, 600);
    }

    private String getItemBarcode(PickupItem item) {
        if (item.barcodes() != null && !item.barcodes().isEmpty()) {
            return item.barcodes().get(0);
        }
        return item.partnerCode();
    }

    private int getQuantityNeedScan(PickupItem item) {
        return item.quantitySold() - item.quantityPick();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting", e);
        }
    }

    private static final class PackingUiItem {
        private final String barcode;
        private final int needScan;

        private PackingUiItem(String barcode, int needScan) {
            this.barcode = barcode;
            this.needScan = needScan;
        }

        private String barcode() {
            return barcode;
        }

        private int needScan() {
            return needScan;
        }
    }
}

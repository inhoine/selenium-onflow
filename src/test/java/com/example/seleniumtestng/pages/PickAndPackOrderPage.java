package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.PackingOrder;
import com.example.seleniumtestng.models.PickupItem;
import com.example.seleniumtestng.utils.ScanTable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PickAndPackOrderPage extends BasePage {
    private static final long MATERIAL_DELAY_MS = 500;

    private final By scanPackingTrolleyField = By.xpath("//input[@placeholder='Quét mã XE/ bảng kê cần đóng gói']");
    private final By receivePackingTrolleyBtn = By.xpath("//button[normalize-space()='Nhận bảng kê' or normalize-space()='Nhan bang ke']");
    private final By scanPickUpField = By.xpath("//input[@placeholder='Quét mã Xe/ Bảng kê/ Rổ']");
    private final By scanSkuField = By.xpath("//input[contains(@placeholder,'Sản phẩm') or contains(@placeholder,'San pham')]");
    private final By packagingMaterialsField = By.xpath("//input[@placeholder='Quét hoặc nhập mã vật liệu đóng gói']");
    private final By fallbackPackagingMaterial = By.xpath("//*[contains(normalize-space(.),'Băng keo') or contains(normalize-space(.),'Bang keo')]");
    private final By productRows = By.xpath("//tr[.//div[contains(@id,'barcode_')]]");
    private final By currentTrackingCode = By.xpath("//h6[contains(.,'Bạn đang đóng gói cho đơn hàng')]//span[contains(@class,'fw-medium')]");

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
        driver.get(ConfigReader.required("WMS_BASE_URL") + "/packing");
        new ScanTable(driver).scan(tableCode);
        visible(scanPickUpField);
    }

    public void scanPickUpOrder(String pickupId) {
        WebElement input = visible(scanPickUpField);
        clearAndEnter(input, pickupId);
    }

    public int packBySystemSuggestion(List<PackingOrder> packingOrders, String materialCode) {
        Set<String> processedTrackingCodes = new HashSet<>();
        for (PackingOrder order : packingOrders) {
            if (processedTrackingCodes.contains(order.trackingCode())) {
                continue;
            }

            PickupItem firstPending = firstPendingItem(order);
            if (firstPending == null) {
                System.out.println("Skip " + order.trackingCode() + ": no pending item by API quantities");
                processedTrackingCodes.add(order.trackingCode());
                continue;
            }

            String firstBarcode = getItemBarcode(firstPending);
            System.out.println("Scan first product to let system suggest order: tracking="
                    + order.trackingCode() + ", barcode=" + firstBarcode);
            scanProductBarcode(firstBarcode);
            String currentTracking = getCurrentPackingTrackingCode();
            if (!processedTrackingCodes.contains(currentTracking)) {
                System.out.println("Packing suggested tracking: " + currentTracking);
                packCurrentSuggestedOrder(materialCode);
                processedTrackingCodes.add(currentTracking);
            } else {
                System.out.println("Skip already processed tracking: " + currentTracking);
            }
        }
        return processedTrackingCodes.size();
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

    private void packCurrentSuggestedOrder(String materialCode) {
        while (true) {
            if (isPackagingMaterialPromptVisibleNow()) {
                break;
            }
            PackingUiItem pending = firstPendingUiItem();
            if (pending == null) {
                break;
            }
            if (!scanProductBarcode(pending.barcode())) {
                break;
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
            String barcode = row.findElement(By.xpath(".//div[contains(@id,'barcode_')]")).getText().trim();
            String qtyText = row.findElement(By.xpath(".//td[contains(@class,'text-right')]//h5")).getText().trim();
            String[] parts = qtyText.split("/");
            int packed = Integer.parseInt(parts[0].trim());
            int total = Integer.parseInt(parts[1].trim());
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
            jsClick(input);
            clearAndEnter(input, barcode);
            return true;
        } catch (RuntimeException error) {
            if (isPackagingMaterialPromptVisibleNow() || !hasPendingUiItemsNow()) {
                System.out.println("Product scan is complete; switch to material scan.");
                return false;
            }
            throw error;
        }
    }

    private void scanPackagingMaterial(String materialCode) {
        WebElement input = findVisiblePackagingMaterialInput(5000);
        if (input == null) {
            clickFallbackPackagingMaterial(materialCode);
            return;
        }
        clearAndEnter(input, materialCode);
        sleep(MATERIAL_DELAY_MS);
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
        sleep(MATERIAL_DELAY_MS);
    }

    private boolean hasPendingUiItemsNow() {
        List<WebElement> rows = all(productRows);
        if (rows.isEmpty()) {
            return true;
        }
        for (WebElement row : rows) {
            try {
                String qtyText = row.findElement(By.xpath(".//td[contains(@class,'text-right')]//h5")).getText().trim();
                String[] parts = qtyText.split("/");
                int packed = Integer.parseInt(parts[0].trim());
                int total = Integer.parseInt(parts[1].trim());
                if (total - packed > 0) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                return true;
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

    private String getCurrentPackingTrackingCode() {
        return visible(currentTrackingCode).getText().trim();
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

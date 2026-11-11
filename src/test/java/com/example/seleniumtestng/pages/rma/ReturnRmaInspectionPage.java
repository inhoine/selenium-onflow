package com.example.seleniumtestng.pages.rma;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.RmaInspectionItem;
import com.example.seleniumtestng.models.RmaReturnOrder;
import com.example.seleniumtestng.pages.BasePage;
import com.example.seleniumtestng.utils.ScanTable;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Main facade coordinating RMA return order inspection workflows:
 * - Scanning table, RMA code, return tracking code
 * - Scanning products via {@link RmaProductTableSection}
 * - Filling inspection details via {@link RmaInspectionDetailSection}
 * - Entering serial numbers via {@link RmaSerialModal}
 * - Completing inspection sessions
 */
public class ReturnRmaInspectionPage extends BasePage {
    private static final String ORDER_RMA_PATH = "/order-rma";
    private static final String INSPECTION_PATH = "/inspection-order-rma";

    private final By pageBody = By.tagName("body");
    private final By editableInputs = By.cssSelector(
            "input:not([type='hidden']), textarea, [contenteditable='true']");
    private final By clickableControls = By.cssSelector("button, [role='button'], a.btn");

    private final RmaSerialModal serialModal;
    private final RmaProductTableSection tableSection;
    private final RmaInspectionDetailSection detailSection;

    public ReturnRmaInspectionPage(WebDriver driver) {
        super(driver);
        this.serialModal = new RmaSerialModal(driver);
        this.tableSection = new RmaProductTableSection(driver);
        this.detailSection = new RmaInspectionDetailSection(driver);
    }

    public RmaSerialModal serialModal() {
        return serialModal;
    }

    public RmaProductTableSection tableSection() {
        return tableSection;
    }

    public RmaInspectionDetailSection detailSection() {
        return detailSection;
    }

    public void openOrderRmaList() {
        driver.get(ConfigReader.required("WMS_BASE_URL") + ORDER_RMA_PATH + "?");
        wait.withTimeout(Duration.ofSeconds(30)).until(ExpectedConditions.urlContains(ORDER_RMA_PATH));
        visible(pageBody);
        wait.until(driver -> !String.valueOf(driver.getCurrentUrl()).contains("/login"));
    }

    public void openInspectionPage() {
        driver.get(ConfigReader.required("WMS_BASE_URL") + INSPECTION_PATH);
        wait.withTimeout(Duration.ofSeconds(30)).until(ExpectedConditions.urlContains(INSPECTION_PATH));
        visible(pageBody);
        wait.until(driver -> !String.valueOf(driver.getCurrentUrl()).contains("/login"));
    }

    public void scanTableIfPresent(String tableCode) {
        new ScanTable(driver).scanIfPresent(tableCode);
        waitForScannerReady();
    }

    public void scanRmaCode(String rmaCode) {
        WebElement input = findInput(800, false, "rma", "phieu hoan", "ma phieu");
        if (input == null) {
            clickFirstActionIfPresent(300, "quet ma", "bat dau", "tao phien");
            input = findInput(2500, false, "rma", "phieu hoan", "ma phieu");
        }
        if (input == null) {
            throw new IllegalStateException("RMA scan input was not found. Screen=" + summarizeScreenText());
        }
        scanIntoInput(input, rmaCode);
        clickConfirmIfPresent(150);
        waitForRmaContext(rmaCode);
        System.out.println("Scanned RMA code: " + rmaCode);
    }

    public int inspectOrders(String rmaCode, List<RmaReturnOrder> orders) {
        int inspected = 0;
        for (RmaReturnOrder order : orders) {
            if (order.trackingCode() == null || order.trackingCode().isBlank()) {
                continue;
            }
            scanRmaCode(rmaCode);
            scanTrackingCode(order.trackingCode());
            int inspectedProducts = inspectProductsInOrder(order);
            System.out.println("Inspected RMA tracking: tracking="
                    + order.trackingCode()
                    + ", products="
                    + inspectedProducts);
            completeSessionIfPresent();
            inspected++;
        }
        return inspected;
    }

    public void completeSessionIfPresent() {
        long startedAt = System.nanoTime();
        detailSection.assertNoOpenInspectionFormBeforeSessionFinish();
        if (clickSessionFinishButtonFast() || clickFirstActionIfPresent(600, "hoan tat phien kiem")) {
            clickConfirmIfPresent();
            System.out.println("Completed RMA inspection session, finishMs=" + elapsedMillis(startedAt));
        } else {
            System.out.println("RMA session finish button not visible, finishCheckMs=" + elapsedMillis(startedAt));
        }
    }

    public String waitForAnyToast(long timeoutMillis) {
        return detailSection.waitForAnyToast(timeoutMillis);
    }

    private void waitForScannerReady() {
        try {
            shortWait(1800).until(driver -> {
                if (findInputNow(false, "rma", "phieu hoan", "tracking", "van don", "don hang") != null) {
                    return true;
                }
                return normalize(bodyText()).contains("hang hoan") ? true : null;
            });
        } catch (RuntimeException ignored) {
            System.out.println("Continue after table scan; RMA scanner did not stabilize immediately");
        }
    }

    private void waitForRmaContext(String rmaCode) {
        try {
            shortWait(3500).until(driver -> {
                String body = normalize(bodyText());
                if (body.contains(normalize(rmaCode))) {
                    return true;
                }
                return findInputNow(false, "tracking", "van don", "don hang", "ma don") != null ? true : null;
            });
        } catch (RuntimeException error) {
            throw new IllegalStateException("RMA context did not open after scanning "
                    + rmaCode
                    + ". Toast="
                    + waitForAnyToast(1000)
                    + ". Screen="
                    + summarizeScreenText(), error);
        }
    }

    private void scanTrackingCode(String trackingCode) {
        WebElement input = findInput(2500, true, "tracking", "van don", "don hang", "ma don", "awb");
        if (input == null) {
            throw new IllegalStateException("Tracking scan input was not found for "
                    + trackingCode
                    + ". Screen="
                    + summarizeScreenText());
        }
        scanIntoInput(input, trackingCode);
        clickConfirmIfPresent(150);
        waitForProductListOrInspectionDetail(trackingCode);
        System.out.println("Scanned return tracking code: " + trackingCode);
    }

    private void waitForInspectionDetailForm(String trackingCode) {
        try {
            shortWait(3500).until(driver -> detailSection.isInspectionDetailFormVisible() ? true : null);
        } catch (RuntimeException error) {
            throw new IllegalStateException("RMA inspection detail form did not open after scanning tracking "
                    + trackingCode
                    + ". Screen="
                    + summarizeScreenText(), error);
        }
    }

    private RmaInspectionItem scanProductsIfPresent(RmaReturnOrder order) {
        RmaInspectionItem uiItem = scanFirstUiProductIfPresent(order);
        if (uiItem != null) {
            detailSection.fillInspectionDetails(uiItem);
            return uiItem;
        }
        if (detailSection.isInspectionDetailFormVisible()) {
            RmaInspectionItem item = order.items().isEmpty() ? null : order.items().get(0);
            detailSection.fillInspectionDetails(item);
            return item;
        }
        if (!tableSection.isProductListVisibleFast()) {
            waitForProductListOrInspectionDetail(order.trackingCode(), 500);
        }
        uiItem = scanFirstUiProductIfPresent(order);
        if (uiItem != null) {
            detailSection.fillInspectionDetails(uiItem);
            return uiItem;
        }
        return null;
    }

    private void waitForProductListOrInspectionDetail(String trackingCode) {
        waitForProductListOrInspectionDetail(trackingCode, 4000);
    }

    private void waitForProductListOrInspectionDetail(String trackingCode, long timeoutMillis) {
        try {
            shortWait(timeoutMillis).until(driver -> {
                if (tableSection.isProductListVisibleFast() || detailSection.isInspectionDetailFormVisible()) {
                    return true;
                }
                return null;
            });
        } catch (RuntimeException error) {
            throw new IllegalStateException("RMA product list/detail did not open after scanning tracking "
                    + trackingCode
                    + ". Screen="
                    + summarizeScreenText(), error);
        }
    }

    private RmaInspectionItem scanFirstUiProductIfPresent(RmaReturnOrder order) {
        long scanStartedAt = System.nanoTime();
        RmaProductTableSection.RmaUiProduct product = tableSection.firstUiProduct(order);
        if (product == null) {
            return null;
        }
        WebElement productInput = tableSection.findProductScanInputFast(400);
        if (productInput == null) {
            productInput = findInput(700, false, "san pham", "sku", "barcode", "ma vach", "serial");
        }
        if (productInput == null) {
            throw new IllegalStateException("RMA product scan input was not found for tracking "
                    + order.trackingCode()
                    + " after reading pending product "
                    + product.sku()
                    + ". Screen="
                    + summarizeScreenText());
        }
        tableSection.scanProductCode(productInput, product.sku());
        waitForInspectionDetailForm(order.trackingCode());
        RmaInspectionItem matchedApiItem = findMatchingItem(order.items(), product.sku());
        List<String> serialCodes = matchedApiItem == null ? List.of() : matchedApiItem.serialCodes();
        String goodsCode = matchedApiItem == null || matchedApiItem.goodsCode() == null || matchedApiItem.goodsCode().isBlank()
                ? product.sku()
                : matchedApiItem.goodsCode();
        System.out.println("Scanned RMA product from UI list: tracking="
                + order.trackingCode()
                + ", sku="
                + product.sku()
                + ", outboundQuantity="
                + product.outboundQuantity()
                + ", checkedQuantity="
                + product.checkedQuantity()
                + ", scanMs="
                + elapsedMillis(scanStartedAt));
        return new RmaInspectionItem(
                product.sku(),
                goodsCode,
                List.of(product.sku()),
                product.outboundQuantity() - product.checkedQuantity(),
                serialCodes);
    }

    private RmaInspectionItem fillInspectionDetailsForOrder(RmaReturnOrder order) {
        waitForInspectionDetailForm(order.trackingCode());
        RmaInspectionItem item = order.items().isEmpty()
                ? null
                : order.items().get(0);
        detailSection.fillInspectionDetails(item);
        return item;
    }

    private int inspectProductsInOrder(RmaReturnOrder order) {
        int inspectedProducts = 0;
        int expectedProducts = Math.max(1, order.items().size());
        int maxProducts = expectedProducts + 2;
        while (inspectedProducts < maxProducts) {
            RmaInspectionItem inspectedItem = scanProductsIfPresent(order);
            if (inspectedItem == null) {
                if (inspectedProducts == 0 && detailSection.isInspectionDetailFormVisible()) {
                    inspectedItem = fillInspectionDetailsForOrder(order);
                } else {
                    break;
                }
            }
            detailSection.completeCurrentInspectionIfPresent(order.trackingCode(), inspectedItem, serialModal);
            inspectedProducts++;
            waitAfterScan(50);
            if (!order.items().isEmpty() && inspectedProducts >= expectedProducts) {
                System.out.println("RMA tracking reached API item count, finishing session without extra pending scan: tracking="
                        + order.trackingCode()
                        + ", products="
                        + inspectedProducts);
                break;
            }
        }
        if (inspectedProducts == 0) {
            if (tableSection.isProductListVisible()
                    && tableSection.firstUiProduct(order) == null
                    && tableSection.visiblePendingQuantityRowCount() == 0) {
                System.out.println("RMA tracking has no pending products: tracking="
                        + order.trackingCode()
                        + ", rows="
                        + tableSection.productRowsSummary());
                return 0;
            }
            throw new IllegalStateException("No pending RMA product was inspected for tracking "
                    + order.trackingCode()
                    + ". API items="
                    + order.items()
                    + ". Product rows="
                    + tableSection.productRowsSummary()
                    + ". Screen="
                    + summarizeScreenText());
        }
        if (inspectedProducts < expectedProducts
                && tableSection.isProductListVisible()
                && tableSection.firstUiProduct(order) != null) {
            throw new IllegalStateException("RMA tracking still has pending products after inspection loop: tracking="
                    + order.trackingCode()
                    + ". Product rows="
                    + tableSection.productRowsSummary()
                    + ". Screen="
                    + summarizeScreenText());
        }
        return inspectedProducts;
    }

    private RmaInspectionItem findMatchingItem(List<RmaInspectionItem> items, String sku) {
        if (items == null || sku == null || sku.isBlank()) {
            return null;
        }
        String expected = skuKey(sku);
        for (RmaInspectionItem item : items) {
            if (itemMatchesSku(item, expected)) {
                return item;
            }
        }
        return null;
    }

    private boolean itemMatchesSku(RmaInspectionItem item, String expectedSkuKey) {
        if (item == null || expectedSkuKey.isBlank()) {
            return false;
        }
        if (expectedSkuKey.equals(skuKey(item.partnerCode())) || expectedSkuKey.equals(skuKey(item.goodsCode()))) {
            return true;
        }
        if (item.barcodes() == null) {
            return false;
        }
        for (String barcode : item.barcodes()) {
            if (expectedSkuKey.equals(skuKey(barcode))) {
                return true;
            }
        }
        return false;
    }

    private String skuKey(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private boolean clickSessionFinishButtonFast() {
        try {
            Object clicked = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "const norm = value => (value || '').normalize('NFD')"
                            + ".replace(/[\\u0300-\\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')"
                            + ".toLowerCase().replace(/\\s+/g, ' ').trim();"
                            + "const visible = el => {"
                            + "  const rect = el.getBoundingClientRect();"
                            + "  const style = window.getComputedStyle(el);"
                            + "  return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden'"
                            + "    && style.display !== 'none' && !el.disabled;"
                            + "};"
                            + "const controls = Array.from(document.querySelectorAll('button, [role=\"button\"], a.btn'));"
                            + "const button = controls.find(el => visible(el)"
                            + "  && norm(el.innerText || el.textContent).includes('hoan tat phien kiem'));"
                            + "if (!button) return false;"
                            + "button.click();"
                            + "return true;");
            return Boolean.TRUE.equals(clicked);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private WebElement findInput(long timeoutMillis, boolean allowGenericFallback, String... keywords) {
        try {
            return shortWait(timeoutMillis).until(driver -> findInputNow(allowGenericFallback, keywords));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private WebElement findInputNow(boolean allowGenericFallback, String... keywords) {
        WebElement fallback = null;
        for (WebElement element : driver.findElements(editableInputs)) {
            if (!isUsableInput(element) || isTableInput(element)) {
                continue;
            }
            String combined = inputDescriptor(element);
            if (containsAny(combined, keywords)) {
                return element;
            }
            if (allowGenericFallback && fallback == null && !isRmaInput(combined)) {
                fallback = element;
            }
        }
        WebElement active = activeEditableElement();
        if (active != null && !isTableInput(active)) {
            String combined = inputDescriptor(active);
            if (containsAny(combined, keywords)) {
                return active;
            }
            if (allowGenericFallback && !isRmaInput(combined)) {
                return active;
            }
        }
        return fallback;
    }

    private boolean isTableInput(WebElement element) {
        String combined = inputDescriptor(element);
        return (combined.contains("ma ban") || combined.contains("ma tram") || combined.contains("tram"))
                && !combined.contains("rma")
                && !combined.contains("phieu hoan");
    }

    private boolean isRmaInput(String descriptor) {
        return descriptor.contains("rma")
                || descriptor.contains("phieu hoan")
                || descriptor.contains("ma phieu");
    }

    private String inputDescriptor(WebElement element) {
        return normalize(element.getAttribute("placeholder")
                + " " + element.getAttribute("aria-label")
                + " " + element.getAttribute("name")
                + " " + element.getAttribute("id")
                + " " + element.getAttribute("data-placeholder"));
    }

    private WebElement activeEditableElement() {
        try {
            Object active = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("return document.activeElement;");
            if (active instanceof WebElement && isUsableInput((WebElement) active)) {
                return (WebElement) active;
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean isUsableInput(WebElement element) {
        try {
            if (!element.isDisplayed() || !element.isEnabled()) {
                return false;
            }
            String tag = element.getTagName();
            String editable = element.getAttribute("contenteditable");
            return "input".equalsIgnoreCase(tag)
                    || "textarea".equalsIgnoreCase(tag)
                    || "true".equalsIgnoreCase(editable);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isClickableControl(WebElement element) {
        try {
            return element.isDisplayed() && element.isEnabled();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isDangerousAction(String normalizedText) {
        return normalizedText.contains("huy")
                || normalizedText.contains("xoa")
                || normalizedText.contains("bo qua")
                || normalizedText.contains("dang xuat");
    }

    private boolean clickFirstActionIfPresent(long timeoutMillis, String... keywords) {
        try {
            WebElement action = shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(clickableControls)) {
                    if (!isClickableControl(element)) {
                        continue;
                    }
                    String text = normalize(element.getText());
                    if (text.isBlank() || isDangerousAction(text)) {
                        continue;
                    }
                    if (containsAny(text, keywords)) {
                        return element;
                    }
                }
                return null;
            });
            clickElement(action);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void clickConfirmIfPresent() {
        clickConfirmIfPresent(250);
    }

    private void clickConfirmIfPresent(long timeoutMillis) {
        clickFirstActionIfPresent(timeoutMillis, "xac nhan", "dong y", "ok");
    }

    private void scanIntoInput(WebElement input, String code) {
        try {
            clearAndEnter(input, code);
        } catch (ElementClickInterceptedException e) {
            setInputValue(input, code);
            input.sendKeys(Keys.ENTER);
        }
    }

    private void clickElement(WebElement element) {
        try {
            element.click();
        } catch (RuntimeException e) {
            jsClick(element);
        }
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void waitAfterScan(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted after scan", e);
        }
    }

    private long elapsedMillis(long startedAtNanos) {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }

    private String bodyText() {
        try {
            return driver.findElement(pageBody).getText();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private String summarizeScreenText() {
        String text = bodyText().replaceAll("\\s+", " ").trim();
        return text.length() <= 600 ? text : text.substring(0, 600);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}


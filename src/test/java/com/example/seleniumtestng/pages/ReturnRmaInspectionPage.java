package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.RmaInspectionItem;
import com.example.seleniumtestng.models.RmaReturnOrder;
import com.example.seleniumtestng.utils.ScanTable;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ReturnRmaInspectionPage extends BasePage {
    private static final String ORDER_RMA_PATH = "/order-rma";
    private static final String INSPECTION_PATH = "/inspection-order-rma";

    private final By pageBody = By.tagName("body");
    private final By editableInputs = By.cssSelector(
            "input:not([type='hidden']), textarea, [contenteditable='true']");
    private final By clickableControls = By.cssSelector("button, [role='button'], a.btn");
    private final By submitInspectionButton = By.cssSelector("form button[type='submit'], button[type='submit']");
    private final By toastMessage = By.cssSelector(
            ".Toastify__toast-body, .ant-message-notice-content, .ant-notification-notice-message, "
                    + ".ant-notification-notice-description");
    private final By goodQtyField = By.xpath(
            "//input[@name='quantity_goods_normal' or @name='quantity_received' or @name='quantity_receive' "
                    + "or contains(@name,'normal') or contains(@name,'received') or contains(@name,'receive')]");
    private final By missingQtyField = By.xpath(
            "//input[@name='quantity_goods_lost' or @name='quantity_lost' or contains(@name,'lost') "
                    + "or contains(@name,'missing') or contains(@name,'lack')]");
    private final By damagedType1QtyField = By.xpath(
            "//input[@name='quantity_goods_damaged' or @name='quantity_damaged' or contains(@name,'damaged')]");
    private final By damagedType2QtyField = By.xpath(
            "//input[@name='quantity_goods_damaged_2' or @name='quantity_damaged_2']");
    private final By damagedType3QtyField = By.xpath(
            "//input[@name='quantity_goods_damaged_3' or @name='quantity_damaged_3']");
    private final By manufactureDateFields = By.cssSelector(
            "input[name*='manufact'], input[name*='mfg'], input[name*='production'], "
                    + "input[name*='produce'], input[name*='nsx']");
    private final By manufactureDateByLabel = By.cssSelector("input[name='manufacturing_date']");
    private final By expiryDateFields = By.cssSelector(
            "input[name*='expiry'], input[name*='expire'], input[name*='expired'], "
                    + "input[name*='expiration'], input[name*='exp_date']");
    private final By expiryDateByLabel = By.cssSelector("input[name='expiry_date']");
    private final By batchLotField = By.cssSelector(
            "input[name='batch_lot_code'], input[name='batchLotCode'], input[name*='batch'], input[name*='lot']");
    private final By noteField = By.xpath("//textarea[contains(@name,'note') or contains(@placeholder,'ghi chú') or contains(@placeholder,'ghi chu')]"
            + " | //input[contains(@name,'note') or contains(@placeholder,'ghi chú') or contains(@placeholder,'ghi chu')]");
    private final By lengthField = By.cssSelector("input[name='goods_d']");
    private final By widthField = By.cssSelector("input[name='goods_w']");
    private final By heightField = By.cssSelector("input[name='goods_h']");
    private final By weightField = By.cssSelector("input[name='goods_weight']");
    private final By productRows = By.cssSelector("tbody tr, [role='row']");

    public ReturnRmaInspectionPage(WebDriver driver) {
        super(driver);
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
        assertNoOpenInspectionFormBeforeSessionFinish();
        if (clickSessionFinishButtonFast() || clickFirstActionIfPresent(600, "hoan tat phien kiem")) {
            clickConfirmIfPresent();
            System.out.println("Completed RMA inspection session, finishMs=" + elapsedMillis(startedAt));
        } else {
            System.out.println("RMA session finish button not visible, finishCheckMs=" + elapsedMillis(startedAt));
        }
    }

    public String waitForAnyToast(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement toast : driver.findElements(toastMessage)) {
                    if (displayed(toast) && !toast.getText().trim().isBlank()) {
                        return toast.getText().trim();
                    }
                }
                return null;
            });
        } catch (RuntimeException e) {
            return "";
        }
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
            shortWait(3500).until(driver -> isInspectionDetailFormVisible() ? true : null);
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
            fillInspectionDetails(uiItem);
            return uiItem;
        }
        if (isInspectionDetailFormVisible()) {
            RmaInspectionItem item = order.items().isEmpty() ? null : order.items().get(0);
            fillInspectionDetails(item);
            return item;
        }
        if (!isProductListVisibleFast()) {
            waitForProductListOrInspectionDetail(order.trackingCode(), 500);
        }
        uiItem = scanFirstUiProductIfPresent(order);
        if (uiItem != null) {
            fillInspectionDetails(uiItem);
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
                if (isProductListVisibleFast() || isInspectionDetailFormVisible()) {
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
        RmaUiProduct product = firstUiProduct(order);
        if (product == null) {
            return null;
        }
        WebElement productInput = findProductScanInputFast(400);
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
        scanProductCode(productInput, product.sku());
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
        fillInspectionDetails(item);
        return item;
    }

    private int inspectProductsInOrder(RmaReturnOrder order) {
        int inspectedProducts = 0;
        int expectedProducts = Math.max(1, order.items().size());
        int maxProducts = expectedProducts + 2;
        while (inspectedProducts < maxProducts) {
            RmaInspectionItem inspectedItem = scanProductsIfPresent(order);
            if (inspectedItem == null) {
                if (inspectedProducts == 0 && isInspectionDetailFormVisible()) {
                    inspectedItem = fillInspectionDetailsForOrder(order);
                } else {
                    break;
                }
            }
            completeCurrentInspectionIfPresent(order.trackingCode(), inspectedItem);
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
            if (isProductListVisible() && firstUiProduct(order) == null && visiblePendingQuantityRowCount() == 0) {
                System.out.println("RMA tracking has no pending products: tracking="
                        + order.trackingCode()
                        + ", rows="
                        + productRowsSummary());
                return 0;
            }
            throw new IllegalStateException("No pending RMA product was inspected for tracking "
                    + order.trackingCode()
                    + ". API items="
                    + order.items()
                    + ". Product rows="
                    + productRowsSummary()
                    + ". Screen="
                    + summarizeScreenText());
        }
        if (inspectedProducts < expectedProducts && isProductListVisible() && firstUiProduct(order) != null) {
            throw new IllegalStateException("RMA tracking still has pending products after inspection loop: tracking="
                    + order.trackingCode()
                    + ". Product rows="
                    + productRowsSummary()
                    + ". Screen="
                    + summarizeScreenText());
        }
        return inspectedProducts;
    }

    private void fillInspectionDetails(RmaInspectionItem item) {
        if (!isInspectionDetailFormVisible()) {
            throw new IllegalStateException("RMA inspection detail form is not visible before filling. Screen="
                    + summarizeScreenText());
        }
        int quantity = item == null ? 1 : Math.max(1, item.quantity());
        inputInspectionQuantities(quantity);
        inputBatchLotIfPresent();
        boolean expiryFieldVisible = hasExpiryFieldVisible();
        if (expiryFieldVisible) {
            inputManufactureDateIfPresent();
        }
        inputInspectionNoteIfPresent();
        inputProductDimensionsIfPresent();
        assertRequiredInspectionFieldsFilled(quantity, expiryFieldVisible);
    }

    private boolean isInspectionDetailFormVisible() {
        return findVisibleEnabledNow(goodQtyField) != null
                || findVisibleEnabledNow(missingQtyField) != null
                || containsAny(normalize(bodyText()), "sl hang tot", "sl hang thieu", "hu hong loai", "ngay san xuat", "ngay het han");
    }

    private boolean isProductListVisible() {
        return isProductListVisibleFast()
                || containsAny(normalize(bodyText()), "san pham trong don hoan tra", "danh sach san pham trong kien");
    }

    private boolean isProductListVisibleFast() {
        try {
            Object visible = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "const norm = value => (value || '').normalize('NFD')"
                            + ".replace(/[\\u0300-\\u036f]/g, '').toLowerCase().replace(/\\s+/g, ' ').trim();"
                            + "const visible = el => {"
                            + "  const rect = el.getBoundingClientRect();"
                            + "  const style = window.getComputedStyle(el);"
                            + "  return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden'"
                            + "    && style.display !== 'none' && !el.disabled;"
                            + "};"
                            + "const hasProductScanner = Array.from(document.querySelectorAll("
                            + "  \"input:not([type='hidden']), textarea, [contenteditable='true']\"))"
                            + "  .some(el => visible(el) && norm([el.placeholder, el.getAttribute('aria-label'),"
                            + "    el.name, el.id, el.getAttribute('data-placeholder')].join(' ')).includes('san pham'));"
                            + "const hasRows = Array.from(document.querySelectorAll('tbody tr')).some(visible);"
                            + "return hasProductScanner && hasRows;");
            return Boolean.TRUE.equals(visible);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void openFirstProductFromList(String trackingCode) {
        WebElement action = findFirstProductRowAction(3000);
        if (action == null) {
            throw new IllegalStateException("No RMA product row/action was found after scanning tracking "
                    + trackingCode
                    + ". Screen="
                    + summarizeScreenText());
        }
        clickElement(action);
        waitForInspectionDetailForm(trackingCode);
        System.out.println("Opened first RMA product from product list for tracking: " + trackingCode);
    }

    private RmaUiProduct firstUiProduct(RmaReturnOrder order) {
        RmaUiProduct fastProduct = firstUiProductFast(order);
        if (fastProduct != null) {
            return fastProduct;
        }
        for (WebElement row : driver.findElements(By.cssSelector("tbody tr"))) {
            if (!displayed(row) || isHeaderLikeRow(row)) {
                continue;
            }
            String sku = skuFromRow(row, order == null ? List.of() : order.items());
            int outboundQuantity = outboundQuantityFromRow(row);
            int checkedQuantity = checkedQuantityFromRow(row);
            if (sku != null && !sku.isBlank() && outboundQuantity > checkedQuantity) {
                return new RmaUiProduct(sku, outboundQuantity, checkedQuantity);
            }
        }
        return null;
    }

    private RmaUiProduct firstUiProductFast(RmaReturnOrder order) {
        try {
            Object result = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    """
                    const apiGroups = arguments[0] || [];
                    const norm = value => (value || '').normalize('NFD')
                      .replace(/[\\u0300-\\u036f]/g, '')
                      .toLowerCase()
                      .replace(/\\s+/g, ' ')
                      .trim();
                    const visible = el => {
                      const rect = el.getBoundingClientRect();
                      const style = window.getComputedStyle(el);
                      return rect.width > 0 && rect.height > 0
                        && style.visibility !== 'hidden'
                        && style.display !== 'none';
                    };
                    const skuKey = value => String(value || '').replace(/[^A-Za-z0-9]/g, '').toLowerCase();
                    const firstInt = value => {
                      const match = String(value || '').match(/\\b\\d+\\b/);
                      return match ? parseInt(match[0], 10) : null;
                    };
                    const ints = value => Array.from(String(value || '').matchAll(/\\b\\d+\\b/g))
                      .map(match => parseInt(match[0], 10));
                    const likelySku = token => {
                      const upper = String(token || '').toUpperCase();
                      if (!upper || upper.startsWith('RMA-') || upper.startsWith('NHSVC')) return false;
                      if (upper.includes('-') || upper.includes('_')) return true;
                      return /^NH[A-Z0-9]{6,}$/.test(upper) || /^[A-Z]{2,}[0-9][A-Z0-9_]{3,}$/.test(upper);
                    };
                    const tokenFromText = value => {
                      const lines = String(value || '').split(/\\n+/).map(line => line.trim()).filter(Boolean);
                      for (let index = lines.length - 1; index >= 0; index--) {
                        const matches = lines[index].match(/\\b[A-Z][A-Z0-9]*(?:[-_][A-Z0-9]+)*\\b/g) || [];
                        for (const token of matches) {
                          if (likelySku(token)) return token;
                        }
                      }
                      return null;
                    };
                    for (const row of Array.from(document.querySelectorAll('tbody tr'))) {
                      if (!visible(row)) continue;
                      const rowText = row.innerText || row.textContent || '';
                      const normalizedRow = norm(rowText);
                      if (!normalizedRow || (normalizedRow.includes('stt') && normalizedRow.includes('thong tin san pham'))) {
                        continue;
                      }
                      const cells = Array.from(row.querySelectorAll('td, [role="cell"]'))
                        .filter(visible)
                        .map(cell => (cell.innerText || cell.textContent || '').trim())
                        .filter(Boolean);
                      let outbound = cells.length >= 4 ? firstInt(cells[3]) : null;
                      let checked = cells.length >= 5 ? firstInt(cells[4]) : null;
                      const rowNumbers = ints(rowText);
                      if (outbound === null && rowNumbers.length >= 2) outbound = rowNumbers[rowNumbers.length - 2];
                      if (checked === null && rowNumbers.length >= 1) checked = rowNumbers[rowNumbers.length - 1];
                      outbound = outbound || 0;
                      checked = checked || 0;
                      if (outbound <= checked) continue;

                      const rowKey = skuKey(rowText);
                      let sku = null;
                      for (const group of apiGroups) {
                        for (const candidate of group || []) {
                          if (candidate && rowKey.includes(skuKey(candidate))) {
                            sku = candidate;
                            break;
                          }
                        }
                        if (sku) break;
                      }
                      sku = sku || tokenFromText(cells[1] || rowText);
                      if (sku) return [sku, outbound, checked];
                    }
                    return null;
                    """,
                    apiCandidateGroups(order));
            if (result instanceof List<?> values && values.size() >= 3) {
                String sku = String.valueOf(values.get(0));
                int outboundQuantity = scriptInt(values.get(1));
                int checkedQuantity = scriptInt(values.get(2));
                if (!sku.isBlank() && outboundQuantity > checkedQuantity) {
                    return new RmaUiProduct(sku, outboundQuantity, checkedQuantity);
                }
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private int visiblePendingProductCount(RmaReturnOrder order) {
        int count = 0;
        for (WebElement row : driver.findElements(By.cssSelector("tbody tr"))) {
            if (!displayed(row) || isHeaderLikeRow(row)) {
                continue;
            }
            String sku = skuFromRow(row, order == null ? List.of() : order.items());
            if (sku != null && !sku.isBlank() && outboundQuantityFromRow(row) > checkedQuantityFromRow(row)) {
                count++;
            }
        }
        return count;
    }

    private int visiblePendingQuantityRowCount() {
        try {
            Object result = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    """
                    const visible = el => {
                      const rect = el.getBoundingClientRect();
                      const style = window.getComputedStyle(el);
                      return rect.width > 0 && rect.height > 0
                        && style.visibility !== 'hidden'
                        && style.display !== 'none';
                    };
                    const firstInt = value => {
                      const match = String(value || '').match(/\\b\\d+\\b/);
                      return match ? parseInt(match[0], 10) : null;
                    };
                    const ints = value => Array.from(String(value || '').matchAll(/\\b\\d+\\b/g))
                      .map(match => parseInt(match[0], 10));
                    let count = 0;
                    for (const row of Array.from(document.querySelectorAll('tbody tr'))) {
                      if (!visible(row)) continue;
                      const text = row.innerText || row.textContent || '';
                      if (!text.trim()) continue;
                      const cells = Array.from(row.querySelectorAll('td, [role="cell"]'))
                        .filter(visible)
                        .map(cell => (cell.innerText || cell.textContent || '').trim())
                        .filter(Boolean);
                      let outbound = cells.length >= 4 ? firstInt(cells[3]) : null;
                      let checked = cells.length >= 5 ? firstInt(cells[4]) : null;
                      const rowNumbers = ints(text);
                      if (outbound === null && rowNumbers.length >= 2) outbound = rowNumbers[rowNumbers.length - 2];
                      if (checked === null && rowNumbers.length >= 1) checked = rowNumbers[rowNumbers.length - 1];
                      if ((outbound || 0) > (checked || 0)) count++;
                    }
                    return count;
                    """);
            return scriptInt(result);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private String skuFromRow(WebElement row, List<RmaInspectionItem> apiItems) {
        String apiScanCode = scanCodeFromApiItemsInRow(row.getText(), apiItems);
        if (apiScanCode != null) {
            return apiScanCode;
        }
        List<String> texts = visibleCellTexts(row);
        for (String text : texts) {
            String sku = firstSkuLikeToken(readMostSpecificProductLine(text));
            if (sku != null) {
                return sku;
            }
        }
        return firstSkuLikeToken(readMostSpecificProductLine(row.getText()));
    }

    private int outboundQuantityFromRow(WebElement row) {
        List<String> texts = visibleCellTexts(row);
        if (texts.size() >= 4) {
            Integer quantity = firstInteger(texts.get(3));
            if (quantity != null) {
                return quantity;
            }
        }
        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\b\\d+\\b").matcher(row.getText());
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        return numbers.size() >= 2 ? numbers.get(numbers.size() - 2) : 0;
    }

    private int checkedQuantityFromRow(WebElement row) {
        List<String> texts = visibleCellTexts(row);
        if (texts.size() >= 5) {
            Integer quantity = firstInteger(texts.get(4));
            if (quantity != null) {
                return quantity;
            }
        }
        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\b\\d+\\b").matcher(row.getText());
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        return numbers.isEmpty() ? 0 : numbers.get(numbers.size() - 1);
    }

    private List<String> visibleCellTexts(WebElement row) {
        List<String> texts = new ArrayList<>();
        for (WebElement cell : row.findElements(By.cssSelector("td, [role='cell']"))) {
            try {
                if (cell.isDisplayed() && !cell.getText().trim().isBlank()) {
                    texts.add(cell.getText().trim());
                }
            } catch (RuntimeException ignored) {
            }
        }
        return texts;
    }

    private String productRowsSummary() {
        List<String> rows = new ArrayList<>();
        for (WebElement row : driver.findElements(By.cssSelector("tbody tr"))) {
            if (!displayed(row) || isHeaderLikeRow(row)) {
                continue;
            }
            rows.add(visibleCellTexts(row).toString());
            if (rows.size() >= 5) {
                break;
            }
        }
        return rows.toString();
    }

    private String firstSkuLikeToken(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\b[A-Z][A-Z0-9]*(?:[-_][A-Z0-9]+)*\\b").matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (isLikelyProductScanCode(token)) {
                return token;
            }
        }
        return null;
    }

    private String readMostSpecificProductLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.split("\\R+");
        for (int index = lines.length - 1; index >= 0; index--) {
            String line = lines[index].trim();
            if (!line.isBlank()) {
                return line;
            }
        }
        return text;
    }

    private String scanCodeFromApiItemsInRow(String rowText, List<RmaInspectionItem> apiItems) {
        String rowKey = skuKey(rowText);
        for (RmaInspectionItem item : apiItems) {
            for (String candidate : scanCodeCandidates(item)) {
                if (!candidate.isBlank() && rowKey.contains(skuKey(candidate))) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private List<String> scanCodeCandidates(RmaInspectionItem item) {
        List<String> candidates = new ArrayList<>();
        if (item == null) {
            return candidates;
        }
        addIfNotBlank(candidates, item.partnerCode());
        if (item.barcodes() != null) {
            for (String barcode : item.barcodes()) {
                addIfNotBlank(candidates, barcode);
            }
        }
        addIfNotBlank(candidates, item.goodsCode());
        return candidates;
    }

    private void addIfNotBlank(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
    }

    private boolean isLikelyProductScanCode(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String normalized = token.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("RMA-") || normalized.startsWith("NHSVC")) {
            return false;
        }
        if (normalized.contains("-") || normalized.contains("_")) {
            return true;
        }
        return normalized.matches("NH[A-Z0-9]{6,}") || normalized.matches("[A-Z]{2,}[0-9][A-Z0-9_]{3,}");
    }

    private List<List<String>> apiCandidateGroups(RmaReturnOrder order) {
        List<List<String>> groups = new ArrayList<>();
        if (order == null || order.items() == null) {
            return groups;
        }
        for (RmaInspectionItem item : order.items()) {
            List<String> candidates = scanCodeCandidates(item);
            if (!candidates.isEmpty()) {
                groups.add(candidates);
            }
        }
        return groups;
    }

    private int scriptInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        Integer parsed = firstInteger(String.valueOf(value));
        return parsed == null ? 0 : parsed;
    }

    private Integer firstInteger(String text) {
        Matcher matcher = Pattern.compile("\\b\\d+\\b").matcher(text == null ? "" : text);
        return matcher.find() ? Integer.parseInt(matcher.group()) : null;
    }

    private void clickProductScanButtonIfPresent() {
        clickFirstActionIfPresent(250, "quet ma");
    }

    private WebElement findProductScanInputFast(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                Object element = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "const norm = value => (value || '').normalize('NFD')"
                                + ".replace(/[\\u0300-\\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')"
                                + ".toLowerCase().replace(/\\s+/g, ' ').trim();"
                                + "const visible = el => {"
                                + "  const rect = el.getBoundingClientRect();"
                                + "  const style = window.getComputedStyle(el);"
                                + "  return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden'"
                                + "    && style.display !== 'none' && !el.disabled && !el.readOnly;"
                                + "};"
                                + "const inputs = Array.from(document.querySelectorAll("
                                + "  \"input:not([type='hidden']), textarea, [contenteditable='true']\"));"
                                + "return inputs.find(el => {"
                                + "  if (!visible(el)) return false;"
                                + "  const descriptor = norm(["
                                + "    el.getAttribute('placeholder'),"
                                + "    el.getAttribute('aria-label'),"
                                + "    el.getAttribute('name'),"
                                + "    el.getAttribute('id'),"
                                + "    el.getAttribute('data-placeholder')"
                                + "  ].join(' '));"
                                + "  if (descriptor.includes('rma') || descriptor.includes('phieu hoan')) return false;"
                                + "  if (descriptor.includes('tracking') || descriptor.includes('van don')) return false;"
                                + "  return descriptor.includes('san pham') || descriptor.includes('sku')"
                                + "    || descriptor.includes('barcode') || descriptor.includes('ma vach')"
                                + "    || descriptor.includes('serial');"
                                + "}) || null;");
                return element instanceof WebElement && isUsableInput((WebElement) element)
                        ? (WebElement) element
                        : null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void scanProductCode(WebElement input, String code) {
        try {
            setScanInputValue(input, code);
            input.sendKeys(Keys.ENTER);
        } catch (RuntimeException error) {
            scanIntoInput(input, code);
        }
        if (!waitForInspectionDetailFormIfVisible(250) && !clickProductScanButtonFast()) {
            clickProductScanButtonIfPresent();
        }
    }

    private void setScanInputValue(WebElement input, String value) {
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});"
                        + "arguments[0].focus();"
                        + "const value = arguments[1];"
                        + "const tag = (arguments[0].tagName || '').toLowerCase();"
                        + "const proto = tag === 'textarea' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;"
                        + "const setter = Object.getOwnPropertyDescriptor(proto, 'value').set;"
                        + "setter.call(arguments[0], '');"
                        + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));"
                        + "setter.call(arguments[0], value);"
                        + "for (const type of ['input', 'change']) {"
                        + "  arguments[0].dispatchEvent(new Event(type, {bubbles: true}));"
                        + "}",
                input,
                value);
    }

    private boolean waitForInspectionDetailFormIfVisible(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> isInspectionDetailFormVisible() ? true : null);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean clickProductScanButtonFast() {
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
                            + "const buttons = Array.from(document.querySelectorAll('button, [role=\"button\"], a.btn'));"
                            + "const button = buttons.find(el => visible(el) && norm(el.innerText || el.textContent).includes('quet ma'));"
                            + "if (!button) return false;"
                            + "button.click();"
                            + "return true;");
            return Boolean.TRUE.equals(clicked);
        } catch (RuntimeException ignored) {
            return false;
        }
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

    private WebElement findFirstProductRowAction(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement row : driver.findElements(productRows)) {
                    if (!displayed(row) || isHeaderLikeRow(row)) {
                        continue;
                    }
                    WebElement button = firstRowActionButton(row);
                    if (button != null) {
                        return button;
                    }
                    return row;
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private WebElement firstRowActionButton(WebElement row) {
        for (WebElement button : row.findElements(By.cssSelector("button, [role='button'], a"))) {
            try {
                if (button.isDisplayed() && button.isEnabled() && !isDangerousAction(normalize(button.getText()))) {
                    return button;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private boolean isHeaderLikeRow(WebElement row) {
        String text = normalize(row.getText());
        return text.isBlank()
                || (text.contains("stt") && text.contains("thong tin san pham"))
                || text.contains("ma rma ma don hang");
    }

    private void inputInspectionQuantities(int goodQuantity) {
        fillNumberField(goodQtyField, "sl hang tot", goodQuantity);
    }

    private void inputManufactureDateIfPresent() {
        int shelfLifeDays = Math.max(1, shelfLifeDays());
        Integer minimumExpiryDays = minimumExpiryDays();
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = LocalDate.parse(ConfigReader.getOrDefault("INBOUND_EXPIRY_DATE", "2027-12-31"));
        LocalDate manufactureDate = expiryDate.minusDays(shelfLifeDays - 1L);

        if (minimumExpiryDays != null
                && (expiryDate.isBefore(today.plusDays(minimumExpiryDays)) || manufactureDate.isAfter(today))) {
            expiryDate = today.plusDays(minimumExpiryDays);
            manufactureDate = expiryDate.minusDays(shelfLifeDays - 1L);
        } else if (manufactureDate.isAfter(today)) {
            manufactureDate = today;
            expiryDate = manufactureDate.plusDays(shelfLifeDays - 1L);
        }

        boolean manufactureFilled = fillDateIfPresent(manufactureDateByLabel, manufactureDateFields, "ngay san xuat", manufactureDate);
        if (manufactureFilled) {
            return;
        }

        LocalDate fallbackManufactureDate = today;
        fillDateIfPresent(manufactureDateByLabel, manufactureDateFields, "ngay san xuat", fallbackManufactureDate);
    }

    private boolean hasExpiryFieldVisible() {
        return findVisibleEnabledNow(expiryDateByLabel) != null
                || findVisibleEnabledNow(expiryDateFields) != null
                || findInputNearNormalizedLabel("ngay het han") != null
                || findInputNearNormalizedLabel("han su dung") != null;
    }

    private void inputBatchLotIfPresent() {
        String batchLot = ConfigReader.getOrDefault("RMA_INSPECTION_BATCH_LOT", String.valueOf(System.currentTimeMillis()));
        fillTextField(batchLotField, "ma batch/lot", batchLot);
    }

    private void inputInspectionNoteIfPresent() {
        String note = ConfigReader.getOrDefault("RMA_INSPECTION_NOTE", "Automation QC hang hoan");
        fillTextField(noteField, "ghi chu kiem hang", note);
    }

    private void inputProductDimensionsIfPresent() {
        fillVisibleNumberField(lengthField, ConfigReader.requiredInt("INBOUND_LENGTH"));
        fillVisibleNumberField(widthField, ConfigReader.requiredInt("INBOUND_WIDTH"));
        fillVisibleNumberField(heightField, ConfigReader.requiredInt("INBOUND_HEIGHT"));
        fillVisibleNumberField(weightField, ConfigReader.requiredInt("INBOUND_WEIGHT"));
    }

    private void completeCurrentInspectionIfPresent(String trackingCode, RmaInspectionItem item) {
        long startedAt = System.nanoTime();
        clickFirstActionIfPresent(150, "hang dat", "binh thuong", "normal");
        clickSubmitInspectionButton(trackingCode, item);
        String toast = waitForAnyToast(350);
        if (!toast.isBlank()) {
            System.out.println("RMA QC toast after tracking "
                    + trackingCode
                    + ": "
                    + toast
                    + ", qcMs="
                    + elapsedMillis(startedAt));
        }
    }

    private void clickSubmitInspectionButton(String trackingCode, RmaInspectionItem item) {
        WebElement button = findInspectionSubmitButton(1200);
        if (button == null) {
            throw new IllegalStateException("RMA inspection submit button was not found for tracking "
                    + trackingCode
                    + ". Screen="
                    + summarizeScreenText());
        }
        clickElement(button);
        waitAfterScan(80);
        boolean expectSerial = hasSerialCodes(item);
        boolean serialHandled = false;
        if (expectSerial && isSerialDialogVisible()) {
            scanSerialsInOpenDialog(trackingCode, item);
            serialHandled = true;
        } else {
            clickConfirmIfPresent(150);
            waitForInspectionSubmitResult(expectSerial ? 600 : 700);
            if (expectSerial && isSerialDialogVisible()) {
                scanSerialsInOpenDialog(trackingCode, item);
                serialHandled = true;
            }
        }
        if (serialHandled && isInspectionDetailFormVisible() && !isSerialDialogVisible()) {
            WebElement retryButton = findInspectionSubmitButton(1200);
            if (retryButton != null) {
                clickElement(retryButton);
                waitAfterScan(200);
                if (!isSerialDialogVisible()) {
                    clickConfirmIfPresent();
                }
                waitAfterScan(300);
            }
        }
        assertNoRequiredValidationAfterSubmit(trackingCode);
    }

    private boolean hasSerialCodes(RmaInspectionItem item) {
        return item != null && item.serialCodes() != null && !item.serialCodes().isEmpty();
    }

    private void waitForInspectionSubmitResult(long timeoutMillis) {
        try {
            shortWait(timeoutMillis).until(driver -> {
                if (isSerialDialogVisible() || isProductListVisibleFast() || !isInspectionDetailFormVisible()) {
                    return true;
                }
                return null;
            });
        } catch (RuntimeException ignored) {
        }
    }

    private void scanSerialsInOpenDialog(String trackingCode, RmaInspectionItem item) {
        WebElement dialog = findVisibleSerialDialog();
        if (dialog == null) {
            return;
        }
        int needed = serialScanTarget(dialog, item);
        List<String> serialCodes = serialCodesForInspection(item, needed);

        for (int index = 0; index < needed; index++) {
            dialog = findVisibleSerialDialog();
            if (dialog == null) {
                break;
            }
            WebElement serialInput = findSerialInputInDialog(dialog);
            if (serialInput == null) {
                throw new IllegalStateException("Serial input was not found in RMA serial modal for tracking "
                        + trackingCode
                        + ". Screen="
                        + summarizeScreenText());
            }
            String serialCode = serialCodes.get(index);
            setAnyEditableValue(serialInput, serialCode);
            if (!clickDialogActionIfPresent(dialog, "them")) {
                serialInput.sendKeys(Keys.ENTER);
            }
            waitAfterScan(150);
            System.out.println("Scanned RMA serial: tracking="
                    + trackingCode
                    + ", serial="
                    + serialCode
                    + ", index="
                    + (index + 1)
                    + "/"
                    + needed);
        }

        dialog = findVisibleSerialDialog();
        if (dialog != null) {
            clickDialogActionIfPresent(dialog, "xac nhan", "dong y", "ok");
            waitAfterScan(300);
        }
    }

    private List<String> serialCodesForInspection(RmaInspectionItem item, int needed) {
        List<String> serialCodes = new ArrayList<>();
        if (item != null && item.serialCodes() != null) {
            serialCodes.addAll(item.serialCodes());
        }
        if (serialCodes.size() < needed) {
            throw new IllegalStateException("Serial modal requires "
                    + needed
                    + " serial codes, but only "
                    + serialCodes.size()
                    + " status_id=202 serial codes were found for item="
                    + item
                    + ". Screen="
                    + summarizeScreenText());
        }
        return serialCodes;
    }

    private WebElement findInspectionSubmitButton(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                WebElement typedSubmit = findVisibleEnabledNow(submitInspectionButton);
                if (typedSubmit != null && !isSessionFinishButton(typedSubmit)) {
                    return typedSubmit;
                }
                for (WebElement element : driver.findElements(clickableControls)) {
                    if (!isClickableControl(element) || isSessionFinishButton(element)) {
                        continue;
                    }
                    String text = normalize(element.getText());
                    if (containsAny(text, "kiem hang", "qc", "cap nhat", "luu", "xac nhan")) {
                        return element;
                    }
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isSerialDialogVisible() {
        return findVisibleSerialDialog() != null;
    }

    private WebElement findVisibleSerialDialog() {
        for (WebElement dialog : driver.findElements(By.cssSelector(".modal.show, .ant-modal, [role='dialog']"))) {
            try {
                String text = normalize(dialog.getText());
                if (dialog.isDisplayed() && containsAny(text, "quet serial", "ma serial", "serial")) {
                    return dialog;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private int serialScanTarget(WebElement dialog, RmaInspectionItem item) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)").matcher(dialog.getText());
        if (matcher.find()) {
            return Math.max(1, Integer.parseInt(matcher.group(2)));
        }
        return item == null ? 1 : Math.max(1, item.quantity());
    }

    private WebElement findSerialInputInDialog(WebElement dialog) {
        WebElement fallback = null;
        for (WebElement input : dialog.findElements(By.cssSelector("input:not([type='hidden']), textarea"))) {
            if (!isUsableInput(input)) {
                continue;
            }
            String descriptor = inputDescriptor(input);
            if (descriptor.contains("serial")) {
                return input;
            }
            if (fallback == null) {
                fallback = input;
            }
        }
        return fallback;
    }

    private boolean clickDialogActionIfPresent(WebElement dialog, String... keywords) {
        for (WebElement element : dialog.findElements(By.cssSelector("button, [role='button'], a.btn"))) {
            if (!isClickableControl(element)) {
                continue;
            }
            String text = normalize(element.getText());
            if (text.isBlank() || isDangerousAction(text)) {
                continue;
            }
            if (containsAny(text, keywords)) {
                clickElement(element);
                return true;
            }
        }
        return false;
    }

    private boolean isSessionFinishButton(WebElement element) {
        String text = normalize(element.getText());
        return text.contains("hoan tat phien kiem");
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

    private void fillNumberField(By locator, String normalizedLabel, Number value) {
        if (value == null) {
            return;
        }
        if (fillTextField(locator, normalizedLabel, String.valueOf(value))) {
            return;
        }
        System.out.println("Numeric field not found for label: " + normalizedLabel);
    }

    private void fillVisibleNumberField(By locator, Number value) {
        if (value == null) {
            return;
        }
        WebElement input = findVisibleEnabledNow(locator);
        if (input != null) {
            setAnyEditableValue(input, String.valueOf(value));
        }
    }

    private boolean fillTextField(By locator, String normalizedLabel, String value) {
        WebElement input = findVisibleEnabledNow(locator);
        if (input != null) {
            setAnyEditableValue(input, value);
            return value.equals(inputValue(input));
        }
        return fillInputNearNormalizedLabel(normalizedLabel, value);
    }

    private boolean fillDateIfPresent(By primaryLocator, By fallbackLocator, String normalizedLabel, LocalDate date) {
        WebElement input = findVisibleEnabledNow(primaryLocator);
        if (input == null) {
            input = findVisibleEnabledNow(fallbackLocator);
        }
        if (input == null) {
            input = findInputNearNormalizedLabel(normalizedLabel);
        }
        if (input == null) {
            return false;
        }

        String displayDate = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        setFlatpickrDate(input, displayDate);
        if (!inputValue(input).isBlank()) {
            return true;
        }

        if ("date".equalsIgnoreCase(input.getDomAttribute("type"))) {
            setAnyEditableValue(input, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            return !inputValue(input).isBlank();
        }
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(displayDate, Keys.TAB);
        } catch (RuntimeException ignored) {
            setAnyEditableValue(input, displayDate);
        }
        if (!inputValue(input).isBlank()) {
            return true;
        }
        setAnyEditableValue(input, displayDate);
        return !inputValue(input).isBlank();
    }

    private WebElement findVisibleEnabledNow(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed() && element.isEnabled() && !Boolean.parseBoolean(element.getAttribute("readonly"))) {
                    return element;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private boolean fillInputNearNormalizedLabel(String normalizedLabel, String value) {
        WebElement input = findInputNearNormalizedLabel(normalizedLabel);
        if (input == null) {
            return false;
        }
        setAnyEditableValue(input, value);
        return value.equals(inputValue(input));
    }

    private WebElement findInputNearNormalizedLabel(String normalizedLabel) {
        Object element = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const target = arguments[0];"
                        + "const norm = text => (text || '')"
                        + "  .normalize('NFD').replace(/[\\u0300-\\u036f]/g, '')"
                        + "  .replace(/đ/g, 'd').replace(/Đ/g, 'D')"
                        + "  .toLowerCase().replace(/\\s+/g, ' ').trim();"
                        + "const visible = el => {"
                        + "  const style = window.getComputedStyle(el);"
                        + "  const rect = el.getBoundingClientRect();"
                        + "  return style.visibility !== 'hidden' && style.display !== 'none'"
                        + "    && rect.width > 0 && rect.height > 0;"
                        + "};"
                        + "const directText = el => Array.from(el.childNodes || [])"
                        + "  .filter(node => node.nodeType === Node.TEXT_NODE)"
                        + "  .map(node => node.textContent || '').join(' ');"
                        + "let labels = Array.from(document.querySelectorAll('label,div,span,p'))"
                        + "  .filter(visible)"
                        + "  .filter(el => {"
                        + "    const text = norm(directText(el));"
                        + "    return text === target || text.startsWith(target + ' ');"
                        + "  });"
                        + "if (!labels.length) {"
                        + "  labels = Array.from(document.querySelectorAll('label,div,span,p'))"
                        + "    .filter(visible)"
                        + "    .filter(el => {"
                        + "      const text = norm(el.innerText || el.textContent);"
                        + "      return text === target || text.startsWith(target + ' ');"
                        + "    })"
                        + "    .filter(el => !Array.from(el.children || [])"
                        + "      .some(child => norm(child.innerText || child.textContent).includes(target)));"
                        + "}"
                        + "const inputs = Array.from(document.querySelectorAll('input,textarea'))"
                        + "  .filter(visible)"
                        + "  .filter(el => !el.disabled && !el.readOnly);"
                        + "for (const label of labels) {"
                        + "  const lr = label.getBoundingClientRect();"
                        + "  const candidates = inputs.map(input => ({input, rect: input.getBoundingClientRect()}))"
                        + "    .filter(item => item.rect.top >= lr.bottom - 8 && item.rect.top <= lr.bottom + 130)"
                        + "    .filter(item => item.rect.right >= lr.left - 40 && item.rect.left <= lr.right + 180)"
                        + "    .sort((a, b) => Math.abs(a.rect.top - lr.bottom) + Math.abs(a.rect.left - lr.left)"
                        + "      - Math.abs(b.rect.top - lr.bottom) - Math.abs(b.rect.left - lr.left));"
                        + "  if (candidates.length) return candidates[0].input;"
                        + "}"
                        + "return null;",
                normalizedLabel);
        return element instanceof WebElement ? (WebElement) element : null;
    }

    private void setAnyEditableValue(WebElement element, String value) {
        try {
            element.click();
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            element.sendKeys(value, Keys.TAB);
            if (value.equals(inputValue(element))) {
                return;
            }
        } catch (RuntimeException ignored) {
        }

        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});"
                        + "arguments[0].focus();"
                        + "const value = arguments[1];"
                        + "const tag = (arguments[0].tagName || '').toLowerCase();"
                        + "const proto = tag === 'textarea' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;"
                        + "const setter = Object.getOwnPropertyDescriptor(proto, 'value').set;"
                        + "setter.call(arguments[0], '');"
                        + "for (const type of ['input', 'change']) {"
                        + "  arguments[0].dispatchEvent(new Event(type, {bubbles: true}));"
                        + "}"
                        + "setter.call(arguments[0], value);"
                        + "for (const type of ['input', 'change', 'blur']) {"
                        + "  arguments[0].dispatchEvent(new Event(type, {bubbles: true}));"
                        + "}",
                element,
                value);
        if (value.equals(inputValue(element))) {
            return;
        }
        try {
            element.click();
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            for (char character : value.toCharArray()) {
                element.sendKeys(String.valueOf(character));
            }
            element.sendKeys(Keys.TAB);
        } catch (RuntimeException ignored) {
        }
    }

    private void assertRequiredInspectionFieldsFilled(int expectedGoodQuantity, boolean expiryFieldVisible) {
        assertLocatorOrLabelValue(goodQtyField, "sl hang tot", String.valueOf(expectedGoodQuantity), true);
        if (expiryFieldVisible) {
            assertFieldValue("ngay san xuat", null, false);
        }
        assertFieldValue("ma batch/lot", null, false);
        assertFieldValue("ghi chu kiem hang", null, false);
    }

    private void assertLocatorOrLabelValue(By locator, String normalizedLabel, String expectedValue, boolean required) {
        WebElement input = findVisibleEnabledNow(locator);
        if (input != null) {
            assertInputValue(normalizedLabel, expectedValue, input);
            return;
        }
        assertFieldValue(normalizedLabel, expectedValue, required);
    }

    private void assertFieldValue(String normalizedLabel, String expectedValue, boolean required) {
        WebElement input = findInputNearNormalizedLabel(normalizedLabel);
        if (input == null) {
            if (required) {
                throw new IllegalStateException("Required RMA inspection field was not found: "
                        + normalizedLabel
                        + ". Screen="
                        + summarizeScreenText());
            }
            return;
        }
        assertInputValue(normalizedLabel, expectedValue, input);
    }

    private void assertInputValue(String normalizedLabel, String expectedValue, WebElement input) {
        String actualValue = inputValue(input);
        if (actualValue.isBlank()) {
            throw new IllegalStateException("RMA inspection field is still blank after fill: "
                    + normalizedLabel
                    + ". Screen="
                    + summarizeScreenText());
        }
        if (expectedValue != null && !expectedValue.equals(actualValue)) {
            throw new IllegalStateException("RMA inspection field has unexpected value: "
                    + normalizedLabel
                    + ". expected="
                    + expectedValue
                    + ", actual="
                    + actualValue
                    + ". Screen="
                    + summarizeScreenText());
        }
    }

    private void assertNoRequiredValidationAfterSubmit(String trackingCode) {
        String validation = visibleValidationText();
        if (!validation.isBlank()) {
            throw new IllegalStateException("RMA inspection still shows validation after submit for tracking "
                    + trackingCode
                    + ": "
                    + validation
                    + ". Screen="
                    + summarizeScreenText());
        }
    }

    private void assertNoOpenInspectionFormBeforeSessionFinish() {
        if (!isInspectionDetailFormVisible()) {
            return;
        }
        String validation = visibleValidationText();
        throw new IllegalStateException("Refusing to finish RMA inspection session while detail form is still open"
                + (validation.isBlank() ? "" : " with validation: " + validation)
                + ". Screen="
                + summarizeScreenText());
    }

    private String visibleValidationText() {
        String text = bodyText();
        String normalized = normalize(text);
        if (normalized.contains("vui long nhap")
                || normalized.contains("khong khop")
                || normalized.contains("bat buoc")
                || normalized.contains("minimum")
                || normalized.contains("shelf life")
                || (normalized.contains("serial")
                        && containsAny(normalized, "chua du", "quet them", "khong ton tai", "trung"))) {
            String compact = text.replaceAll("\\s+", " ").trim();
            return compact.length() <= 500 ? compact : compact.substring(0, 500);
        }
        return "";
    }

    private String inputValue(WebElement input) {
        try {
            String value = input.getDomProperty("value");
            return value == null ? "" : value.trim();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private int shelfLifeDays() {
        Integer fromWarning = shelfLifeDaysFromWarning();
        if (fromWarning != null) {
            return fromWarning;
        }

        String configured = ConfigReader.get("INBOUND_SHELF_LIFE_DAYS");
        if (configured != null && !configured.isBlank()) {
            return Integer.parseInt(configured.trim());
        }
        return 182;
    }

    private Integer shelfLifeDaysFromWarning() {
        Matcher matcher = Pattern.compile("h[eệ]\\s*th[oố]ng\\s*\\((\\d+)\\s*ng[aà]y\\)", Pattern.CASE_INSENSITIVE)
                .matcher(normalize(bodyText()));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private Integer minimumExpiryDays() {
        Matcher matcher = Pattern.compile("minimum\\s+(\\d+)\\s+ng\\S*y", Pattern.CASE_INSENSITIVE)
                .matcher(bodyText());
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
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

    private boolean isDangerousAction(String normalizedText) {
        return normalizedText.contains("huy")
                || normalizedText.contains("xoa")
                || normalizedText.contains("bo qua")
                || normalizedText.contains("dang xuat");
    }

    private boolean isClickableControl(WebElement element) {
        try {
            return element.isDisplayed() && element.isEnabled();
        } catch (RuntimeException e) {
            return false;
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

    private String firstScanCode(RmaInspectionItem item) {
        if (item.barcodes() != null && !item.barcodes().isEmpty()) {
            return item.barcodes().get(0);
        }
        if (item.goodsCode() != null && !item.goodsCode().isBlank()) {
            return item.goodsCode();
        }
        return item.partnerCode();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
            return driver.findElement(By.tagName("body")).getText();
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

    private record RmaUiProduct(String sku, int outboundQuantity, int checkedQuantity) {
    }
}

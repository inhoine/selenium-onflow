package com.example.seleniumtestng.pages.rma;

import com.example.seleniumtestng.models.RmaInspectionItem;
import com.example.seleniumtestng.models.RmaReturnOrder;
import com.example.seleniumtestng.pages.BasePage;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Handles the RMA product list table, row parsing, and product barcode/SKU scanning:
 * - Detecting visible product list views
 * - Parsing pending RMA products from table rows (outbound vs checked qty)
 * - Locating and clicking row actions or scanning product barcodes
 */
public class RmaProductTableSection extends BasePage {

    private final By productRows = By.cssSelector("tbody tr, [role='row']");
    private final By clickableControls = By.cssSelector("button, [role='button'], a.btn");

    public RmaProductTableSection(WebDriver driver) {
        super(driver);
    }

    public boolean isProductListVisible() {
        return isProductListVisibleFast()
                || containsAny(normalize(bodyText()), "san pham trong don hoan tra", "danh sach san pham trong kien");
    }

    public boolean isProductListVisibleFast() {
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

    public void openFirstProductFromList(String trackingCode) {
        WebElement action = findFirstProductRowAction(3000);
        if (action == null) {
            throw new IllegalStateException("No RMA product row/action was found after scanning tracking "
                    + trackingCode
                    + ". Screen="
                    + summarizeScreenText());
        }
        clickElement(action);
        System.out.println("Opened first RMA product from product list for tracking: " + trackingCode);
    }

    public RmaUiProduct firstUiProduct(RmaReturnOrder order) {
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

    public RmaUiProduct firstUiProductFast(RmaReturnOrder order) {
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
                    const rows = Array.from(document.querySelectorAll('tbody tr')).filter(visible);
                    for (const row of rows) {
                      const cells = Array.from(row.querySelectorAll('td')).filter(visible);
                      const rowText = norm(row.innerText || row.textContent);
                      if (cells.length < 3 || rowText.includes('stt') && rowText.includes('thong tin')) {
                        continue;
                      }

                      let outbound = 0;
                      let checked = 0;
                      for (const cell of cells) {
                        const ct = norm(cell.innerText || cell.textContent);
                        const mOut = ct.match(/(?:sl\\s*can\\s*kiem|sl\\s*xuat|so\\s*luong\\s*xuat|sl\\s*nhap|so\\s*luong\\s*nhap)\\s*[:=-]?\\s*(\\d+)/);
                        if (mOut && outbound === 0) outbound = parseInt(mOut[1], 10);
                        const mChk = ct.match(/(?:sl\\s*da\\s*kiem|da\\s*kiem|so\\s*luong\\s*da\\s*kiem)\\s*[:=-]?\\s*(\\d+)/);
                        if (mChk && checked === 0) checked = parseInt(mChk[1], 10);
                      }
                      if (outbound === 0) {
                        const numbers = rowText.match(/\\b\\d+\\b/g) || [];
                        if (numbers.length >= 2) {
                          outbound = parseInt(numbers[numbers.length - 2], 10);
                          checked = parseInt(numbers[numbers.length - 1], 10);
                        }
                      }
                      if (outbound <= checked) continue;

                      for (const group of apiGroups) {
                        for (const code of group) {
                          if (code && rowText.includes(norm(code))) {
                            return { sku: code, outbound, checked };
                          }
                        }
                      }
                      const skuMatch = rowText.match(/[a-z0-9]+(?:[-_][a-z0-9]+)+|[a-z]{2,}\\d{3,}/i);
                      if (skuMatch) {
                        return { sku: skuMatch[0], outbound, checked };
                      }
                    }
                    return null;
                    """,
                    apiCandidateGroups(order));

            if (result instanceof java.util.Map<?, ?> map) {
                String sku = String.valueOf(map.get("sku"));
                int outbound = ((Number) map.get("outbound")).intValue();
                int checked = ((Number) map.get("checked")).intValue();
                if (sku != null && !sku.isBlank() && outbound > checked) {
                    return new RmaUiProduct(sku, outbound, checked);
                }
            }
            return null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public int visiblePendingProductCount(RmaReturnOrder order) {
        int count = 0;
        for (WebElement row : driver.findElements(By.cssSelector("tbody tr"))) {
            if (!displayed(row) || isHeaderLikeRow(row)) {
                continue;
            }
            String sku = skuFromRow(row, order == null ? List.of() : order.items());
            int outboundQuantity = outboundQuantityFromRow(row);
            int checkedQuantity = checkedQuantityFromRow(row);
            if (sku != null && !sku.isBlank() && outboundQuantity > checkedQuantity) {
                count++;
            }
        }
        return count;
    }

    public int visiblePendingQuantityRowCount() {
        int count = 0;
        for (WebElement row : driver.findElements(By.cssSelector("tbody tr"))) {
            if (!displayed(row) || isHeaderLikeRow(row)) {
                continue;
            }
            int outboundQuantity = outboundQuantityFromRow(row);
            int checkedQuantity = checkedQuantityFromRow(row);
            if (outboundQuantity > checkedQuantity) {
                count++;
            }
        }
        return count;
    }

    public String skuFromRow(WebElement row, List<RmaInspectionItem> apiItems) {
        String rowText = row.getText();
        String apiScanCode = scanCodeFromApiItemsInRow(apiItems, rowText);
        if (apiScanCode != null) {
            return apiScanCode;
        }
        List<WebElement> cells = row.findElements(By.tagName("td"));
        if (cells.size() >= 3) {
            String productCellText = cells.get(2).getText();
            String candidateFromCell = firstSkuLikeToken(productCellText);
            if (candidateFromCell != null) {
                return candidateFromCell;
            }
            String specificLine = readMostSpecificProductLine(productCellText);
            if (specificLine != null) {
                return specificLine;
            }
        }
        return firstSkuLikeToken(rowText);
    }

    public int outboundQuantityFromRow(WebElement row) {
        List<String> cellTexts = visibleCellTexts(row);
        for (String text : cellTexts) {
            Matcher matcher = Pattern.compile("(?:sl\\s*c[aà]n\\s*ki[eê]m|sl\\s*xu[aá]t|s[oố]\\s*l[uư][oợ]ng\\s*xu[aá]t|sl\\s*nh[aậ]p|s[oố]\\s*l[uư][oợ]ng\\s*nh[aậ]p)\\s*[:=-]?\\s*(\\d+)",
                    Pattern.CASE_INSENSITIVE).matcher(text);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        if (cellTexts.size() >= 5) {
            Integer value = firstInteger(cellTexts.get(3));
            if (value != null) {
                return value;
            }
        }
        return 0;
    }

    public int checkedQuantityFromRow(WebElement row) {
        List<String> cellTexts = visibleCellTexts(row);
        for (String text : cellTexts) {
            Matcher matcher = Pattern.compile("(?:sl\\s*[đd][aã]\\s*ki[eê]m|[đd][aã]\\s*ki[eê]m|s[oố]\\s*l[uư][oợ]ng\\s*[đd][aã]\\s*ki[eê]m)\\s*[:=-]?\\s*(\\d+)",
                    Pattern.CASE_INSENSITIVE).matcher(text);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        if (cellTexts.size() >= 5) {
            Integer value = firstInteger(cellTexts.get(4));
            if (value != null) {
                return value;
            }
        }
        return 0;
    }

    public List<String> visibleCellTexts(WebElement row) {
        List<String> values = new ArrayList<>();
        for (WebElement cell : row.findElements(By.tagName("td"))) {
            if (displayed(cell)) {
                values.add(cell.getText().trim());
            }
        }
        return values;
    }

    public String productRowsSummary() {
        List<String> summaries = new ArrayList<>();
        for (WebElement row : driver.findElements(By.cssSelector("tbody tr"))) {
            if (displayed(row) && !isHeaderLikeRow(row)) {
                summaries.add(row.getText().replaceAll("\\s+", " ").trim());
            }
        }
        return summaries.toString();
    }

    public WebElement findProductScanInputFast(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                Object element = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "const norm = value => (value || '').normalize('NFD')"
                                + ".replace(/[\\u0300-\\u036f]/g, '').toLowerCase().replace(/\\s+/g, ' ').trim();"
                                + "const visible = el => {"
                                + "  const rect = el.getBoundingClientRect();"
                                + "  const style = window.getComputedStyle(el);"
                                + "  return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden'"
                                + "    && style.display !== 'none' && !el.disabled && !el.readOnly;"
                                + "};"
                                + "const inputs = Array.from(document.querySelectorAll("
                                + "  \"input:not([type='hidden']), textarea, [contenteditable='true']\"));"
                                + "const productInput = inputs.find(el => visible(el) && norm([el.placeholder,"
                                + "  el.getAttribute('aria-label'), el.name, el.id,"
                                + "  el.getAttribute('data-placeholder')].join(' ')).includes('san pham'));"
                                + "if (productInput) return productInput;"
                                + "const barcodeInput = inputs.find(el => visible(el) && norm([el.placeholder,"
                                + "  el.getAttribute('aria-label'), el.name, el.id,"
                                + "  el.getAttribute('data-placeholder')].join(' ')).includes('barcode'));"
                                + "return barcodeInput || null;");
                return element instanceof WebElement ? (WebElement) element : null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public void clickProductScanButtonIfPresent(long timeoutMillis) {
        if (!clickProductScanButtonFast()) {
            clickFirstActionIfPresent(timeoutMillis, "quet san pham", "quet sp", "quet ma san pham", "scan product");
        }
    }

    public boolean clickProductScanButtonFast() {
        try {
            Object clicked = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "const norm = value => (value || '').normalize('NFD')"
                            + ".replace(/[\\u0300-\\u036f]/g, '').toLowerCase().replace(/\\s+/g, ' ').trim();"
                            + "const visible = el => {"
                            + "  const rect = el.getBoundingClientRect();"
                            + "  const style = window.getComputedStyle(el);"
                            + "  return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden'"
                            + "    && style.display !== 'none' && !el.disabled;"
                            + "};"
                            + "const button = Array.from(document.querySelectorAll(\"button, [role='button'], a.btn\"))"
                            + "  .find(el => visible(el) && norm(el.innerText || el.textContent).includes('quet san pham'));"
                            + "if (button) { button.click(); return true; }"
                            + "return false;");
            return Boolean.TRUE.equals(clicked);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public WebElement findFirstProductRowAction(long timeoutMillis) {
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

    public void scanProductCode(WebElement input, String productCode) {
        clickProductScanButtonIfPresent(100);
        try {
            setScanInputValue(input, productCode);
            input.sendKeys(Keys.ENTER);
        } catch (RuntimeException e) {
            setInputValue(input, productCode);
            input.sendKeys(Keys.ENTER);
        }
    }

    private void setScanInputValue(WebElement element, String value) {
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        element.sendKeys(value);
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

    private boolean isDangerousAction(String normalizedText) {
        return normalizedText.contains("huy")
                || normalizedText.contains("xoa")
                || normalizedText.contains("bo qua")
                || normalizedText.contains("dang xuat");
    }

    private String firstSkuLikeToken(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?m)^\\s*([A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)+|[A-Za-z]{2,}\\d{3,})\\s*$").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        Matcher anyTokenMatcher = Pattern.compile("([A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)+|[A-Za-z]{2,}\\d{3,})").matcher(text);
        if (anyTokenMatcher.find()) {
            return anyTokenMatcher.group(1).trim();
        }
        return null;
    }

    private String readMostSpecificProductLine(String text) {
        if (text == null) {
            return null;
        }
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank() || normalize(line).contains("thong tin san pham")) {
                continue;
            }
            if (line.matches("^[A-Za-z0-9-_/]+$") && !line.matches("^\\d+$")) {
                return line;
            }
        }
        return null;
    }

    private String scanCodeFromApiItemsInRow(List<RmaInspectionItem> apiItems, String rowText) {
        if (apiItems == null || apiItems.isEmpty() || rowText == null || rowText.isBlank()) {
            return null;
        }
        String normalizedRow = normalize(rowText);
        for (RmaInspectionItem item : apiItems) {
            for (String candidate : scanCodeCandidates(item)) {
                if (normalizedRow.contains(normalize(candidate))) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private List<String> scanCodeCandidates(RmaInspectionItem item) {
        if (item == null) {
            return Collections.emptyList();
        }
        List<String> candidates = new ArrayList<>();
        if (item.barcodes() != null) {
            for (String barcode : item.barcodes()) {
                addIfNotBlank(candidates, barcode);
            }
        }
        addIfNotBlank(candidates, item.partnerCode());
        addIfNotBlank(candidates, item.goodsCode());
        return candidates;
    }

    private void addIfNotBlank(List<String> list, String value) {
        if (value != null && !value.isBlank() && isLikelyProductScanCode(value) && !list.contains(value.trim())) {
            list.add(value.trim());
        }
    }

    private boolean isLikelyProductScanCode(String value) {
        String normalized = normalize(value);
        if (normalized.length() < 3) {
            return false;
        }
        return !containsAny(normalized, "sl hang tot", "sl hang thieu", "hu hong", "san pham trong don", "stt");
    }

    private List<List<String>> apiCandidateGroups(RmaReturnOrder order) {
        if (order == null || order.items() == null || order.items().isEmpty()) {
            return Collections.emptyList();
        }
        List<List<String>> groups = new ArrayList<>();
        for (RmaInspectionItem item : order.items()) {
            List<String> group = scanCodeCandidates(item);
            if (!group.isEmpty()) {
                groups.add(group);
            }
        }
        return groups;
    }

    private Integer firstInteger(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d+)").matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private boolean clickFirstActionIfPresent(long timeoutMillis, String... keywords) {
        try {
            WebElement action = shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(clickableControls)) {
                    if (!element.isDisplayed() || !element.isEnabled()) {
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

    public record RmaUiProduct(String sku, int outboundQuantity, int checkedQuantity) {
    }
}


package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class PackingCampaignPage extends BasePage {
    private static final String PATH = "/packing-campaign";

    private final By pageBody = By.tagName("body");
    private final By editableInputs = By.cssSelector(
            "input:not([type='hidden']), textarea, [contenteditable='true']");
    private final By clickableControls = By.cssSelector("button, [role='button'], a.btn");
    private final By productRows = By.cssSelector("tbody tr, [role='row']");
    private final By tableHeaders = By.cssSelector("thead th, [role='columnheader']");
    private final By tableCells = By.cssSelector("td, [role='cell']");
    private final By toastMessage = By.cssSelector(
            ".Toastify__toast-body, .ant-message-notice-content, .ant-notification-notice-message, "
                    + ".ant-notification-notice-description");

    public PackingCampaignPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(ConfigReader.required("WMS_BASE_URL") + PATH);
        waitForPage();
    }

    public void waitForPage() {
        wait.withTimeout(Duration.ofSeconds(30))
                .until(ExpectedConditions.urlContains(PATH));
        visible(pageBody);
        wait.until(driver -> !String.valueOf(driver.getCurrentUrl()).contains("/login"));
    }

    public void openCampaignForPacking(String tableCode, String pickupCode) {
        open();
        scanTableIfPresent(tableCode);
        scanCampaignPickup(pickupCode);
        waitForCampaignContext(pickupCode);
    }

    public boolean isAtPage() {
        return String.valueOf(driver.getCurrentUrl()).contains(PATH);
    }

    public boolean isCampaignContextOpen(String pickupCode) {
        return isCampaignContextOpenNow(pickupCode);
    }

    public boolean isPackingMaterialScannerOpen() {
        return findInputByPlaceholderNow("vat lieu dong goi", "nvl") != null;
    }

    public boolean isOrderConfirmationListOpen() {
        return isOrderConfirmationListOpenNow();
    }

    public void scanPackingMaterial(String materialCode) {
        WebElement input = wait.until(driver -> findInputByPlaceholderNow("vat lieu dong goi", "nvl"));
        scanIntoInput(input, materialCode);
        System.out.println("Scanned packing campaign material code: " + materialCode);
        waitForMaterialScanFeedback(materialCode);
    }

    public void scanPackingMaterialIfPresent(String materialCode) {
        WebElement input = findInputByPlaceholder(5000, "vat lieu dong goi", "nvl");
        if (input == null) {
            if (isOrderConfirmationListOpenNow()) {
                System.out.println("Skip packing material scan: order confirmation list is already open");
                return;
            }
            throw new IllegalStateException("Packing campaign material scanner was not found, and order list is not open."
                    + " Screen="
                    + summarizeScreenText());
        }

        scanIntoInput(input, materialCode);
        System.out.println("Scanned packing campaign material code: " + materialCode);
        waitForMaterialScanFeedback(materialCode);
    }

    public List<String> waitForAllAwbBillsPrintedAndScanOrders() {
        long timeoutMillis = Long.parseLong(ConfigReader.getOrDefault(
                "PACKING_CAMPAIGN_AWB_PRINT_TIMEOUT_MS",
                "600000"));
        long deadline = System.nanoTime() + Duration.ofMillis(timeoutMillis).toNanos();
        Set<String> scannedOrderCodes = new LinkedHashSet<>();
        scrollOrderListToTop();

        while (System.nanoTime() < deadline) {
            String orderCode = firstVisiblePrintedAwbOrderCodeNow(scannedOrderCodes);
            if (orderCode != null) {
                scanOrderConfirmationCode(orderCode);
                scannedOrderCodes.add(orderCode);
                continue;
            }

            if (hasVisibleUnscannedPrintingAwbRows(scannedOrderCodes)) {
                sleepAfterOrderScan(500);
                continue;
            }

            if (scrollOrderListDown()) {
                sleepAfterOrderScan(300);
                continue;
            }

            if (!scannedOrderCodes.isEmpty()) {
                List<String> scanned = new ArrayList<>(scannedOrderCodes);
                System.out.println("Scanned all visible packing campaign orders with printed AWB bills. count="
                        + scanned.size()
                        + ", orders="
                        + String.join(",", scanned));
                return scanned;
            }

            sleepAfterOrderScan(500);
        }

        throw new IllegalStateException("Not all packing campaign orders reached AWB bill status 'Da in' within "
                + timeoutMillis
                + "ms. Scanned="
                + String.join(",", scannedOrderCodes)
                + ". AWB statuses="
                + awbStatusSummary()
                + ". Screen="
                + summarizeScreenText());
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

    private void scanTableIfPresent(String tableCode) {
        WebElement tableInput = findInputByPlaceholder(3000, "ma tram", "tram");
        if (tableInput == null) {
            System.out.println("Packing campaign table scan is not required or table is already selected");
            return;
        }

        scanIntoInput(tableInput, tableCode);
        System.out.println("Scanned packing campaign table code: " + tableCode);
        try {
            shortWait(8000).until(driver -> {
                WebElement campaignInput = findInputByPlaceholderNow("bang ke", "campaign", "pickup", "xe", "ro");
                if (campaignInput != null && !isTableInput(campaignInput)) {
                    return true;
                }
                WebElement currentTableInput = findInputByPlaceholderNow("ma tram", "tram");
                return currentTableInput == null ? true : null;
            });
        } catch (RuntimeException ignored) {
            System.out.println("Continue after table scan; campaign input did not stabilize immediately");
        }
    }

    private void scanCampaignPickup(String pickupCode) {
        clickStartScanButtonIfPresent();
        WebElement input = wait.until(driver -> {
            WebElement specific = findInputByPlaceholderNow("bang ke", "campaign", "pickup");
            if (specific != null && !isTableInput(specific)) {
                return specific;
            }

            WebElement mixedScanner = findInputByPlaceholderNow("xe", "ro");
            if (mixedScanner != null && !isTableInput(mixedScanner)) {
                return mixedScanner;
            }

            for (WebElement element : driver.findElements(editableInputs)) {
                if (isUsableInput(element) && !isTableInput(element)) {
                    return element;
                }
            }
            WebElement active = activeEditableElement();
            return active != null && !isTableInput(active) ? active : null;
        });

        scanIntoInput(input, pickupCode);
        System.out.println("Scanned packing campaign pickup code: " + pickupCode);
        clickSubmitScanButtonIfStillOnSearch(input, pickupCode);
    }

    private void waitForCampaignContext(String pickupCode) {
        try {
            shortWait(15000).until(driver -> isCampaignContextOpenNow(pickupCode) ? true : null);
        } catch (RuntimeException e) {
            String toast = waitForAnyToast(1000);
            throw new IllegalStateException("Packing campaign context did not open after scanning pickup "
                    + pickupCode
                    + ". Toast="
                    + toast
                    + ". Screen="
                    + summarizeScreenText(), e);
        }
    }

    private void waitForMaterialScanFeedback(String materialCode) {
        try {
            shortWait(10000).until(driver -> {
                String toast = waitForAnyToast(200);
                if (!toast.isBlank()) {
                    return true;
                }
                if (findInputByPlaceholderNow("vat lieu dong goi", "nvl") == null) {
                    return true;
                }
                String body = normalize(driver.findElement(By.tagName("body")).getText());
                return body.contains(normalize(materialCode)) ? true : null;
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException("Packing campaign material scan feedback was not detected for "
                    + materialCode
                    + ". Screen="
                    + summarizeScreenText(), e);
        }
    }

    private String firstVisiblePrintedAwbOrderCodeNow(Set<String> scannedOrderCodes) {
        int orderColumnIndex = headerIndex("don hang", 1);
        int awbColumnIndex = headerIndex("awb", 3);
        for (WebElement row : driver.findElements(productRows)) {
            if (!displayed(row)) {
                continue;
            }
            List<WebElement> cells = row.findElements(tableCells);
            int requiredCellCount = Math.max(orderColumnIndex, awbColumnIndex) + 1;
            if (cells.size() < requiredCellCount) {
                continue;
            }
            String awbStatus = normalize(cells.get(awbColumnIndex).getText());
            if (!awbStatus.contains("da in") || awbStatus.contains("dang in")) {
                continue;
            }
            String orderCode = extractOrderCode(cells.get(orderColumnIndex).getText());
            if (!orderCode.isBlank() && !scannedOrderCodes.contains(orderCode)) {
                return orderCode;
            }
        }
        return null;
    }

    private void scanOrderConfirmationCode(String orderCode) {
        WebElement input = wait.until(driver -> findInputByPlaceholderNow("ma don hang"));
        scanIntoInput(input, orderCode);
        System.out.println("Scanned packing campaign order after AWB bill was printed: " + orderCode);
        waitForOrderConfirmationToast(orderCode);
        waitForOrderScanInputReady();
    }

    private void waitForOrderConfirmationToast(String orderCode) {
        long timeoutMillis = Long.parseLong(ConfigReader.getOrDefault(
                "PACKING_CAMPAIGN_ORDER_CONFIRM_TOAST_TIMEOUT_MS",
                "10000"));
        try {
            shortWait(timeoutMillis).until(driver -> {
                String normalizedOrderCode = normalize(orderCode);
                for (WebElement toast : driver.findElements(toastMessage)) {
                    if (!displayed(toast)) {
                        continue;
                    }
                    String text = normalize(toast.getText());
                    if (text.contains(normalizedOrderCode) && text.contains("thanh cong")) {
                        return true;
                    }
                }
                return null;
            });
            System.out.println("Confirmed packing campaign order toast: " + orderCode);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Packing campaign success toast was not shown after scanning order "
                    + orderCode
                    + ". Screen="
                    + summarizeScreenText(), e);
        }
    }

    private void waitForOrderScanInputReady() {
        try {
            shortWait(5000).until(driver -> findInputByPlaceholderNow("ma don hang") != null ? true : null);
        } catch (RuntimeException e) {
            System.out.println("Order scan input was not ready immediately after success toast; continuing with main wait");
        }
    }

    private int headerIndex(String keyword, int fallbackIndex) {
        List<WebElement> headers = driver.findElements(tableHeaders);
        for (int index = 0; index < headers.size(); index++) {
            WebElement header = headers.get(index);
            if (displayed(header) && normalize(header.getText()).contains(keyword)) {
                return index;
            }
        }
        return fallbackIndex;
    }

    private String extractOrderCode(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = Pattern.compile("[A-Z]{2,}[A-Z0-9-]*\\d[A-Z0-9-]*")
                .matcher(text.replaceAll("\\s+", " ").trim());
        return matcher.find() ? matcher.group() : "";
    }

    private String awbStatusSummary() {
        int orderColumnIndex = headerIndex("don hang", 1);
        int awbColumnIndex = headerIndex("awb", 3);
        StringBuilder builder = new StringBuilder();
        int rowCount = 0;
        for (WebElement row : driver.findElements(productRows)) {
            if (!displayed(row)) {
                continue;
            }
            List<WebElement> cells = row.findElements(tableCells);
            int requiredCellCount = Math.max(orderColumnIndex, awbColumnIndex) + 1;
            if (cells.size() < requiredCellCount) {
                continue;
            }
            if (rowCount > 0) {
                builder.append("; ");
            }
            builder.append(extractOrderCode(cells.get(orderColumnIndex).getText()))
                    .append("=")
                    .append(cells.get(awbColumnIndex).getText().replaceAll("\\s+", " ").trim());
            rowCount++;
            if (rowCount >= 8) {
                break;
            }
        }
        return builder.toString();
    }

    private boolean isOrderConfirmationListOpenNow() {
        return findInputByPlaceholderNow("ma don hang") != null && hasAwbBillRowsNow();
    }

    private boolean hasAwbBillRowsNow() {
        int orderColumnIndex = headerIndex("don hang", 1);
        int awbColumnIndex = headerIndex("awb", 3);
        for (WebElement row : driver.findElements(productRows)) {
            if (!displayed(row)) {
                continue;
            }
            List<WebElement> cells = row.findElements(tableCells);
            int requiredCellCount = Math.max(orderColumnIndex, awbColumnIndex) + 1;
            if (cells.size() < requiredCellCount) {
                continue;
            }
            String orderCode = extractOrderCode(cells.get(orderColumnIndex).getText());
            String awbStatus = normalize(cells.get(awbColumnIndex).getText());
            if (!orderCode.isBlank() && (awbStatus.contains("dang in") || awbStatus.contains("da in"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasVisibleUnscannedPrintingAwbRows(Set<String> scannedOrderCodes) {
        int orderColumnIndex = headerIndex("don hang", 1);
        int awbColumnIndex = headerIndex("awb", 3);
        for (WebElement row : driver.findElements(productRows)) {
            if (!displayed(row)) {
                continue;
            }
            List<WebElement> cells = row.findElements(tableCells);
            int requiredCellCount = Math.max(orderColumnIndex, awbColumnIndex) + 1;
            if (cells.size() < requiredCellCount) {
                continue;
            }
            String orderCode = extractOrderCode(cells.get(orderColumnIndex).getText());
            if (orderCode.isBlank() || scannedOrderCodes.contains(orderCode)) {
                continue;
            }
            String awbStatus = normalize(cells.get(awbColumnIndex).getText());
            if (awbStatus.contains("dang in")) {
                return true;
            }
        }
        return false;
    }

    private void scrollOrderListToTop() {
        ((JavascriptExecutor) driver).executeScript(
                "const visible = el => { const r = el.getBoundingClientRect();"
                        + " return r.width > 0 && r.height > 0; };"
                        + "const rows = Array.from(document.querySelectorAll('tbody tr,[role=\"row\"]'))"
                        + " .filter(visible);"
                        + "if (!rows.length) return false;"
                        + "let node = rows[0].parentElement;"
                        + "while (node && node !== document.body) {"
                        + "  if (node.scrollHeight > node.clientHeight + 5) { node.scrollTop = 0; return true; }"
                        + "  node = node.parentElement;"
                        + "}"
                        + "return false;");
    }

    private boolean scrollOrderListDown() {
        Object result = ((JavascriptExecutor) driver).executeScript(
                "const visible = el => { const r = el.getBoundingClientRect();"
                        + " return r.width > 0 && r.height > 0; };"
                        + "const rows = Array.from(document.querySelectorAll('tbody tr,[role=\"row\"]'))"
                        + " .filter(visible);"
                        + "if (!rows.length) return false;"
                        + "let node = rows[0].parentElement;"
                        + "while (node && node !== document.body) {"
                        + "  if (node.scrollHeight > node.clientHeight + 5) {"
                        + "    const before = node.scrollTop;"
                        + "    node.scrollTop = Math.min(node.scrollTop + Math.max(120, node.clientHeight * 0.8),"
                        + "      node.scrollHeight - node.clientHeight);"
                        + "    return node.scrollTop > before + 1;"
                        + "  }"
                        + "  node = node.parentElement;"
                        + "}"
                        + "return false;");
        return Boolean.TRUE.equals(result);
    }

    private void sleepAfterOrderScan(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for packing campaign order list", e);
        }
    }

    private boolean isCampaignContextOpenNow(String pickupCode) {
        if (!isAtPage()) {
            return false;
        }

        WebElement productScanner = findInputByPlaceholderNow("san pham", "sku", "barcode", "ma vach");
        if (productScanner != null) {
            return true;
        }

        WebElement packingMaterialScanner = findInputByPlaceholderNow("vat lieu dong goi", "nvl");
        if (packingMaterialScanner != null) {
            return true;
        }

        for (WebElement row : driver.findElements(productRows)) {
            if (displayed(row) && !row.getText().trim().isBlank()) {
                return true;
            }
        }

        String normalizedBody = normalize(driver.findElement(By.tagName("body")).getText());
        String normalizedPickup = normalize(pickupCode);
        return normalizedBody.contains(normalizedPickup)
                && (normalizedBody.contains("dong goi")
                || normalizedBody.contains("campaign")
                || normalizedBody.contains("san pham")
                || normalizedBody.contains("don hang")
                || normalizedBody.contains("vat lieu dong goi")
                || normalizedBody.contains("bang ke"));
    }

    private WebElement findInputByPlaceholder(long timeoutMillis, String... keywords) {
        try {
            return shortWait(timeoutMillis).until(driver -> findInputByPlaceholderNow(keywords));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private WebElement findInputByPlaceholderNow(String... keywords) {
        for (WebElement element : driver.findElements(editableInputs)) {
            if (!isUsableInput(element)) {
                continue;
            }
            String placeholder = normalize(element.getAttribute("placeholder"));
            String ariaLabel = normalize(element.getAttribute("aria-label"));
            String name = normalize(element.getAttribute("name"));
            String dataPlaceholder = normalize(element.getAttribute("data-placeholder"));
            String combined = placeholder + " " + ariaLabel + " " + name + " " + dataPlaceholder;
            if (containsAny(combined, keywords)) {
                return element;
            }
        }
        WebElement active = activeEditableElement();
        if (active != null) {
            String placeholder = normalize(active.getAttribute("placeholder"));
            String ariaLabel = normalize(active.getAttribute("aria-label"));
            String dataPlaceholder = normalize(active.getAttribute("data-placeholder"));
            String combined = placeholder + " " + ariaLabel + " " + dataPlaceholder;
            if (containsAny(combined, keywords)) {
                return active;
            }
        }
        return null;
    }

    private boolean isTableInput(WebElement element) {
        String placeholder = normalize(element.getAttribute("placeholder"));
        String ariaLabel = normalize(element.getAttribute("aria-label"));
        String combined = placeholder + " " + ariaLabel;
        return !combined.contains("bang ke")
                && (combined.contains("ma ban") || combined.matches(".*\\bban\\b.*"));
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

    private WebElement activeEditableElement() {
        try {
            Object active = ((JavascriptExecutor) driver).executeScript("return document.activeElement;");
            if (active instanceof WebElement && isUsableInput((WebElement) active)) {
                return (WebElement) active;
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void clickStartScanButtonIfPresent() {
        WebElement button = findControlByText(3000, "quet ma", "bang ke moi");
        if (button == null) {
            button = findControlByText(1000, "quet ma", "xe");
        }
        if (button == null) {
            System.out.println("Packing campaign new scan button not found; fallback to visible scan/search input");
            return;
        }
        clickElement(button);
        System.out.println("Opened packing campaign scan form");
    }

    private void clickSubmitScanButtonIfStillOnSearch(WebElement scannedInput, String pickupCode) {
        try {
            shortWait(1500).until(driver -> isCampaignContextOpenNow(pickupCode) ? true : null);
            return;
        } catch (RuntimeException ignored) {
        }

        String placeholder = normalize(scannedInput.getAttribute("placeholder"));
        if (!placeholder.contains("bang ke")) {
            return;
        }

        WebElement button = findControlByText(1000, "tim kiem");
        if (button == null) {
            button = findControlByText(1000, "xac nhan");
        }
        if (button != null) {
            clickElement(button);
            System.out.println("Clicked packing campaign scan/search submit button");
        }
    }

    private WebElement findControlByText(long timeoutMillis, String... keywords) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(clickableControls)) {
                    if (!isClickableControl(element)) {
                        continue;
                    }
                    String text = normalize(element.getText());
                    if (containsAll(text, keywords) && !text.contains("lam moi")) {
                        return element;
                    }
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isClickableControl(WebElement element) {
        try {
            return element.isDisplayed() && element.isEnabled();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean containsAll(String value, String... keywords) {
        for (String keyword : keywords) {
            if (!value.contains(keyword)) {
                return false;
            }
        }
        return true;
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

    private void scanIntoInput(WebElement input, String code) {
        try {
            clearAndEnter(input, code);
        } catch (ElementClickInterceptedException e) {
            setInputValue(input, code);
            input.sendKeys(Keys.ENTER);
        }
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

    private String summarizeScreenText() {
        String text = driver.findElement(By.tagName("body")).getText().replaceAll("\\s+", " ").trim();
        return text.length() <= 600 ? text : text.substring(0, 600);
    }
}

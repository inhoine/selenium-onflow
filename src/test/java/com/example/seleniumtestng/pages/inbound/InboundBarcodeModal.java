package com.example.seleniumtestng.pages.inbound;

import com.example.seleniumtestng.pages.BasePage;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Handles manufacturer barcode modal interactions and barcode input during WMS Inbound Inspection:
 * - Opening and reading manufacturer barcodes from the product modal
 * - Validating barcode formats
 * - Inputting custom or retrieved barcodes into the inspection form
 */
public class InboundBarcodeModal extends BasePage {

    private final By barcodeField = By.xpath(
            "//input[@name='manufacturer_barcode' or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'barcode')]");
    private final By viewBarcodeBtn = By.xpath(
            "//input[@name='manufacturer_barcode' or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'barcode')]"
                    + "/following-sibling::*[contains(@class,'input-group-text')]//*[contains(normalize-space(.),'Xem') or contains(@class,'cursor-pointer')][1]");
    private final By productBarcodeModal = By
            .xpath("//*[(@role='dialog' or contains(@class,'modal-content') or contains(@class,'modal-dialog'))"
                    + " and .//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'barcode')]]");
    private final By productBarcodeRows = By
            .xpath("//*[(@role='dialog' or contains(@class,'modal-content') or contains(@class,'modal-dialog'))"
                    + " and .//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'barcode')]]"
                    + "//*[self::tr or self::td or self::div or self::span][normalize-space()]");

    public InboundBarcodeModal(WebDriver driver) {
        super(driver);
    }

    public By barcodeField() {
        return barcodeField;
    }

    public boolean isBarcodeFieldVisible() {
        return firstVisibleEnabled(barcodeField) != null;
    }

    public void inputBarcode() {
        type(barcodeField, "AUTO" + System.nanoTime());
    }

    public void inputBarcode(String barcode) {
        setEditableInputValue(barcodeField, barcode);
    }

    public void inputBarcodeFromProductModal() {
        inputBarcode(readBarcodeFromProductModal());
    }

    public String readBarcodeFromProductModal() {
        waitForBarcodeFieldVisible();
        return firstBarcodeFromProductModal();
    }

    public String firstBarcodeFromProductModal() {
        if (!openProductBarcodeModal()) {
            throw new IllegalStateException("Clicked barcode view button but barcode modal did not open. Buttons="
                    + visibleButtonSummary());
        }

        String barcode = waitForBarcodeFromOpenModal();
        closeProductBarcodeModal();
        if (barcode == null || barcode.isBlank()) {
            throw new IllegalStateException("Product barcode modal did not contain a usable barcode");
        }
        System.out.println("Using product barcode from WMS modal: " + barcode);
        return barcode;
    }

    public boolean openProductBarcodeModal() {
        for (WebElement button : all(viewBarcodeBtn)) {
            if (!isDisplayed(button) || !button.isEnabled()) {
                continue;
            }
            if (clickBarcodeViewElementAndWait(button)) {
                return true;
            }
            WebElement parent = parentElement(button);
            if (parent != null && clickBarcodeViewElementAndWait(parent)) {
                return true;
            }
        }
        if (clickNearestBarcodeViewButton()) {
            return waitUntilProductBarcodeModalVisible();
        }
        return false;
    }

    private boolean clickBarcodeViewElementAndWait(WebElement element) {
        try {
            new org.openqa.selenium.interactions.Actions(driver)
                    .moveToElement(element)
                    .click()
                    .perform();
        } catch (RuntimeException e) {
            try {
                element.click();
            } catch (RuntimeException ignored) {
                try {
                    jsClick(element);
                } catch (RuntimeException ignoredToo) {
                    return false;
                }
            }
        }
        return waitUntilProductBarcodeModalVisible();
    }

    private WebElement parentElement(WebElement element) {
        try {
            Object parent = ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("return arguments[0].parentElement;", element);
            return parent instanceof WebElement ? (WebElement) parent : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String waitForBarcodeFromOpenModal() {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            String fromRows = firstBarcodeFromRows();
            if (fromRows != null) {
                return fromRows;
            }
            String fromModal = firstBarcodeInText(visibleText(productBarcodeModal));
            if (fromModal != null) {
                return fromModal;
            }
            String fromDom = firstBarcodeInText(visibleDomText(productBarcodeModal));
            if (fromDom != null) {
                return fromDom;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        String modalText = compactForLog(visibleText(productBarcodeModal) + " " + visibleDomText(productBarcodeModal));
        throw new IllegalStateException("Product barcode modal is open but no barcode was parsed. Modal=" + modalText);
    }

    public void waitForBarcodeFieldVisible() {
        shortWait(5000).until(driver -> firstVisibleEnabled(barcodeField) != null ? true : null);
    }

    private boolean clickNearestBarcodeViewButton() {
        WebElement input = firstVisibleEnabled(barcodeField);
        if (input == null) {
            return false;
        }
        Object clicked = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const input = arguments[0];"
                        + "const norm = value => (value || '')"
                        + "  .normalize('NFD').replace(/[\\u0300-\\u036f]/g, '')"
                        + "  .replace(/đ/g, 'd').replace(/Đ/g, 'D')"
                        + "  .toLowerCase().replace(/\\s+/g, ' ').trim();"
                        + "const visible = el => {"
                        + "  if (!el) return false;"
                        + "  const style = window.getComputedStyle(el);"
                        + "  const rect = el.getBoundingClientRect();"
                        + "  return style.visibility !== 'hidden' && style.display !== 'none'"
                        + "    && rect.width > 0 && rect.height > 0 && !el.disabled;"
                        + "};"
                        + "const group = input.closest('.input-group') || input.parentElement;"
                        + "const candidates = Array.from(group.querySelectorAll('.input-group-text span, .input-group-text, [class*=\"cursor\"]'))"
                        + "  .filter(visible)"
                        + "  .filter(el => norm(el.innerText || el.textContent || el.getAttribute('aria-label') || el.getAttribute('title')).includes('xem')"
                        + "    || (el.getAttribute('class') || '').includes('cursor-pointer')"
                        + "    || !!el.querySelector('i[class*=\"eye\"]'));"
                        + "candidates.sort((a, b) => (norm(b.innerText || b.textContent).includes('xem') ? 1 : 0)"
                        + "  - (norm(a.innerText || a.textContent).includes('xem') ? 1 : 0));"
                        + "if (!candidates.length) return false;"
                        + "const button = candidates[0];"
                        + "button.scrollIntoView({block:'center', inline:'center'});"
                        + "for (const type of ['pointerdown','mousedown','pointerup','mouseup','click']) {"
                        + "  button.dispatchEvent(new MouseEvent(type, {bubbles: true, cancelable: true, view: window}));"
                        + "}"
                        + "return true;",
                input);
        return Boolean.TRUE.equals(clicked);
    }

    public boolean waitUntilProductBarcodeModalVisible() {
        try {
            shortWait(5000).until(driver -> isProductBarcodeModalActuallyVisible() ? true : null);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean isProductBarcodeModalActuallyVisible() {
        if (firstVisibleEnabled(productBarcodeModal) != null) {
            return true;
        }
        Object visible = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const norm = value => (value || '')"
                        + "  .normalize('NFD').replace(/[\\u0300-\\u036f]/g, '')"
                        + "  .replace(/đ/g, 'd').replace(/Đ/g, 'D')"
                        + "  .toLowerCase().replace(/\\s+/g, ' ').trim();"
                        + "const shown = el => {"
                        + "  const style = window.getComputedStyle(el);"
                        + "  const rect = el.getBoundingClientRect();"
                        + "  return style.visibility !== 'hidden' && style.display !== 'none'"
                        + "    && rect.width > 0 && rect.height > 0;"
                        + "};"
                        + "return Array.from(document.querySelectorAll('[role=\"dialog\"], .modal-content, .modal-dialog, .modal'))"
                        + "  .some(el => shown(el) && norm(el.innerText || el.textContent).includes('danh sach barcode'));");
        return Boolean.TRUE.equals(visible);
    }

    private String firstBarcodeFromRows() {
        for (WebElement row : all(productBarcodeRows)) {
            if (!isDisplayed(row)) {
                continue;
            }
            String barcode = firstBarcodeInText(safeText(row));
            if (barcode != null) {
                return barcode;
            }
        }
        return null;
    }

    private String firstBarcodeInText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\b(?:\\d[\\s\\-]*){6,}\\b").matcher(text);
        if (matcher.find()) {
            String barcode = matcher.group().replaceAll("\\D+", "");
            return barcode.length() >= 6 ? barcode : null;
        }

        Matcher alphaNumeric = Pattern.compile("\\b[A-Za-z0-9][A-Za-z0-9_-]{3,}\\b").matcher(text);
        while (alphaNumeric.find()) {
            String candidate = alphaNumeric.group();
            if (!isBarcodeHeaderToken(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isBarcodeHeaderToken(String value) {
        String token = normalized(value);
        return token.equals("barcode")
                || token.equals("danh")
                || token.equals("sach")
                || token.equals("theo")
                || token.equals("san")
                || token.equals("pham")
                || token.equals("ma")
                || token.equals("khong")
                || token.equals("ket")
                || token.equals("qua")
                || token.equals("phu")
                || token.equals("hop")
                || token.equals("truy")
                || token.equals("van")
                || token.equals("cua")
                || token.equals("ban");
    }

    private String visibleDomText(By locator) {
        String fromBarcodeModal = barcodeModalTextFromDom();
        if (!fromBarcodeModal.isBlank()) {
            return fromBarcodeModal;
        }
        for (WebElement element : all(locator)) {
            if (!isDisplayed(element)) {
                continue;
            }
            Object value = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "return [arguments[0].innerText, arguments[0].textContent, arguments[0].outerHTML]"
                            + ".filter(Boolean).join(' ');",
                    element);
            return value == null ? "" : value.toString();
        }
        return "";
    }

    private String barcodeModalTextFromDom() {
        Object value = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const norm = value => (value || '')"
                        + "  .normalize('NFD').replace(/[\\u0300-\\u036f]/g, '')"
                        + "  .replace(/đ/g, 'd').replace(/Đ/g, 'D')"
                        + "  .toLowerCase().replace(/\\s+/g, ' ').trim();"
                        + "const shown = el => {"
                        + "  const style = window.getComputedStyle(el);"
                        + "  const rect = el.getBoundingClientRect();"
                        + "  return style.visibility !== 'hidden' && style.display !== 'none'"
                        + "    && rect.width > 0 && rect.height > 0;"
                        + "};"
                        + "const modal = Array.from(document.querySelectorAll('[role=\"dialog\"], .modal-content, .modal-dialog, .modal'))"
                        + "  .filter(shown)"
                        + "  .find(el => norm(el.innerText || el.textContent).includes('danh sach barcode'));"
                        + "return modal ? [modal.innerText, modal.textContent, modal.outerHTML].filter(Boolean).join(' ') : '';");
        return value == null ? "" : value.toString();
    }

    public void closeProductBarcodeModal() {
        try {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
            shortWait(2000).until(ExpectedConditions.invisibilityOfElementLocated(productBarcodeModal));
            return;
        } catch (RuntimeException ignored) {
        }
        for (WebElement button : all(By.cssSelector(".modal button"))) {
            if (isDisplayed(button) && button.isEnabled()) {
                try {
                    jsClick(button);
                    shortWait(2000).until(ExpectedConditions.invisibilityOfElementLocated(productBarcodeModal));
                    return;
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    private void setEditableInputValue(By locator, String value) {
        WebElement element = firstEditable(locator);
        if (element != null) {
            setInputValue(element, value);
        }
    }

    private WebElement firstEditable(By locator) {
        for (WebElement element : all(locator)) {
            if (isEditable(element)) {
                return element;
            }
        }
        return null;
    }

    private WebElement firstVisibleEnabled(By locator) {
        for (WebElement element : all(locator)) {
            if (isDisplayed(element) && element.isEnabled()) {
                return element;
            }
        }
        return null;
    }

    private boolean isEditable(WebElement input) {
        try {
            return input.isDisplayed() && input.isEnabled() && input.getDomAttribute("readonly") == null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String safeText(WebElement element) {
        try {
            return element.getText();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String visibleText(By locator) {
        for (WebElement element : all(locator)) {
            if (isDisplayed(element)) {
                return element.getText();
            }
        }
        return "";
    }

    private String normalized(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .toLowerCase();
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private String compactForLog(String value) {
        if (value == null) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 1000 ? compact : compact.substring(0, 1000) + "...";
    }

    private String visibleButtonSummary() {
        List<String> summaries = new ArrayList<>();
        for (WebElement button : all(By.cssSelector("button"))) {
            if (!isDisplayed(button)) {
                continue;
            }
            String text = button.getText() == null ? "" : button.getText().replaceAll("\\s+", " ").trim();
            String type = button.getDomAttribute("type");
            String disabled = button.getDomAttribute("disabled");
            summaries.add("{text='" + text + "', type='" + type + "', enabled=" + button.isEnabled()
                    + ", disabled='" + disabled + "'}");
        }
        return summaries.toString();
    }
}

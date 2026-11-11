package com.example.seleniumtestng.pages.rma;

import com.example.seleniumtestng.models.RmaInspectionItem;
import com.example.seleniumtestng.pages.BasePage;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Handles serial number modal interactions during WMS RMA return inspection:
 * - Detecting visible serial dialogs
 * - Determining required serial scan count
 * - Entering serial codes into the modal and confirming
 */
public class RmaSerialModal extends BasePage {

    public RmaSerialModal(WebDriver driver) {
        super(driver);
    }

    public boolean isSerialDialogVisible() {
        return findVisibleSerialDialog() != null;
    }

    public WebElement findVisibleSerialDialog() {
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

    public void scanSerialsInOpenDialog(String trackingCode, RmaInspectionItem item) {
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

    public List<String> serialCodesForInspection(RmaInspectionItem item, int needed) {
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

    public int serialScanTarget(WebElement dialog, RmaInspectionItem item) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)").matcher(dialog.getText());
        if (matcher.find()) {
            return Math.max(1, Integer.parseInt(matcher.group(2)));
        }
        return item == null ? 1 : Math.max(1, item.quantity());
    }

    public WebElement findSerialInputInDialog(WebElement dialog) {
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

    public boolean clickDialogActionIfPresent(WebElement dialog, String... keywords) {
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
    }

    private String inputValue(WebElement input) {
        try {
            String value = input.getDomProperty("value");
            return value == null ? "" : value.trim();
        } catch (RuntimeException e) {
            return "";
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

    private String inputDescriptor(WebElement element) {
        return normalize(element.getAttribute("placeholder")
                + " " + element.getAttribute("aria-label")
                + " " + element.getAttribute("name")
                + " " + element.getAttribute("id")
                + " " + element.getAttribute("data-placeholder"));
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

    private String summarizeScreenText() {
        try {
            String text = driver.findElement(By.tagName("body")).getText().replaceAll("\\s+", " ").trim();
            return text.length() <= 600 ? text : text.substring(0, 600);
        } catch (RuntimeException e) {
            return "";
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
}


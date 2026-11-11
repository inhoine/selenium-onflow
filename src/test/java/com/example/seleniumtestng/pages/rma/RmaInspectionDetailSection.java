package com.example.seleniumtestng.pages.rma;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.RmaInspectionItem;
import com.example.seleniumtestng.pages.BasePage;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Handles the RMA inspection detail form:
 * - Entering quantities (good, missing, damaged)
 * - Filling shelf life / manufacture / expiry dates via Flatpickr or native inputs
 * - Entering batch / lot numbers, notes, product dimensions and weight
 * - Submitting inspection form and asserting validations
 */
public class RmaInspectionDetailSection extends BasePage {

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
    private final By noteField = By.xpath(
            "//textarea[contains(@name,'note') or contains(@placeholder,'ghi chú') or contains(@placeholder,'ghi chu')]"
                    + " | //input[contains(@name,'note') or contains(@placeholder,'ghi chú') or contains(@placeholder,'ghi chu')]");
    private final By lengthField = By.cssSelector("input[name='goods_d']");
    private final By widthField = By.cssSelector("input[name='goods_w']");
    private final By heightField = By.cssSelector("input[name='goods_h']");
    private final By weightField = By.cssSelector("input[name='goods_weight']");
    private final By clickableControls = By.cssSelector("button, [role='button'], a.btn");

    public RmaInspectionDetailSection(WebDriver driver) {
        super(driver);
    }

    public boolean isInspectionDetailFormVisible() {
        return findVisibleEnabledNow(goodQtyField) != null
                || findVisibleEnabledNow(missingQtyField) != null
                || containsAny(normalize(bodyText()), "sl hang tot", "sl hang thieu", "hu hong loai", "ngay san xuat", "ngay het han");
    }

    public void fillInspectionDetails(RmaInspectionItem item) {
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

    public void inputInspectionQuantities(int goodQuantity) {
        fillNumberField(goodQtyField, "sl hang tot", goodQuantity);
    }

    public void inputManufactureDateIfPresent() {
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

    public boolean hasExpiryFieldVisible() {
        return findVisibleEnabledNow(expiryDateByLabel) != null
                || findVisibleEnabledNow(expiryDateFields) != null
                || findInputNearNormalizedLabel("ngay het han") != null
                || findInputNearNormalizedLabel("han su dung") != null;
    }

    public void inputBatchLotIfPresent() {
        String batchLot = ConfigReader.getOrDefault("RMA_INSPECTION_BATCH_LOT", String.valueOf(System.currentTimeMillis()));
        fillTextField(batchLotField, "ma batch/lot", batchLot);
    }

    public void inputInspectionNoteIfPresent() {
        String note = ConfigReader.getOrDefault("RMA_INSPECTION_NOTE", "Automation QC hang hoan");
        fillTextField(noteField, "ghi chu kiem hang", note);
    }

    public void inputProductDimensionsIfPresent() {
        fillVisibleNumberField(lengthField, ConfigReader.requiredInt("INBOUND_LENGTH"));
        fillVisibleNumberField(widthField, ConfigReader.requiredInt("INBOUND_WIDTH"));
        fillVisibleNumberField(heightField, ConfigReader.requiredInt("INBOUND_HEIGHT"));
        fillVisibleNumberField(weightField, ConfigReader.requiredInt("INBOUND_WEIGHT"));
    }

    public void completeCurrentInspectionIfPresent(String trackingCode, RmaInspectionItem item, RmaSerialModal serialModal) {
        long startedAt = System.nanoTime();
        clickFirstActionIfPresent(150, "hang dat", "binh thuong", "normal");
        clickSubmitInspectionButton(trackingCode, item, serialModal);
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

    public void clickSubmitInspectionButton(String trackingCode, RmaInspectionItem item, RmaSerialModal serialModal) {
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
        if (expectSerial && serialModal.isSerialDialogVisible()) {
            serialModal.scanSerialsInOpenDialog(trackingCode, item);
            serialHandled = true;
        } else {
            clickConfirmIfPresent(150);
            waitForInspectionSubmitResult(expectSerial ? 600 : 700, serialModal);
            if (expectSerial && serialModal.isSerialDialogVisible()) {
                serialModal.scanSerialsInOpenDialog(trackingCode, item);
                serialHandled = true;
            }
        }
        if (serialHandled && isInspectionDetailFormVisible() && !serialModal.isSerialDialogVisible()) {
            WebElement retryButton = findInspectionSubmitButton(1200);
            if (retryButton != null) {
                clickElement(retryButton);
                waitAfterScan(200);
                if (!serialModal.isSerialDialogVisible()) {
                    clickConfirmIfPresent();
                }
                waitAfterScan(300);
            }
        }
        assertNoRequiredValidationAfterSubmit(trackingCode);
    }

    public void assertRequiredInspectionFieldsFilled(int expectedGoodQuantity, boolean expiryFieldVisible) {
        assertLocatorOrLabelValue(goodQtyField, "sl hang tot", String.valueOf(expectedGoodQuantity), true);
        if (expiryFieldVisible) {
            assertFieldValue("ngay san xuat", null, false);
        }
        assertFieldValue("ma batch/lot", null, false);
        assertFieldValue("ghi chu kiem hang", null, false);
    }

    public void assertNoRequiredValidationAfterSubmit(String trackingCode) {
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

    public void assertNoOpenInspectionFormBeforeSessionFinish() {
        if (!isInspectionDetailFormVisible()) {
            return;
        }
        String validation = visibleValidationText();
        throw new IllegalStateException("Refusing to finish RMA inspection session while detail form is still open"
                + (validation.isBlank() ? "" : " with validation: " + validation)
                + ". Screen="
                + summarizeScreenText());
    }

    public String visibleValidationText() {
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

    public int shelfLifeDays() {
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

    public Integer shelfLifeDaysFromWarning() {
        Matcher matcher = Pattern.compile("h[eệ]\\s*th[oố]ng\\s*\\((\\d+)\\s*ng[aà]y\\)", Pattern.CASE_INSENSITIVE)
                .matcher(normalize(bodyText()));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    public Integer minimumExpiryDays() {
        Matcher matcher = Pattern.compile("minimum\\s+(\\d+)\\s+ng\\S*y", Pattern.CASE_INSENSITIVE)
                .matcher(bodyText());
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    public void fillNumberField(By locator, String normalizedLabel, Number value) {
        if (value == null) {
            return;
        }
        if (fillTextField(locator, normalizedLabel, String.valueOf(value))) {
            return;
        }
        System.out.println("Numeric field not found for label: " + normalizedLabel);
    }

    public void fillVisibleNumberField(By locator, Number value) {
        if (value == null) {
            return;
        }
        WebElement input = findVisibleEnabledNow(locator);
        if (input != null) {
            setAnyEditableValue(input, String.valueOf(value));
        }
    }

    public boolean fillTextField(By locator, String normalizedLabel, String value) {
        WebElement input = findVisibleEnabledNow(locator);
        if (input != null) {
            setAnyEditableValue(input, value);
            return value.equals(inputValue(input));
        }
        return fillInputNearNormalizedLabel(normalizedLabel, value);
    }

    public boolean fillDateIfPresent(By primaryLocator, By fallbackLocator, String normalizedLabel, LocalDate date) {
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

    public WebElement findVisibleEnabledNow(By locator) {
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

    public boolean fillInputNearNormalizedLabel(String normalizedLabel, String value) {
        WebElement input = findInputNearNormalizedLabel(normalizedLabel);
        if (input == null) {
            return false;
        }
        setAnyEditableValue(input, value);
        return value.equals(inputValue(input));
    }

    public WebElement findInputNearNormalizedLabel(String normalizedLabel) {
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

    public void setAnyEditableValue(WebElement element, String value) {
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

    private void waitForInspectionSubmitResult(long timeoutMillis, RmaSerialModal serialModal) {
        try {
            shortWait(timeoutMillis).until(driver -> {
                if ((serialModal != null && serialModal.isSerialDialogVisible()) || !isInspectionDetailFormVisible()) {
                    return true;
                }
                return null;
            });
        } catch (RuntimeException ignored) {
        }
    }

    private boolean isSessionFinishButton(WebElement element) {
        String text = normalize(element.getText());
        return text.contains("hoan tat phien kiem");
    }

    private boolean isClickableControl(WebElement element) {
        try {
            return element.isDisplayed() && element.isEnabled();
        } catch (RuntimeException e) {
            return false;
        }
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

    private boolean hasSerialCodes(RmaInspectionItem item) {
        return item != null && item.serialCodes() != null && !item.serialCodes().isEmpty();
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

    private String inputValue(WebElement input) {
        try {
            String value = input.getDomProperty("value");
            return value == null ? "" : value.trim();
        } catch (RuntimeException e) {
            return "";
        }
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
}


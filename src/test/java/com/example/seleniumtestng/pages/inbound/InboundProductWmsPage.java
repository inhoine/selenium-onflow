package com.example.seleniumtestng.pages.inbound;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.POSku;
import com.example.seleniumtestng.pages.BasePage;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Main Page Object / Facade for WMS Inbound Inspection:
 * - Scanning PO code & Box code
 * - Product list view & row inspection actions
 * - Inspect form input (quantities, batch, dimensions)
 * - Composes specialized sub-components: InboundSerialModal,
 * InboundBarcodeModal,
 * InboundShelfLifeSection, InboundAttachmentModal
 */
public class InboundProductWmsPage extends BasePage {
    private static final long POST_INSPECT_WAIT_MS = 3500;
    private static final long POST_INSPECT_RECOVERY_WAIT_MS = 6000;
    private static final long ATTACHMENT_WAIT_MS = 2500;

    private final By scanPoField = By.xpath("//input[contains(@placeholder,'PO')]");
    private final By scanBoxField = By.xpath("//input["
            + "contains(@placeholder,'Kiện') "
            + "or contains(@placeholder,'kiện') "
            + "or contains(@placeholder,'Kien') "
            + "or contains(@placeholder,'kien') "
            + "or contains(@placeholder,'Barcode')]");
    private final By selectProductBtn = By.xpath("//button[i[contains(@class,'ri-more') or contains(@class,'ellipsis')]"
            + " or contains(normalize-space(.),'...') or @aria-haspopup='menu' or @aria-expanded]");
    private final By goodQtyField = By.xpath(
            "//input[@name='quantity_goods_normal' or @name='quantity_received' or @name='quantity_receive' "
                    + "or contains(@name,'normal') or contains(@name,'received') or contains(@name,'receive')]");
    private final By lostQtyField = By.xpath(
            "//input[@name='quantity_goods_lost' or @name='quantity_lost' or contains(@name,'lost')]");
    private final By damagedType1QtyField = By.xpath(
            "//input[@name='quantity_goods_damaged' or @name='quantity_damaged' or contains(@name,'damaged')]");
    private final By damagedType2QtyField = By.xpath(
            "//input[@name='quantity_goods_damaged_2' or @name='quantity_damaged_2']");
    private final By damagedType3QtyField = By.xpath(
            "//input[@name='quantity_goods_damaged_3' or @name='quantity_damaged_3']");
    private final By batchLotField = By.xpath("//input["
            + "@name='batch_lot_code' "
            + "or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'batch') "
            + "or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'lot') "
            + "or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'batch') "
            + "or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'lot')]");
    private final By lengthField = By.cssSelector("input[name='goods_d']");
    private final By widthField = By.cssSelector("input[name='goods_w']");
    private final By heightField = By.cssSelector("input[name='goods_h']");
    private final By weightField = By.cssSelector("input[name='goods_weight']");
    private final By confirmInspectBtn = By.xpath(
            "//button["
                    + "contains(normalize-space(.),'Kiểm hàng') "
                    + "or contains(normalize-space(.),'Kiem hang') "
                    + "or normalize-space(.)='Xác nhận' "
                    + "or normalize-space(.)='Xac nhan' "
                    + "or normalize-space(.)='Cập nhật' "
                    + "or normalize-space(.)='Cap nhat' "
                    + "or normalize-space(.)='Hoàn tất' "
                    + "or normalize-space(.)='Hoan tat' "
                    + "or normalize-space(.)='Lưu' "
                    + "or normalize-space(.)='Luu']");
    private final By submitInspectBtn = By.cssSelector("form button[type='submit'], button[type='submit']");
    private final By inspectMenuItem = By.xpath(
            "//*[contains(normalize-space(.),'Kiểm hàng') or contains(normalize-space(.),'Kiem hang')]"
                    + "[self::button or self::a or self::li or self::div or @role='menuitem']"
                    + "[@role='menuitem' or contains(@class,'dropdown-item') or contains(@class,'menu-item') "
                    + "or ancestor::*[@role='menu' or contains(@class,'dropdown-menu') or contains(@class,'popover')]]");
    private final By inspectMenuItemFallback = By.xpath(
            "//*[not(ancestor-or-self::nav or ancestor-or-self::aside or contains(@class,'sidebar') or contains(@class,'navbar') or contains(@class,'header'))]"
                    + "//*[(self::button or self::a or self::li or self::div or self::span or @role='menuitem') "
                    + "and (contains(normalize-space(.),'Kiểm hàng') or contains(normalize-space(.),'Kiem hang')) "
                    + "and not(contains(normalize-space(.),'Không nhận')) "
                    + "and not(contains(normalize-space(.),'Hoàn tất')) "
                    + "and not(contains(normalize-space(.),'DANH SÁCH'))]");
    private final By finishInspectionSessionBtn = By.xpath(
            "//button[contains(normalize-space(.),'Hoàn tất phiên kiểm') or contains(normalize-space(.),'Hoan tat phien kiem')]");
    private final By productListTitle = By.xpath(
            "//*[contains(normalize-space(.),'DANH SÁCH SẢN PHẨM') or contains(normalize-space(.),'DANH SACH SAN PHAM')]");
    private final By backButton = By
            .xpath("//button[contains(normalize-space(.),'Quay l') or contains(normalize-space(.),'Back')]");
    private final By productRows = By.cssSelector("tbody tr");

    // Sub-components
    private final InboundSerialModal serialModal;
    private final InboundBarcodeModal barcodeModal;
    private final InboundShelfLifeSection shelfLifeSection;
    private final InboundAttachmentModal attachmentModal;

    private String lastInspectOpenFailure = "";

    public InboundProductWmsPage(WebDriver driver) {
        super(driver);
        this.serialModal = new InboundSerialModal(driver);
        this.barcodeModal = new InboundBarcodeModal(driver);
        this.shelfLifeSection = new InboundShelfLifeSection(driver);
        this.attachmentModal = new InboundAttachmentModal(driver);
    }

    public InboundSerialModal serialModal() {
        return serialModal;
    }

    public InboundBarcodeModal barcodeModal() {
        return barcodeModal;
    }

    public InboundShelfLifeSection shelfLifeSection() {
        return shelfLifeSection;
    }

    public InboundAttachmentModal attachmentModal() {
        return attachmentModal;
    }

    // --- Scanning & PO Navigation ---

    public void scanPo(String poCode) {
        scanCode(scanPoField, poCode);
    }

    public void scanBox(String boxCode) {
        scanCode(scanBoxField, boxCode);
    }

    public void scanBoxIfNeeded(String boxCode) {
        if (scanCodeIfPresent(scanBoxField, boxCode)) {
            return;
        }
        if (hasProductRowForBox(boxCode)) {
            return;
        }
        returnToScanBoxFromProductListIfPresent();
        if (scanCodeIfPresent(scanBoxField, boxCode)) {
            return;
        }

        shortWait(5000).until(driver -> {
            if (scanCodeIfPresent(scanBoxField, boxCode)) {
                return true;
            }
            return hasProductRowForBox(boxCode) ? true : null;
        });
    }

    private boolean returnToScanBoxFromProductListIfPresent() {
        if (firstVisibleEnabled(scanBoxField) != null || !isProductListViewVisible()) {
            return false;
        }
        WebElement button = firstVisibleEnabled(backButton);
        if (button == null) {
            return false;
        }
        jsClick(button);
        try {
            shortWait(2000).until(driver -> firstVisibleEnabled(scanBoxField) != null ? true : null);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // --- Product List Inspection Actions ---

    public void waitProductActionReady() {
        wait.until(ExpectedConditions.elementToBeClickable(selectProductBtn));
    }

    public List<ScannedInboundProduct> scannedProducts(String boxCode) {
        return scannedProducts(boxCode, 0);
    }

    public List<ScannedInboundProduct> scannedProductsIfPresent(String boxCode) {
        try {
            shortWait(500).until(driver -> hasProductRowForBox(boxCode) ? true : null);
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
        return currentScannedProducts(boxCode, 0);
    }

    public List<ScannedInboundProduct> scannedProducts(String boxCode, int limit) {
        wait.until(driver -> hasProductRowForBox(boxCode) ? true : null);
        return currentScannedProducts(boxCode, limit);
    }

    private List<ScannedInboundProduct> currentScannedProducts(String boxCode, int limit) {
        List<ScannedInboundProduct> products = new ArrayList<>();
        for (WebElement row : all(productRows)) {
            if (!rowBelongsToScannedBox(row, boxCode)) {
                continue;
            }
            ScannedInboundProduct product = scannedProductFromRow(row, boxCode);
            if (product == null) {
                continue;
            }
            products.add(product);
            if (limit > 0 && products.size() >= limit) {
                break;
            }
        }
        return products;
    }

    public ScannedInboundProduct openFirstInspectableProduct(String boxCode) {
        lastInspectOpenFailure = "";

        try {
            return shortWait(5000).until(driver -> {
                for (WebElement row : all(productRows)) {
                    if (!isDisplayed(row)) {
                        continue;
                    }
                    ScannedInboundProduct product = scannedProductFromRow(row, boxCode);
                    if (product == null || !boxCode.equals(product.boxCode()) || product.quantityInbound() <= 0) {
                        continue;
                    }
                    if (clickInspectActionInRow(row)) {
                        return product;
                    }
                }
                return null;
            });
        } catch (RuntimeException e) {
            if (lastInspectOpenFailure.isBlank()) {
                lastInspectOpenFailure = "no product row ready for box " + boxCode;
            }
            return null;
        }
    }

    public void inspectProduct(String boxCode, String sku) {
        if (!tryOpenInspectProduct(boxCode, sku)) {
            throw new IllegalStateException("Product is not available for inspection: box=" + boxCode + ", sku=" + sku);
        }
    }

    public boolean tryOpenInspectProduct(String boxCode, String sku) {
        lastInspectOpenFailure = "";
        WebElement row = findProductRow(boxCode, sku);
        if (row == null) {
            lastInspectOpenFailure = "row not found";
            return false;
        }
        if (!clickInspectActionInRow(row)) {
            return false;
        }
        return true;
    }

    public String lastInspectOpenFailure() {
        return lastInspectOpenFailure;
    }

    public boolean finishInspectionSessionIfPresent() {
        WebElement button = firstVisibleEnabled(finishInspectionSessionBtn);
        if (button == null) {
            return false;
        }
        jsClick(button);
        try {
            attachmentModal.clickConfirmDialogIfPresent();
        } catch (RuntimeException ignored) {
        }
        try {
            shortWait(5000).until(driver -> firstVisibleEnabled(finishInspectionSessionBtn) == null ? true : null);
        } catch (RuntimeException ignored) {
        }
        return true;
    }

    public void inspectProduct() {
        click(selectProductBtn);
        clickVisibleInspectMenuItem();
    }

    // --- Quantity Inputs ---

    public void inputGoodQuantity(int quantity) {
        setEditableInputValue(goodQtyField, String.valueOf(quantity));
    }

    public void inputLostQuantity(int quantity) {
        type(lostQtyField, String.valueOf(quantity));
    }

    public void inputDamagedType1Quantity(int quantity) {
        type(damagedType1QtyField, String.valueOf(quantity));
    }

    public void inputDamagedType2Quantity(int quantity) {
        type(damagedType2QtyField, String.valueOf(quantity));
    }

    public void inputDamagedType3Quantity(int quantity) {
        type(damagedType3QtyField, String.valueOf(quantity));
    }

    // --- Barcode Delegation ---

    public void inputBarcode() {
        barcodeModal.inputBarcode();
    }

    public void inputBarcode(String barcode) {
        barcodeModal.inputBarcode(barcode);
    }

    public void inputBarcodeFromProductModal() {
        barcodeModal.inputBarcodeFromProductModal();
    }

    public String readBarcodeFromProductModal() {
        return barcodeModal.readBarcodeFromProductModal();
    }

    // --- Batch / Lot Inputs ---

    public void inputBatchLotIfPresent() {
        WebElement field = firstEditable(batchLotField);
        if (field != null) {
            setInputValue(field, "BATCH-" + System.currentTimeMillis());
        }
    }

    public void clearBatchLotIfPresent() {
        WebElement field = firstEditable(batchLotField);
        if (field != null) {
            setInputValue(field, "");
        }
    }

    // --- Serial Modal Delegation ---

    public void inputSerialsIfPresent(int quantity, String sku) {
        serialModal.inputSerialsIfPresent(quantity, sku);
    }

    public void inputSerialsIfPresent(int quantity, String sku, int serialCount) {
        serialModal.inputSerialsIfPresent(quantity, sku, serialCount);
    }

    public void inputDuplicateSerialsIfPresent(String sku) {
        serialModal.inputDuplicateSerialsIfPresent(sku);
    }

    public boolean isSerialModalVisible() {
        return serialModal.isSerialModalVisible();
    }

    // --- Shelf-Life Delegation ---

    public void inputShelfLifeDatesIfPresent() {
        shelfLifeSection.inputShelfLifeDatesIfPresent();
    }

    public void clearShelfLifeDatesIfPresent() {
        shelfLifeSection.clearShelfLifeDatesIfPresent();
    }

    public void inputMismatchedShelfLifeDatesIfPresent() {
        shelfLifeSection.inputMismatchedShelfLifeDatesIfPresent();
    }

    // --- Dimensions & Weight Inputs ---

    public void inputProductDimensions(POSku sku) {
        SystemDimensions redlineDimensions = systemDimensionsFromRedline();
        fillNumberField(lengthField, "chieu dai",
                valueOrDefault(redlineDimensions.length, sku.goodsD(), "INBOUND_LENGTH"));
        fillNumberField(widthField, "chieu rong",
                valueOrDefault(redlineDimensions.width, sku.goodsW(), "INBOUND_WIDTH"));
        fillNumberField(heightField, "chieu cao",
                valueOrDefault(redlineDimensions.height, sku.goodsH(), "INBOUND_HEIGHT"));
        fillNumberField(weightField, "can nang",
                valueOrDefault(redlineDimensions.weight, sku.goodsWeight(), "INBOUND_WEIGHT"));
    }

    public void inputProductDimensions(Number length, Number width, Number height, Number weight) {
        fillNumberField(lengthField, "chieu dai", length);
        fillNumberField(widthField, "chieu rong", width);
        fillNumberField(heightField, "chieu cao", height);
        fillNumberField(weightField, "can nang", weight);
    }

    public void inputProductDimensionsExcept(POSku sku, String omittedField) {
        SystemDimensions redlineDimensions = systemDimensionsFromRedline();
        if ("length".equals(omittedField)) {
            clearNumberField(lengthField, "chieu dai");
        } else {
            fillNumberField(lengthField, "chieu dai",
                    valueOrDefault(redlineDimensions.length, sku.goodsD(), "INBOUND_LENGTH"));
        }
        if ("width".equals(omittedField)) {
            clearNumberField(widthField, "chieu rong");
        } else {
            fillNumberField(widthField, "chieu rong",
                    valueOrDefault(redlineDimensions.width, sku.goodsW(), "INBOUND_WIDTH"));
        }
        if ("height".equals(omittedField)) {
            clearNumberField(heightField, "chieu cao");
        } else {
            fillNumberField(heightField, "chieu cao",
                    valueOrDefault(redlineDimensions.height, sku.goodsH(), "INBOUND_HEIGHT"));
        }
        if ("weight".equals(omittedField)) {
            clearNumberField(weightField, "can nang");
        } else {
            fillNumberField(weightField, "can nang",
                    valueOrDefault(redlineDimensions.weight, sku.goodsWeight(), "INBOUND_WEIGHT"));
        }
    }

    public void clearProductDimension(String field) {
        if ("length".equals(field)) {
            clearNumberField(lengthField, "chieu dai");
        } else if ("width".equals(field)) {
            clearNumberField(widthField, "chieu rong");
        } else if ("height".equals(field)) {
            clearNumberField(heightField, "chieu cao");
        } else if ("weight".equals(field)) {
            clearNumberField(weightField, "can nang");
        }
    }

    // --- Inspection Submit & Confirmation ---

    public String submitInspectExpectingValidation() {
        clickConfirmInspectButton();
        try {
            shortWait(2000).until(driver -> isInspectionFormVisibleStrict() ? true : null);
        } catch (RuntimeException ignored) {
        }
        if (isPostInspectReady()) {
            throw new AssertionError("Expected inspection validation, but submit appears to have completed");
        }
        String validationText = visibleValidationText();
        if (validationText.isBlank()) {
            throw new AssertionError("Expected inspection validation, but no visible validation text was found");
        }
        return validationText;
    }

    public boolean isConfirmInspectDisabled() {
        WebElement button = confirmInspectButtonByLocatorOrText(false);
        return button != null && !button.isEnabled();
    }

    public void confirmInspect() {
        if (!tryConfirmInspectAndReturnToScanBox(POST_INSPECT_WAIT_MS)) {
            ensureReturnedAfterInspectSubmit();
        }
    }

    public void confirmInspectWithRedlineRetry(POSku sku) {
        if (tryConfirmInspectAndReturnToScanBox(1200)) {
            return;
        }

        SystemDimensions redlineDimensions = systemDimensionsFromRedline();
        if (!redlineDimensions.hasAnyValue()) {
            Integer shelfLifeDays = shelfLifeSection.shelfLifeDaysFromWarning();
            if (shelfLifeDays != null) {
                shelfLifeSection.inputShelfLifeDatesIfPresent();
                if (!tryConfirmInspectAndReturnToScanBox(POST_INSPECT_WAIT_MS)) {
                    if (retryInspectSubmitIfFormStillOpen(POST_INSPECT_RECOVERY_WAIT_MS)) {
                        return;
                    }
                    System.out.println("Inspect submit did not return to scan box after shelf-life retry. Buttons="
                            + visibleButtonSummary());
                    ensureReturnedAfterInspectSubmit();
                }
                return;
            }
            if (!waitForScanBoxOrSkipAttachment(POST_INSPECT_WAIT_MS)) {
                if (retryInspectSubmitIfFormStillOpen(POST_INSPECT_RECOVERY_WAIT_MS)) {
                    return;
                }
                System.out.println("Inspect submit did not return to scan box. Buttons=" + visibleButtonSummary());
                ensureReturnedAfterInspectSubmit();
            }
            return;
        }
        shelfLifeSection.inputShelfLifeDatesIfPresent();
        inputProductDimensions(sku);
        if (!tryConfirmInspectAndReturnToScanBox(POST_INSPECT_WAIT_MS)) {
            if (retryInspectSubmitIfFormStillOpen(POST_INSPECT_RECOVERY_WAIT_MS)) {
                return;
            }
            System.out.println("Inspect submit did not return to scan box after redline retry. Buttons="
                    + visibleButtonSummary());
            ensureReturnedAfterInspectSubmit();
        }
    }

    private boolean tryConfirmInspectAndReturnToScanBox(long millis) {
        clickConfirmInspectButton();
        return waitForScanBoxOrSkipAttachment(millis);
    }

    private void clickConfirmInspectButton() {
        WebElement button = confirmInspectButtonByLocatorOrText(true);
        if (button == null) {
            System.out.println("Visible buttons before confirm inspect: " + visibleButtonSummary());
            button = shortWait(2500).until(driver -> {
                WebElement candidate = confirmInspectButtonByLocatorOrText(true);
                return candidate == null ? null : candidate;
            });
        }
        button.click();
    }

    private boolean retryInspectSubmitIfFormStillOpen(long millis) {
        if (!isInspectionFormVisibleStrict()) {
            return false;
        }
        WebElement button = confirmInspectButtonByLocatorOrText(true);
        if (button == null) {
            return false;
        }
        System.out.println("Retry clicking confirm inspect because form is still open. Button="
                + button.getText().replaceAll("\\s+", " ").trim());
        try {
            button.click();
        } catch (RuntimeException e) {
            jsClick(button);
        }
        return waitForScanBoxOrSkipAttachment(millis);
    }

    private WebElement confirmInspectButtonIfPresent() {
        return confirmInspectButtonByLocatorOrText(true);
    }

    private WebElement confirmInspectButtonByLocatorOrText(boolean mustBeEnabled) {
        for (WebElement button : all(confirmInspectBtn)) {
            if (isDisplayed(button) && (!mustBeEnabled || button.isEnabled())) {
                return button;
            }
        }
        for (WebElement button : all(submitInspectBtn)) {
            if (isDisplayed(button) && (!mustBeEnabled || button.isEnabled())) {
                return button;
            }
        }
        for (WebElement button : all(By.cssSelector("button"))) {
            if (!isDisplayed(button) || (mustBeEnabled && !button.isEnabled())) {
                continue;
            }
            String normalizedText = normalized(button.getText());
            if (isInspectSubmitText(normalizedText)) {
                return button;
            }
        }
        return null;
    }

    private boolean isInspectSubmitText(String normalizedText) {
        return normalizedText.contains("xac nhan kiem hang")
                || normalizedText.equals("kiem hang")
                || normalizedText.equals("xac nhan")
                || normalizedText.equals("cap nhat")
                || normalizedText.equals("hoan tat")
                || normalizedText.equals("luu");
    }

    private boolean waitForScanBoxOrSkipAttachment(long millis) {
        try {
            boolean[] clickedBack = { false };
            return shortWait(millis).until(driver -> {
                if (attachmentModal.skipAttachmentIfPresent()) {
                    return attachmentModal.waitUntilAttachmentModalClosed(
                            this::isPostInspectReady,
                            this::clickBackAfterSubmittedDetailIfPresent,
                            ATTACHMENT_WAIT_MS);
                }
                if (isPostInspectReady()) {
                    return true;
                }
                if (!clickedBack[0] && clickBackAfterSubmittedDetailIfPresent()) {
                    clickedBack[0] = true;
                }
                return null;
            });
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void ensureReturnedAfterInspectSubmit() {
        try {
            boolean[] clickedBack = { false };
            shortWait(POST_INSPECT_RECOVERY_WAIT_MS).until(driver -> {
                if (isPostInspectReady()) {
                    return true;
                }
                if (!clickedBack[0] && clickBackAfterSubmittedDetailIfPresent()) {
                    clickedBack[0] = true;
                }
                return null;
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException("Inspect submit did not reach scan box or product list. Buttons="
                    + visibleButtonSummary(), e);
        }
    }

    private boolean isPostInspectReady() {
        if (firstVisibleEnabled(scanBoxField) != null) {
            return true;
        }
        if (isProductListViewVisible()) {

            return true;
        }
        return hasVisibleProductRows() && !attachmentModal.isAttachmentModalVisible()
                && !isInspectionFormVisibleStrict();
    }

    public String visibleValidationText() {
        String bodyText;
        try {
            bodyText = driver.findElement(By.tagName("body")).getText();
        } catch (RuntimeException e) {
            return "";
        }
        String normalized = normalizeForMatch(bodyText);
        if (normalized.contains("vui long")
                || normalized.contains("khong")
                || normalized.contains("chenh lech")
                || normalized.contains("barcode")
                || normalized.contains("lon hon")
                || normalized.contains("bat buoc")
                || normalized.contains("serial")
                || normalized.contains("trung")) {
            return compactForLog(bodyText);
        }
        return "";
    }

    private boolean clickBackAfterSubmittedDetailIfPresent() {
        if (attachmentModal.isAttachmentModalVisible()
                || isInspectionFormVisibleStrict()
                || firstVisibleEnabled(scanBoxField) != null
                || isProductListViewVisible()) {
            return false;
        }
        WebElement button = firstVisibleEnabled(backButton);
        if (button == null) {
            return false;
        }
        jsClick(button);
        return true;
    }

    private void clickVisibleInspectMenuItem() {
        if (clickVisibleInspectMenuItemIfPresent(2000)) {
            return;
        }
        throw new IllegalStateException("Visible inspect product button not found");
    }

    private boolean clickVisibleInspectMenuItemIfPresent() {
        return clickVisibleInspectMenuItemIfPresent(2000);
    }

    private boolean clickVisibleInspectMenuItemIfPresent(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (isInspectionFormVisible()) {
                return true;
            }

            // Strategy 1: Targeted JavaScript search scoped to popup menu (strictly
            // excludes sidebar/navbar/header)
            // Finds the exact leaf element with text "Kiểm hàng" and dispatches
            // pointer/mouse events and clicks
            Object clicked = null;
            try {
                clicked = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "const norm = v => (v || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();"
                                + "const isVisible = el => {"
                                + "  if (!el) return false;"
                                + "  try {"
                                + "    const s = window.getComputedStyle(el);"
                                + "    const r = el.getBoundingClientRect();"
                                + "    return s.visibility !== 'hidden' && s.display !== 'none' && r.width > 0 && r.height > 0;"
                                + "  } catch (e) { return false; }"
                                + "};"
                                + "const isExcluded = el => !!el.closest('nav, aside, [class*=\"sidebar\"], [class*=\"navbar\"], [class*=\"header\"], [class*=\"breadcrumb\"]');"
                                + "const allMatches = Array.from(document.querySelectorAll('*'))"
                                + "  .filter(isVisible)"
                                + "  .filter(el => !isExcluded(el))"
                                + "  .filter(el => {"
                                + "    const t = norm(el.innerText || el.textContent);"
                                + "    return t.includes('kiem hang') && !t.includes('khong nhan') && !t.includes('hoan tat') && !t.includes('danh sach');"
                                + "  });"
                                + "const leafMatches = allMatches.filter(el => !Array.from(el.children).some(child => isVisible(child) && norm(child.innerText || child.textContent).includes('kiem hang')));"
                                + "const candidates = leafMatches.length > 0 ? leafMatches : allMatches;"
                                + "if (candidates.length === 0) return false;"
                                + "candidates.sort((a, b) => (a.innerText || a.textContent || '').trim().length - (b.innerText || b.textContent || '').trim().length);"
                                + "const leaf = candidates[0];"
                                + "const clickable = leaf.closest('button, a, [role=\"menuitem\"], [role=\"button\"], li') || leaf;"
                                + "for (const el of [leaf, clickable]) {"
                                + "  try {"
                                + "    el.scrollIntoView({block: 'center', inline: 'center'});"
                                + "    for (const type of ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click']) {"
                                + "      el.dispatchEvent(new MouseEvent(type, { bubbles: true, cancelable: true, view: window }));"
                                + "    }"
                                + "    if (typeof el.click === 'function') { el.click(); }"
                                + "  } catch (e) {}"
                                + "}"
                                + "return true;");
            } catch (RuntimeException ignored) {
            }

            if (Boolean.TRUE.equals(clicked)) {
                if (waitUntilInspectionFormVisible(3000)) {
                    return true;
                }
            }

            // Strategy 2: Selenium element lookup with click, sorted by shortest text
            List<WebElement> buttons = new ArrayList<>(all(inspectMenuItem));
            if (buttons.isEmpty()) {
                buttons.addAll(all(inspectMenuItemFallback));
            }
            buttons.removeIf(b -> !isDisplayed(b));
            buttons.sort(Comparator.comparingInt(b -> safeText(b).length()));
            for (WebElement button : buttons) {
                try {
                    jsClick(button);
                } catch (RuntimeException ignored) {
                    try {
                        button.click();
                    } catch (RuntimeException ignoredToo) {
                    }
                }
                if (waitUntilInspectionFormVisible(2500)) {
                    return true;
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return isInspectionFormVisible();
    }

    private boolean clickInspectActionInRow(WebElement row) {
        if (isInspectionFormVisible()) {
            return true;
        }

        List<WebElement> actionButtons;
        try {
            actionButtons = rowActionCandidates(row);
        } catch (RuntimeException e) {
            lastInspectOpenFailure = "row refreshed while locating action button";
            return false;
        }
        if (actionButtons.isEmpty()) {
            lastInspectOpenFailure = "row has no action button";
            return false;
        }

        for (WebElement button : actionButtons) {
            if (!isDisplayed(button) || !button.isEnabled()) {
                continue;
            }
            try {
                jsClick(button);
            } catch (RuntimeException e) {
                try {
                    button.click();
                } catch (RuntimeException ignored) {
                    lastInspectOpenFailure = "action button refreshed while clicking";
                    continue;
                }
            }

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (clickVisibleInspectMenuItemIfPresent(3500)) {
                return true;
            }
            if (waitUntilInspectionFormVisible(1000)) {
                return true;
            }
            closeOpenMenu();
        }

        lastInspectOpenFailure = "dropdown opened but inspect menu is not available; row="
                + compactForLog(safeText(row));
        return false;
    }

    private List<WebElement> rowActionCandidates(WebElement row) {
        List<WebElement> priorityButtons = new ArrayList<>();
        List<WebElement> fallbackButtons = new ArrayList<>();

        // Priority 1: Controls inside the last column (Hành động)
        for (WebElement ctrl : row.findElements(By.cssSelector(
                "td:last-child button, td:last-child [role='button'], td:last-child a, td:last-child div[class*='cursor'], td:last-child span[class*='cursor']"))) {
            if (isDisplayed(ctrl) && ctrl.isEnabled()) {
                priorityButtons.add(ctrl);
            }
        }

        // Priority 2: Any button in the row matching action menu patterns
        for (WebElement button : row.findElements(By.cssSelector("button, [role='button']"))) {
            if (!isDisplayed(button) || !button.isEnabled() || priorityButtons.contains(button)) {
                continue;
            }
            if (looksLikeActionMenu(button)) {
                priorityButtons.add(button);
            } else {
                fallbackButtons.add(button);
            }
        }
        priorityButtons.addAll(fallbackButtons);
        return priorityButtons;
    }

    private boolean looksLikeActionMenu(WebElement button) {
        String text = safeText(button);
        String classes = attribute(button, "class");
        String ariaHasPopup = attribute(button, "aria-haspopup");
        String ariaExpanded = attribute(button, "aria-expanded");
        if ((text != null && (text.replaceAll("\\s+", "").contains("...") || text.contains("…")))
                || containsIgnoreCase(classes, "dropdown")
                || containsIgnoreCase(classes, "more")
                || containsIgnoreCase(classes, "ellipsis")
                || containsIgnoreCase(classes, "action")
                || containsIgnoreCase(ariaHasPopup, "menu")
                || ariaExpanded != null) {
            return true;
        }
        return !button.findElements(By.cssSelector(
                "i[class*='ri-more'], i[class*='more'], i[class*='ellipsis'], "
                        + "svg[class*='more'], svg[class*='ellipsis']"))
                .isEmpty();
    }

    private void closeOpenMenu() {
        try {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
        } catch (RuntimeException ignored) {
        }
    }

    private boolean waitUntilInspectionFormVisible() {
        return waitUntilInspectionFormVisible(ConfigReader.timeout().toMillis());
    }

    private boolean waitUntilInspectionFormVisible(long timeoutMillis) {
        try {
            shortWait(timeoutMillis).until(driver -> isInspectionFormVisible() ? true : null);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isInspectionFormVisible() {
        return firstEditable(goodQtyField) != null
                || barcodeModal.isBarcodeFieldVisible()
                || firstEditable(lostQtyField) != null
                || firstEditable(damagedType1QtyField) != null;
    }

    private boolean isInspectionFormVisibleStrict() {
        return firstEditable(goodQtyField) != null
                || barcodeModal.isBarcodeFieldVisible();
    }

    private boolean isProductListViewVisible() {
        return firstVisibleEnabled(productListTitle) != null
                || (hasVisibleProductRows() && !isInspectionFormVisibleStrict());
    }

    public boolean hasActionableProduct(String boxCode, String sku) {
        return findProductRow(boxCode, sku) != null;
    }

    private WebElement productRow(String boxCode, String sku) {
        WebElement row = findProductRow(boxCode, sku);
        if (row == null) {
            throw new IllegalStateException("Product row not found for box: " + boxCode + ", sku: " + sku);
        }
        return row;
    }

    private WebElement findProductRow(String boxCode, String sku) {
        for (WebElement row : productRowsForBox(boxCode)) {
            if (sku == null || containsIgnoreCase(safeText(row), sku)) {
                return row;
            }
        }
        return null;
    }

    private boolean hasProductRowForBox(String boxCode) {
        return !productRowsForBox(boxCode).isEmpty();
    }

    private boolean hasVisibleProductRows() {
        for (WebElement row : all(productRows)) {
            if (isDisplayed(row)) {
                return true;
            }
        }
        return false;
    }

    private List<WebElement> productRowsForBox(String boxCode) {
        List<WebElement> rows = new ArrayList<>();
        for (WebElement row : all(productRows)) {
            if (!isDisplayed(row)) {
                continue;
            }
            if (rowBelongsToScannedBox(row, boxCode)) {
                rows.add(row);
            }
        }
        return rows;
    }

    private boolean rowBelongsToScannedBox(WebElement row, String boxCode) {
        if (boxCode == null || boxCode.isBlank()) {
            return true;
        }
        String rowText = safeText(row);
        if (containsIgnoreCase(rowText, boxCode)) {
            return true;
        }
        return hasScannedBoxContext(boxCode);
    }

    private boolean hasScannedBoxContext(String boxCode) {
        if (boxCode == null || boxCode.isBlank()) {
            return false;
        }
        String bodyText = safeText(driver.findElement(By.tagName("body")));
        return containsIgnoreCase(bodyText, boxCode);
    }

    private ScannedInboundProduct scannedProductFromRow(WebElement row, String boxCode) {
        String text = safeText(row);
        if (text == null || text.isBlank()) {
            return null;
        }

        List<WebElement> cells = row.findElements(By.tagName("td"));
        if (cells.size() >= 5) {
            String sku = lastNonBlankLine(safeText(cells.get(2)));
            int quantityInbound = parseQuantity(safeText(cells.get(3)));
            int quantityInspected = parseQuantity(safeText(cells.get(4)));
            int remaining = remainingQuantity(quantityInbound, quantityInspected);
            return new ScannedInboundProduct(boxCode, sku, quantityInbound, remaining);
        }

        String[] lines = text.split("\\R");
        String sku = lines.length > 2 ? lines[2].trim() : lines[0].trim();
        int total = sumQuantities(text);
        return new ScannedInboundProduct(boxCode, sku, total, total);
    }

    private String lastNonBlankLine(String text) {
        if (text == null) {
            return "";
        }
        String[] lines = text.split("\\R");
        for (int index = lines.length - 1; index >= 0; index--) {
            String line = lines[index].trim();
            if (!line.isBlank()) {
                return line;
            }
        }
        return "";
    }

    private int parseQuantity(String text) {
        if (text == null) {
            return 0;
        }
        Matcher matcher = Pattern.compile("\\b(\\d+)\\b").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private int remainingQuantity(int quantityInbound, int quantityInspected) {
        return Math.max(0, quantityInbound - quantityInspected);
    }

    private int sumQuantities(String text) {
        Matcher matcher = Pattern.compile("\\b(\\d+)\\b").matcher(text);
        int total = 0;
        while (matcher.find()) {
            total += Integer.parseInt(matcher.group(1));
        }
        return total;
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

    private void fillNumberField(By locator, String normalizedLabel, Number value) {
        if (value == null) {
            return;
        }
        if (fillIfEditable(locator, value)) {
            return;
        }
        fillInputNearNormalizedLabel(normalizedLabel, String.valueOf(value));
    }

    private void clearNumberField(By locator, String normalizedLabel) {
        if (clearIfEditable(locator)) {
            return;
        }
        clearInputNearNormalizedLabel(normalizedLabel);
    }

    private boolean fillIfEditable(By locator, Number value) {
        WebElement element = firstEditable(locator);
        if (element == null) {
            return false;
        }
        setInputValue(element, String.valueOf(value));
        return true;
    }

    private void setEditableInputValue(By locator, String value) {
        WebElement element = firstEditable(locator);
        if (element != null) {
            setInputValue(element, value);
        }
    }

    private boolean clearIfEditable(By locator) {
        WebElement element = firstEditable(locator);
        if (element == null) {
            return false;
        }
        setInputValue(element, "");
        return true;
    }

    private boolean fillInputNearNormalizedLabel(String normalizedLabel, String value) {
        for (WebElement input : all(By.cssSelector("input"))) {
            if (!isEditable(input)) {
                continue;
            }
            String label = normalized(attribute(input, "placeholder") + " " + attribute(input, "name"));
            if (label.contains(normalizedLabel)) {
                setInputValue(input, value);
                return true;
            }
        }
        return false;
    }

    private boolean clearInputNearNormalizedLabel(String normalizedLabel) {
        for (WebElement input : all(By.cssSelector("input"))) {
            if (!isEditable(input)) {
                continue;
            }
            String label = normalized(attribute(input, "placeholder") + " " + attribute(input, "name"));
            if (label.contains(normalizedLabel)) {
                setInputValue(input, "");
                return true;
            }
        }
        return false;
    }

    private Number valueOrDefault(Number value, String key) {
        if (value != null && value.doubleValue() > 0) {
            return value;
        }
        String configured = ConfigReader.get(key);
        if (configured == null || configured.isBlank()) {
            return value;
        }
        return Double.parseDouble(configured.trim());
    }

    private Number valueOrDefault(Number redlineValue, Number skuValue, String key) {
        if (redlineValue != null && redlineValue.doubleValue() > 0) {
            return redlineValue;
        }
        if (skuValue != null && skuValue.doubleValue() > 0) {
            return skuValue;
        }
        String configured = ConfigReader.get(key);
        if (configured == null || configured.isBlank()) {
            return skuValue;
        }
        return Double.parseDouble(configured.trim());
    }

    private SystemDimensions systemDimensionsFromRedline() {
        String bodyText = driver.findElement(By.tagName("body")).getText();
        Number weight = numberFromPattern(bodyText, "Khối lượng hệ thống.*?([0-9]+(?:\\.[0-9]+)?)\\s*g");
        Number length = numberFromPattern(bodyText, "Kích thước hệ thống.*?([0-9]+(?:\\.[0-9]+)?)\\s*cm");
        Number width = numberFromPattern(bodyText, "Kích thước hệ thống.*?[xX]\\s*([0-9]+(?:\\.[0-9]+)?)\\s*cm");
        Number height = numberFromPattern(bodyText,
                "Kích thước hệ thống.*?[xX].*?[xX]\\s*([0-9]+(?:\\.[0-9]+)?)\\s*cm");
        return new SystemDimensions(weight, length, width, height);
    }

    private Number numberFromPattern(String text, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return Double.parseDouble(matcher.group(1));
    }

    private WebElement firstEditable(By locator) {
        for (WebElement element : all(locator)) {
            if (isEditable(element)) {
                return element;
            }
        }
        return null;
    }

    private void scanCode(By locator, String value) {
        WebElement input = wait.until(driver -> {
            WebElement element = firstVisibleEnabled(locator);
            return element == null ? null : element;
        });
        scanCode(input, value);
    }

    private boolean scanCodeIfPresent(By locator, String value) {
        WebElement input = firstVisibleEnabled(locator);
        if (input == null) {
            return false;
        }
        scanCode(input, value);
        return true;
    }

    private void scanCode(WebElement input, String value) {
        input.clear();
        input.sendKeys(value);
        input.sendKeys(org.openqa.selenium.Keys.ENTER);
    }

    private WebElement firstVisibleEnabled(By locator) {
        for (WebElement element : all(locator)) {
            if (isDisplayed(element) && element.isEnabled()) {
                return element;
            }
        }
        return null;
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

    public static final class SystemDimensions {
        private final Number weight;
        private final Number length;
        private final Number width;
        private final Number height;

        public SystemDimensions(Number weight, Number length, Number width, Number height) {
            this.weight = weight;
            this.length = length;
            this.width = width;
            this.height = height;
        }

        public boolean hasAnyValue() {
            return weight != null || length != null || width != null || height != null;
        }

        public Number weight() {
            return weight;
        }

        public Number length() {
            return length;
        }

        public Number width() {
            return width;
        }

        public Number height() {
            return height;
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

    private String attribute(WebElement element, String name) {
        try {
            return element.getDomAttribute(name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && search != null && value.toLowerCase().contains(search.toLowerCase());
    }

    private boolean isEditable(WebElement input) {
        try {
            return input.isDisplayed() && input.isEnabled() && input.getDomAttribute("readonly") == null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static final class ScannedInboundProduct {
        private final String boxCode;
        private final String sku;
        private final int quantityTotalInbound;
        private final int quantityInbound;

        public ScannedInboundProduct(String boxCode, String sku, int quantityTotalInbound, int quantityInbound) {
            this.boxCode = boxCode;
            this.sku = sku;
            this.quantityTotalInbound = quantityTotalInbound;
            this.quantityInbound = quantityInbound;
        }

        public String boxCode() {
            return boxCode;
        }

        public String sku() {
            return sku;
        }

        public int quantityTotalInbound() {
            return quantityTotalInbound;
        }

        public int quantityInbound() {
            return quantityInbound;
        }
    }
}

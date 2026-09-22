package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.models.POSku;
import com.example.seleniumtestng.config.ConfigReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class InboundProductWmsPage extends BasePage {
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
    private final By barcodeField = By.xpath("//input[@name='manufacturer_barcode' or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'barcode')]");
    private final By viewBarcodeBtn = By.xpath("//input[@name='manufacturer_barcode' or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'barcode')]"
            + "/following-sibling::*[contains(@class,'input-group-text')]//*[contains(normalize-space(.),'Xem') or contains(@class,'cursor-pointer')][1]");
    private final By productBarcodeModal = By.xpath("//*[(@role='dialog' or contains(@class,'modal-content') or contains(@class,'modal-dialog'))"
            + " and .//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'barcode')]]");
    private final By productBarcodeRows = By.xpath("//*[(@role='dialog' or contains(@class,'modal-content') or contains(@class,'modal-dialog'))"
            + " and .//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'barcode')]]"
            + "//*[self::tr or self::td or self::div or self::span][normalize-space()]");
    private final By batchLotField = By.cssSelector("input[name='batch_lot_code']");
    private final By manufactureDateFields = By.cssSelector(
            "input[name*='manufact'], input[name*='mfg'], input[name*='production'], "
                    + "input[name*='produce'], input[name*='nsx']");
    private final By manufactureDateByLabel = By.cssSelector("input[name='manufacturing_date']");
    private final By expiryDateFields = By.cssSelector(
            "input[name*='expiry'], input[name*='expire'], input[name*='expired'], "
                    + "input[name*='expiration'], input[name*='exp_date']");
    private final By expiryDateByLabel = By.cssSelector("input[name='expiry_date']");
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
    private final By scanSerialBtn = By.xpath("//button[contains(.,'Quét mã serial') or contains(.,'Quet ma serial')]");
    private final By serialModal = By.xpath("//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]");
    private final By serialInput = By.xpath("//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]//input[contains(@placeholder,'serial') or contains(@placeholder,'Serial')]");
    private final By addSerialBtn = By.xpath("//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]//button[contains(.,'Thêm') or contains(.,'Them')]");
    private final By confirmSerialBtn = By.xpath("//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");
    private final By serialRows = By.xpath("//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]//tbody/tr");
    private final By attachmentModal = By.xpath("//*[.//button[contains(.,'Bỏ qua') or contains(.,'Bo qua')] and (contains(@class,'modal') or @role='dialog')]");
    private final By skipAttachmentBtn = By.xpath("//button[contains(normalize-space(.),'Bỏ qua') or contains(normalize-space(.),'Bo qua')]");
    private final By confirmSkipAttachmentBtn = By.xpath("//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");
    private final By finishInspectionSessionBtn = By.xpath(
            "//button[contains(normalize-space(.),'Hoàn tất phiên kiểm') or contains(normalize-space(.),'Hoan tat phien kiem')]");
    private final By productListTitle = By.xpath(
            "//*[contains(normalize-space(.),'DANH SÁCH SẢN PHẨM') or contains(normalize-space(.),'DANH SACH SAN PHAM')]");
    private final By backButton = By.xpath("//button[contains(normalize-space(.),'Quay l') or contains(normalize-space(.),'Back')]");
    private final By productRows = By.cssSelector("tbody tr");
    private String lastInspectOpenFailure = "";

    public InboundProductWmsPage(WebDriver driver) {
        super(driver);
    }

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
            return shortWait(2500).until(driver -> {
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
            clickConfirmDialogIfPresent();
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

    public void inputBatchLotIfPresent() {
        List<WebElement> fields = all(batchLotField);
        if (!fields.isEmpty()) {
            setInputValue(fields.get(0), String.valueOf(System.currentTimeMillis()));
        }
    }

    public void clearBatchLotIfPresent() {
        List<WebElement> fields = all(batchLotField);
        if (!fields.isEmpty()) {
            setInputValue(fields.get(0), "");
        }
    }

    public void inputSerialsIfPresent(int quantity, String sku) {
        inputSerialsIfPresent(quantity, sku, Math.max(1, quantity));
    }

    public void inputSerialsIfPresent(int quantity, String sku, int serialCount) {
        WebElement button = firstVisibleEnabled(scanSerialBtn);
        if (button == null) {
            return;
        }

        openSerialModal(button);
        for (int index = 1; index <= serialCount; index++) {
            addSerialToOpenModal(serialValue(sku, index), true);
        }
        if (serialCount >= Math.max(1, quantity)) {
            confirmSerialModal();
        } else {
            confirmSerialModalIfPossible();
        }
    }

    public void inputDuplicateSerialsIfPresent(String sku) {
        WebElement button = firstVisibleEnabled(scanSerialBtn);
        if (button == null) {
            return;
        }

        openSerialModal(button);
        String serial = serialValue(sku, 1);
        addSerialToOpenModal(serial, true);
        addSerialToOpenModal(serial, false);
        clickVisibleEnabled(confirmSerialBtn);
        try {
            shortWait(2000).until(ExpectedConditions.invisibilityOfElementLocated(serialModal));
        } catch (RuntimeException ignored) {
        }
    }

    public boolean isSerialModalVisible() {
        return firstVisibleEnabled(serialModal) != null;
    }

    private void openSerialModal(WebElement button) {
        jsClick(button);
        visible(serialModal);
    }

    private void addSerialToOpenModal(String serial, boolean waitForAdd) {
        int rowCountBeforeAdd = visibleCount(serialRows);
        WebElement input = visible(serialInput);
        setInputValue(input, serial);
        click(addSerialBtn);
        if (waitForAdd) {
            waitUntilSerialAdded(serial, rowCountBeforeAdd);
        } else {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void confirmSerialModal() {
        clickVisibleEnabled(confirmSerialBtn);
        shortWait(3000).until(ExpectedConditions.invisibilityOfElementLocated(serialModal));
    }

    private void confirmSerialModalIfPossible() {
        clickVisibleEnabled(confirmSerialBtn);
        try {
            shortWait(2000).until(ExpectedConditions.invisibilityOfElementLocated(serialModal));
        } catch (RuntimeException ignored) {
        }
    }

    public void inputShelfLifeDatesIfPresent() {
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

        boolean manufactureFilled = fillDateIfPresent(manufactureDateByLabel, manufactureDateFields, manufactureDate);
        boolean expiryFilled = fillDateIfPresent(expiryDateByLabel, expiryDateFields, expiryDate);
        if (manufactureFilled && expiryFilled) {
            return;
        }

        LocalDate fallbackManufactureDate = today;
        LocalDate fallbackExpiryDate = fallbackManufactureDate.plusDays(shelfLifeDays - 1L);
        if (minimumExpiryDays != null && fallbackExpiryDate.isBefore(today.plusDays(minimumExpiryDays))) {
            fallbackExpiryDate = today.plusDays(minimumExpiryDays);
        }
        System.out.println("Fallback to nearest valid shelf-life dates: manufacture_date="
                + fallbackManufactureDate
                + ", expiry_date="
                + fallbackExpiryDate);
        fillDateIfPresent(manufactureDateByLabel, manufactureDateFields, fallbackManufactureDate);
        fillDateIfPresent(expiryDateByLabel, expiryDateFields, fallbackExpiryDate);
    }

    public void clearShelfLifeDatesIfPresent() {
        clearDateIfPresent(manufactureDateByLabel, manufactureDateFields);
        clearDateIfPresent(expiryDateByLabel, expiryDateFields);
    }

    public void inputMismatchedShelfLifeDatesIfPresent() {
        int shelfLifeDays = Math.max(1, shelfLifeDays());
        Integer minimumExpiryDays = minimumExpiryDays();
        LocalDate manufactureDate = LocalDate.now();
        LocalDate expiryDate = manufactureDate.plusDays(shelfLifeDays + 5L);
        if (minimumExpiryDays != null && expiryDate.isBefore(LocalDate.now().plusDays(minimumExpiryDays))) {
            expiryDate = LocalDate.now().plusDays(minimumExpiryDays + 5L);
            manufactureDate = expiryDate.minusDays(shelfLifeDays + 5L);
            if (manufactureDate.isAfter(LocalDate.now())) {
                manufactureDate = LocalDate.now();
            }
        }
        fillDateIfPresent(manufactureDateByLabel, manufactureDateFields, manufactureDate);
        fillDateIfPresent(expiryDateByLabel, expiryDateFields, expiryDate);
    }

    public void inputProductDimensions(POSku sku) {
        SystemDimensions redlineDimensions = systemDimensionsFromRedline();
        fillNumberField(lengthField, "chieu dai", valueOrDefault(redlineDimensions.length, sku.goodsD(), "INBOUND_LENGTH"));
        fillNumberField(widthField, "chieu rong", valueOrDefault(redlineDimensions.width, sku.goodsW(), "INBOUND_WIDTH"));
        fillNumberField(heightField, "chieu cao", valueOrDefault(redlineDimensions.height, sku.goodsH(), "INBOUND_HEIGHT"));
        fillNumberField(weightField, "can nang", valueOrDefault(redlineDimensions.weight, sku.goodsWeight(), "INBOUND_WEIGHT"));
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
            fillNumberField(lengthField, "chieu dai", valueOrDefault(redlineDimensions.length, sku.goodsD(), "INBOUND_LENGTH"));
        }
        if ("width".equals(omittedField)) {
            clearNumberField(widthField, "chieu rong");
        } else {
            fillNumberField(widthField, "chieu rong", valueOrDefault(redlineDimensions.width, sku.goodsW(), "INBOUND_WIDTH"));
        }
        if ("height".equals(omittedField)) {
            clearNumberField(heightField, "chieu cao");
        } else {
            fillNumberField(heightField, "chieu cao", valueOrDefault(redlineDimensions.height, sku.goodsH(), "INBOUND_HEIGHT"));
        }
        if ("weight".equals(omittedField)) {
            clearNumberField(weightField, "can nang");
        } else {
            fillNumberField(weightField, "can nang", valueOrDefault(redlineDimensions.weight, sku.goodsWeight(), "INBOUND_WEIGHT"));
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
        WebElement button = firstVisible(confirmInspectBtn);
        if (button == null) {
            button = firstVisible(submitInspectBtn);
        }
        return button != null && !button.isEnabled();
    }

    public void confirmInspect() {
        if (!tryConfirmInspectAndReturnToScanBox(5000)) {
            ensureReturnedAfterInspectSubmit();
        }
    }

    public void confirmInspectWithRedlineRetry(POSku sku) {
        if (tryConfirmInspectAndReturnToScanBox(1200)) {
            return;
        }

        SystemDimensions redlineDimensions = systemDimensionsFromRedline();
        if (!redlineDimensions.hasAnyValue()) {
            Integer shelfLifeDays = shelfLifeDaysFromWarning();
            if (shelfLifeDays != null) {
                inputShelfLifeDatesIfPresent();
                if (!tryConfirmInspectAndReturnToScanBox(5000)) {
                    if (retryInspectSubmitIfFormStillOpen(15000)) {
                        return;
                    }
                    System.out.println("Inspect submit did not return to scan box after shelf-life retry. Buttons="
                            + visibleButtonSummary());
                    ensureReturnedAfterInspectSubmit();
                }
                return;
            }
            if (!waitForScanBoxOrSkipAttachment(5000)) {
                if (retryInspectSubmitIfFormStillOpen(15000)) {
                    return;
                }
                System.out.println("Inspect submit did not return to scan box. Buttons=" + visibleButtonSummary());
                ensureReturnedAfterInspectSubmit();
            }
            return;
        }
        inputShelfLifeDatesIfPresent();
        inputProductDimensions(sku);
        if (!tryConfirmInspectAndReturnToScanBox(5000)) {
            if (retryInspectSubmitIfFormStillOpen(15000)) {
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
        WebElement button = firstVisibleEnabled(confirmInspectBtn);
        if (button == null) {
            button = firstVisibleEnabled(submitInspectBtn);
        }
        if (button == null) {
            System.out.println("Visible buttons before confirm inspect: " + visibleButtonSummary());
            button = shortWait(5000).until(driver -> {
                WebElement candidate = firstVisibleEnabled(confirmInspectBtn);
                if (candidate == null) {
                    candidate = firstVisibleEnabled(submitInspectBtn);
                }
                return candidate == null ? null : candidate;
            });
        }
        jsClick(button);
    }

    private boolean retryInspectSubmitIfFormStillOpen(long millis) {
        if (!isInspectionFormVisibleStrict() || isAttachmentModalVisible()) {
            return false;
        }
        WebElement button = confirmInspectButtonIfPresent();
        if (button == null) {
            System.out.println("Skip inspect submit retry because no enabled submit button is visible. Buttons="
                    + visibleButtonSummary());
            return waitForScanBoxOrSkipAttachment(millis);
        }
        System.out.println("Retry inspect submit because form is still open. Buttons=" + visibleButtonSummary());
        jsClick(button);
        return waitForScanBoxOrSkipAttachment(millis);
    }

    private WebElement confirmInspectButtonIfPresent() {
        WebElement button = firstVisibleEnabled(confirmInspectBtn);
        if (button == null) {
            button = firstVisibleEnabled(submitInspectBtn);
        }
        return button;
    }

    private boolean waitForScanBoxOrSkipAttachment(long millis) {
        try {
            boolean[] clickedBack = {false};
            return shortWait(millis).until(driver -> {
                if (skipAttachmentIfPresent()) {
                    return waitUntilAttachmentModalClosed();
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

    private boolean skipAttachmentIfPresent() {
        if (!isAttachmentModalVisible()) {
            return false;
        }
        WebElement skipButton = firstVisibleEnabled(skipAttachmentBtn);
        if (skipButton == null) {
            scrollAttachmentModalToBottom();
            skipButton = firstVisibleEnabled(skipAttachmentBtn);
        }
        if (skipButton == null) {
            return false;
        }
        jsClick(skipButton);
        confirmSkipAttachmentIfPresent();
        return true;
    }

    private boolean waitUntilAttachmentModalClosed() {
        try {
            shortWait(5000).until(driver -> !isAttachmentModalVisible() ? true : null);
            boolean[] clickedBack = {false};
            return shortWait(5000).until(driver -> {
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
            boolean[] clickedBack = {false};
            shortWait(5000).until(driver -> {
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
        return hasVisibleProductRows() && !isAttachmentModalVisible() && !isInspectionFormVisibleStrict();
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
        if (isAttachmentModalVisible()
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

    private void confirmSkipAttachmentIfPresent() {
        try {
            clickConfirmDialogIfPresent();
        } catch (RuntimeException ignored) {
        }
    }

    private void clickConfirmDialogIfPresent() {
        WebElement confirmButton = shortWait(2500).until(driver -> {
            WebElement button = firstVisibleEnabled(confirmSkipAttachmentBtn);
            return button == null ? null : button;
        });
        jsClick(confirmButton);
    }

    private boolean isAttachmentModalVisible() {
        for (WebElement element : all(attachmentModal)) {
            if (isDisplayed(element)) {
                return true;
            }
        }
        return false;
    }

    private void scrollAttachmentModalToBottom() {
        for (WebElement element : all(attachmentModal)) {
            if (!isDisplayed(element)) {
                continue;
            }
            try {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollTop = arguments[0].scrollHeight;"
                                + "arguments[0].querySelectorAll('*').forEach(function(el) {"
                                + "  if (el.scrollHeight > el.clientHeight) el.scrollTop = el.scrollHeight;"
                                + "});",
                        element);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void clickVisibleInspectMenuItem() {
        if (clickVisibleInspectMenuItemIfPresent()) {
            return;
        }
        throw new IllegalStateException("Visible inspect product button not found");
    }

    private boolean clickVisibleInspectMenuItemIfPresent() {
        try {
            return shortWait(1000).until(driver -> {
                List<WebElement> inspectButtons = all(inspectMenuItem);
                for (WebElement button : inspectButtons) {
                    if (isDisplayed(button)) {
                        jsClick(button);
                        return waitUntilInspectionFormVisible();
                    }
                }
                return false;
            });
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean clickInspectActionInRow(WebElement row) {
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
                lastInspectOpenFailure = "action button refreshed while clicking";
                continue;
            }
            if (waitUntilInspectionFormVisible()) {
                return true;
            }
            if (clickVisibleInspectMenuItemIfPresent()) {
                return true;
            }
            closeOpenMenu();
        }

        lastInspectOpenFailure = "dropdown opened but inspect menu is not available; row_actions="
                + rowActionSummary(row);
        return false;
    }

    private List<WebElement> rowActionCandidates(WebElement row) {
        List<WebElement> priorityButtons = new ArrayList<>();
        List<WebElement> fallbackButtons = new ArrayList<>();
        for (WebElement button : row.findElements(By.cssSelector("button"))) {
            if (!isDisplayed(button) || !button.isEnabled()) {
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
        if ((text != null && text.replaceAll("\\s+", "").contains("..."))
                || containsIgnoreCase(classes, "dropdown")
                || containsIgnoreCase(classes, "more")
                || containsIgnoreCase(classes, "ellipsis")
                || containsIgnoreCase(ariaHasPopup, "menu")
                || ariaExpanded != null) {
            return true;
        }
        return !button.findElements(By.cssSelector(
                "i[class*='ri-more'], i[class*='more'], i[class*='ellipsis'], "
                        + "svg[class*='more'], svg[class*='ellipsis']")).isEmpty();
    }

    private void closeOpenMenu() {
        try {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
        } catch (RuntimeException ignored) {
        }
    }

    private boolean waitUntilInspectionFormVisible() {
        try {
            return shortWait(3000).until(driver -> isInspectionFormVisible() ? true : null);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isInspectionFormVisible() {
        if (isInspectionFormVisibleStrict()) {
            return true;
        }
        return false;
    }

    private boolean isInspectionFormVisibleStrict() {
        if (firstEditable(barcodeField) != null) {
            return true;
        }
        if (isProductListViewVisible()) {
            return false;
        }
        if (firstEditable(goodQtyField) != null && confirmInspectButtonIfPresent() != null) {
            return true;
        }
        return firstEditable(By.xpath("//textarea[contains(@name,'note') or contains(@placeholder,'ghi chú') or contains(@placeholder,'ghi chu')]")) != null
                && firstVisibleEnabled(By.xpath("//button[contains(normalize-space(.),'Kiểm hàng') or contains(normalize-space(.),'Kiem hang')]")) != null;
    }

    private boolean isProductListViewVisible() {
        for (WebElement title : all(productListTitle)) {
            if (isDisplayed(title)) {
                return true;
            }
        }
        try {
            String bodyText = driver.findElement(By.tagName("body")).getText();
            return bodyText.contains("DANH SÁCH SẢN PHẨM")
                    || bodyText.contains("DANH SACH SAN PHAM")
                    || (bodyText.contains("SL đã kiểm") && bodyText.contains("Hành động"));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String rowActionSummary(WebElement row) {
        List<String> summaries = new ArrayList<>();
        try {
            for (WebElement button : row.findElements(By.cssSelector("button"))) {
                String text = safeText(button);
                summaries.add("{text='" + (text == null ? "" : text.replaceAll("\\s+", " ").trim())
                        + "', class='" + attribute(button, "class")
                        + "', aria-haspopup='" + attribute(button, "aria-haspopup")
                        + "', aria-expanded='" + attribute(button, "aria-expanded")
                        + "', enabled=" + button.isEnabled() + "}");
            }
        } catch (RuntimeException e) {
            summaries.add("<row refreshed>");
        }
        return summaries.toString();
    }

    public boolean hasActionableProduct(String boxCode, String sku) {
        WebElement row = findProductRow(boxCode, sku);
        if (row == null) {
            return false;
        }
        for (WebElement button : row.findElements(By.cssSelector("button"))) {
            if (isDisplayed(button) && button.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    private WebElement productRow(String boxCode, String sku) {
        return wait.until(driver -> {
            return findProductRow(boxCode, sku);
        });
    }

    private WebElement findProductRow(String boxCode, String sku) {
        for (WebElement row : productRowsForBox(boxCode)) {
            String rowText = safeText(row);
            if (rowText != null && normalized(rowText).contains(normalized(sku))) {
                return row;
            }
        }
        for (WebElement row : all(productRows)) {
            String rowText = safeText(row);
            if (rowText != null && isDisplayed(row) && normalized(rowText).contains(normalized(sku))) {
                return row;
            }
        }
        return null;
    }

    private boolean hasProductRowForBox(String boxCode) {
        for (WebElement row : all(productRows)) {
            String rowText = safeText(row);
            if (rowText != null && isDisplayed(row) && rowText.contains(boxCode)) {
                return true;
            }
        }
        return hasScannedBoxContext(boxCode) && hasVisibleProductRows();
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
            if (rowBelongsToScannedBox(row, boxCode)) {
                rows.add(row);
            }
        }
        return rows;
    }

    private boolean rowBelongsToScannedBox(WebElement row, String boxCode) {
        String rowText = safeText(row);
        if (rowText == null || !isDisplayed(row)) {
            return false;
        }
        return rowText.contains(boxCode) || hasScannedBoxContext(boxCode);
    }

    private boolean hasScannedBoxContext(String boxCode) {
        try {
            return driver.findElement(By.tagName("body")).getText().contains(boxCode);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private ScannedInboundProduct scannedProductFromRow(WebElement row, String boxCode) {
        try {
            List<WebElement> cells = row.findElements(By.cssSelector("td"));
            if (cells.size() < 4) {
                return null;
            }
            if (cells.get(1).getText().contains(boxCode)) {
                int quantityInbound = parseQuantity(cells.get(3).getText());
                int quantityInspected = 0;
                for (int index = 4; index < Math.min(cells.size(), 7); index++) {
                    quantityInspected += sumQuantities(cells.get(index).getText());
                }
                return new ScannedInboundProduct(
                        cells.get(1).getText().trim(),
                        lastNonBlankLine(cells.get(2).getText()),
                        quantityInbound,
                        remainingQuantity(quantityInbound, quantityInspected));
            }
            int quantityInbound = parseQuantity(cells.get(2).getText());
            int quantityInspected = sumQuantities(cells.get(3).getText());
            return new ScannedInboundProduct(
                    boxCode,
                    lastNonBlankLine(cells.get(1).getText()),
                    quantityInbound,
                    remainingQuantity(quantityInbound, quantityInspected));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String lastNonBlankLine(String text) {
        String result = "";
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                result = line.trim();
            }
        }
        return result;
    }

    private int parseQuantity(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            throw new IllegalStateException("Unable to parse inbound quantity from: " + text);
        }
        return Integer.parseInt(digits);
    }

    private int remainingQuantity(int quantityInbound, int quantityInspected) {
        return Math.max(0, quantityInbound - quantityInspected);
    }

    private int sumQuantities(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int total = 0;
        Matcher matcher = Pattern.compile("\\d+").matcher(text);
        while (matcher.find()) {
            total += Integer.parseInt(matcher.group());
        }
        return total;
    }

    private String normalized(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private void fillNumberField(By locator, String normalizedLabel, Number value) {
        if (fillIfEditable(locator, value)) {
            return;
        }
        if (value != null && fillInputNearNormalizedLabel(normalizedLabel, String.valueOf(value))) {
            return;
        }
        System.out.println("Numeric field not found for label: " + normalizedLabel);
    }

    private void clearNumberField(By locator, String normalizedLabel) {
        if (clearIfEditable(locator)) {
            return;
        }
        if (clearInputNearNormalizedLabel(normalizedLabel)) {
            return;
        }
        System.out.println("Numeric field not found to clear for label: " + normalizedLabel);
    }

    private boolean fillIfEditable(By locator, Number value) {
        if (value == null) {
            return false;
        }
        for (WebElement input : all(locator)) {
            if (isEditable(input)) {
                setInputValue(input, String.valueOf(value));
                return true;
            }
        }
        return false;
    }

    private void setEditableInputValue(By locator, String value) {
        WebElement input = shortWait(5000).until(driver -> {
            WebElement candidate = firstEditable(locator);
            return candidate == null ? null : candidate;
        });
        setInputValue(input, value);
    }

    private boolean clearIfEditable(By locator) {
        for (WebElement input : all(locator)) {
            if (isEditable(input)) {
                setInputValue(input, "");
                return true;
            }
        }
        return false;
    }

    private boolean fillInputNearNormalizedLabel(String normalizedLabel, String value) {
        Object filled = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const target = arguments[0];"
                        + "const value = String(arguments[1]);"
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
                        + "const labels = Array.from(document.querySelectorAll('label,div,span,p'))"
                        + "  .filter(visible)"
                        + "  .filter(el => norm(el.innerText || el.textContent).includes(target));"
                        + "const inputs = Array.from(document.querySelectorAll('input'))"
                        + "  .filter(visible)"
                        + "  .filter(el => !el.disabled && !el.readOnly);"
                        + "for (const label of labels) {"
                        + "  const lr = label.getBoundingClientRect();"
                        + "  const candidates = inputs.map(input => ({input, rect: input.getBoundingClientRect()}))"
                        + "    .filter(item => item.rect.top >= lr.top - 12 && item.rect.top <= lr.bottom + 90)"
                        + "    .sort((a, b) => Math.abs(a.rect.left - lr.left) + Math.abs(a.rect.top - lr.bottom)"
                        + "      - Math.abs(b.rect.left - lr.left) - Math.abs(b.rect.top - lr.bottom));"
                        + "  if (!candidates.length) continue;"
                        + "  const input = candidates[0].input;"
                        + "  input.scrollIntoView({block:'center', inline:'center'});"
                        + "  input.focus();"
                        + "  input.value = value;"
                        + "  for (const type of ['input', 'change', 'blur']) {"
                        + "    input.dispatchEvent(new Event(type, {bubbles: true}));"
                        + "  }"
                        + "  return true;"
                        + "}"
                        + "return false;",
                normalizedLabel,
                value);
        return Boolean.TRUE.equals(filled);
    }

    private boolean clearInputNearNormalizedLabel(String normalizedLabel) {
        Object cleared = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
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
                        + "const labels = Array.from(document.querySelectorAll('label,div,span,p'))"
                        + "  .filter(visible)"
                        + "  .filter(el => norm(el.innerText || el.textContent).includes(target));"
                        + "const inputs = Array.from(document.querySelectorAll('input'))"
                        + "  .filter(visible)"
                        + "  .filter(el => !el.disabled && !el.readOnly);"
                        + "for (const label of labels) {"
                        + "  const lr = label.getBoundingClientRect();"
                        + "  const candidates = inputs.map(input => ({input, rect: input.getBoundingClientRect()}))"
                        + "    .filter(item => item.rect.top >= lr.top - 12 && item.rect.top <= lr.bottom + 90)"
                        + "    .sort((a, b) => Math.abs(a.rect.left - lr.left) + Math.abs(a.rect.top - lr.bottom)"
                        + "      - Math.abs(b.rect.left - lr.left) - Math.abs(b.rect.top - lr.bottom));"
                        + "  if (!candidates.length) continue;"
                        + "  const input = candidates[0].input;"
                        + "  input.scrollIntoView({block:'center', inline:'center'});"
                        + "  input.focus();"
                        + "  input.value = '';"
                        + "  for (const type of ['input', 'change', 'blur']) {"
                        + "    input.dispatchEvent(new Event(type, {bubbles: true}));"
                        + "  }"
                        + "  return true;"
                        + "}"
                        + "return false;",
                normalizedLabel);
        return Boolean.TRUE.equals(cleared);
    }

    private Number valueOrDefault(Number value, String key) {
        if (value != null) {
            return value;
        }
        return ConfigReader.requiredInt(key);
    }

    private Number valueOrDefault(Number redlineValue, Number skuValue, String key) {
        if (redlineValue != null) {
            return redlineValue;
        }
        return valueOrDefault(skuValue, key);
    }

    private SystemDimensions systemDimensionsFromRedline() {
        String bodyText = driver.findElement(By.tagName("body")).getText();
        return new SystemDimensions(
                numberFromPattern(bodyText, "KL:\\s*(\\d+(?:[\\.,]\\d+)?)\\s*g"),
                numberFromPattern(bodyText, "d:\\s*(\\d+(?:[\\.,]\\d+)?)\\s*cm"),
                numberFromPattern(bodyText, "w:\\s*(\\d+(?:[\\.,]\\d+)?)\\s*cm"),
                numberFromPattern(bodyText, "h:\\s*(\\d+(?:[\\.,]\\d+)?)\\s*cm"));
    }

    private Number numberFromPattern(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String rawValue = matcher.group(1).replace(',', '.');
        double parsed = Double.parseDouble(rawValue);
        if (parsed == Math.rint(parsed)) {
            return (int) parsed;
        }
        return parsed;
    }

    private String toDisplayDate(String isoDate) {
        String[] parts = isoDate.split("-");
        if (parts.length != 3) {
            return isoDate;
        }
        return parts[2] + "-" + parts[1] + "-" + parts[0];
    }

    private boolean fillDateIfPresent(By primaryLocator, By fallbackLocator, LocalDate date) {
        WebElement input = firstVisibleEnabled(primaryLocator);
        if (input == null) {
            input = firstVisibleEnabled(fallbackLocator);
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
            setInputValue(input, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            return !inputValue(input).isBlank();
        }
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        input.sendKeys(displayDate, Keys.TAB);
        if (!inputValue(input).isBlank()) {
            return true;
        }
        setInputValue(input, displayDate);
        return !inputValue(input).isBlank();
    }

    private boolean clearDateIfPresent(By primaryLocator, By fallbackLocator) {
        WebElement input = firstVisibleEnabled(primaryLocator);
        if (input == null) {
            input = firstVisibleEnabled(fallbackLocator);
        }
        if (input == null) {
            return false;
        }
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'});"
                            + "if (arguments[0]._flatpickr) { arguments[0]._flatpickr.clear(); }",
                    input);
        } catch (RuntimeException ignored) {
        }
        setInputValue(input, "");
        return inputValue(input).isBlank();
    }

    private WebElement firstEditable(By locator) {
        for (WebElement input : all(locator)) {
            if (isEditable(input)) {
                return input;
            }
        }
        return null;
    }

    private void scanCode(By locator, String value) {
        if (scanCodeIfPresent(locator, value)) {
            return;
        }
        RuntimeException[] lastError = {null};
        WebElement input = shortWait(5000).until(driver -> {
            for (WebElement candidate : all(locator)) {
                if (!isEditable(candidate)) {
                    continue;
                }
                try {
                    scanCode(candidate, value);
                    return candidate;
                } catch (RuntimeException e) {
                    lastError[0] = e;
                }
            }
            return null;
        });
        if (input == null && lastError[0] != null) {
            throw lastError[0];
        }
    }

    private boolean scanCodeIfPresent(By locator, String value) {
        RuntimeException lastError = null;
        for (WebElement input : all(locator)) {
            if (!isEditable(input)) {
                continue;
            }
            try {
                scanCode(input, value);
                return true;
            } catch (RuntimeException e) {
                lastError = e;
            }
        }
        if (lastError != null) {
            System.out.println("Skipped non-interactable scan input: " + lastError.getClass().getSimpleName());
        }
        return false;
    }

    private void scanCode(WebElement input, String value) {
        try {
            clearAndEnter(input, value);
        } catch (RuntimeException e) {
            setInputValue(input, value);
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "for (const type of ['keydown', 'keypress', 'keyup']) {"
                            + "arguments[0].dispatchEvent(new KeyboardEvent(type, {"
                            + "  key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true"
                            + "}));"
                            + "}",
                    input);
        }
    }

    private WebElement firstVisibleEnabled(By locator) {
        for (WebElement element : all(locator)) {
            if (isDisplayed(element) && element.isEnabled()) {
                return element;
            }
        }
        return null;
    }

    private String serialValue(String sku, int index) {
        return normalized(sku).toUpperCase() + "-SERIAL-" + System.currentTimeMillis() + "-" + index;
    }

    private String firstBarcodeFromProductModal() {
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

    private boolean openProductBarcodeModal() {
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

    private void waitForBarcodeFieldVisible() {
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

    private boolean waitUntilProductBarcodeModalVisible() {
        try {
            shortWait(5000).until(driver -> isProductBarcodeModalActuallyVisible() ? true : null);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isProductBarcodeModalActuallyVisible() {
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

    private WebElement firstVisible(By locator) {
        for (WebElement element : all(locator)) {
            if (isDisplayed(element)) {
                return element;
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
                        + "return modal ? [modal.innerText, modal.textContent, modal.outerHTML].filter(Boolean).join(' ') : '';"
        );
        return value == null ? "" : value.toString();
    }

    private String compactForLog(String value) {
        if (value == null) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 1000 ? compact : compact.substring(0, 1000) + "...";
    }

    private void closeProductBarcodeModal() {
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

    private void waitUntilSerialAdded(String serial, int previousRowCount) {
        shortWait(3000).until(driver -> {
            WebElement input = firstVisibleEnabled(serialInput);
            boolean inputCleared = input != null && inputValue(input).isBlank();
            boolean rowAdded = visibleCount(serialRows) > previousRowCount;
            boolean serialVisible = visibleText(serialModal).contains(serial);
            return inputCleared || rowAdded || serialVisible ? true : null;
        });
    }

    private void clickVisibleEnabled(By locator) {
        WebElement element = shortWait(3000).until(driver -> {
            WebElement candidate = firstVisibleEnabled(locator);
            return candidate == null ? null : candidate;
        });
        jsClick(element);
    }

    private int visibleCount(By locator) {
        int count = 0;
        for (WebElement element : all(locator)) {
            if (isDisplayed(element)) {
                count++;
            }
        }
        return count;
    }

    private String visibleText(By locator) {
        for (WebElement element : all(locator)) {
            if (isDisplayed(element)) {
                return element.getText();
            }
        }
        return "";
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
        String bodyText = driver.findElement(By.tagName("body")).getText();
        Matcher matcher = Pattern.compile("hệ thống \\((\\d+) ngày\\)").matcher(bodyText);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private Integer minimumExpiryDays() {
        String bodyText = driver.findElement(By.tagName("body")).getText();
        Matcher matcher = Pattern.compile("Minimum\\s+(\\d+)\\s+ng\\S*y", Pattern.CASE_INSENSITIVE).matcher(bodyText);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static final class SystemDimensions {
        private final Number weight;
        private final Number length;
        private final Number width;
        private final Number height;

        private SystemDimensions(Number weight, Number length, Number width, Number height) {
            this.weight = weight;
            this.length = length;
            this.width = width;
            this.height = height;
        }

        private boolean hasAnyValue() {
            return weight != null || length != null || width != null || height != null;
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

    private String inputValue(WebElement input) {
        try {
            String value = input.getDomProperty("value");
            return value == null ? "" : value.trim();
        } catch (RuntimeException e) {
            return "";
        }
    }

    public static final class ScannedInboundProduct {
        private final String boxCode;
        private final String sku;
        private final int quantityTotalInbound;
        private final int quantityInbound;

        private ScannedInboundProduct(String boxCode, String sku, int quantityTotalInbound, int quantityInbound) {
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

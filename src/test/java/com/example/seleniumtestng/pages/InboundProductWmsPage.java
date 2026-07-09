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
    private final By scanBoxField = By.xpath("//input[contains(@placeholder,'kiện') or contains(@placeholder,'kien')]");
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
    private final By barcodeField = By.cssSelector("input[name='manufacturer_barcode']");
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
        type(goodQtyField, String.valueOf(quantity));
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
        type(barcodeField, barcode);
    }

    public void inputBatchLotIfPresent() {
        List<WebElement> fields = all(batchLotField);
        if (!fields.isEmpty()) {
            fields.get(0).clear();
            fields.get(0).sendKeys(String.valueOf(System.currentTimeMillis()));
        }
    }

    public void inputSerialsIfPresent(int quantity, String sku) {
        WebElement button = firstVisibleEnabled(scanSerialBtn);
        if (button == null) {
            return;
        }

        jsClick(button);
        visible(serialModal);
        int serialCount = Math.max(1, quantity);
        for (int index = 1; index <= serialCount; index++) {
            String serial = serialValue(sku, index);
            int rowCountBeforeAdd = visibleCount(serialRows);
            WebElement input = visible(serialInput);
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(serial);
            click(addSerialBtn);
            waitUntilSerialAdded(serial, rowCountBeforeAdd);
        }
        clickVisibleEnabled(confirmSerialBtn);
        shortWait(3000).until(ExpectedConditions.invisibilityOfElementLocated(serialModal));
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

    public void inputProductDimensions(POSku sku) {
        SystemDimensions redlineDimensions = systemDimensionsFromRedline();
        fillIfEditable(lengthField, valueOrDefault(redlineDimensions.length, sku.goodsD(), "INBOUND_LENGTH"));
        fillIfEditable(widthField, valueOrDefault(redlineDimensions.width, sku.goodsW(), "INBOUND_WIDTH"));
        fillIfEditable(heightField, valueOrDefault(redlineDimensions.height, sku.goodsH(), "INBOUND_HEIGHT"));
        fillIfEditable(weightField, valueOrDefault(redlineDimensions.weight, sku.goodsWeight(), "INBOUND_WEIGHT"));
    }

    public void inputProductDimensions(Number length, Number width, Number height, Number weight) {
        fillIfEditable(lengthField, length);
        fillIfEditable(widthField, width);
        fillIfEditable(heightField, height);
        fillIfEditable(weightField, weight);
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
        if (firstEditable(goodQtyField) != null || firstEditable(barcodeField) != null) {
            return true;
        }
        return firstEditable(By.xpath("//textarea[contains(@name,'note') or contains(@placeholder,'ghi chú') or contains(@placeholder,'ghi chu')]")) != null
                && firstVisibleEnabled(By.xpath("//button[contains(normalize-space(.),'Kiểm hàng') or contains(normalize-space(.),'Kiem hang')]")) != null;
    }

    private boolean isProductListViewVisible() {
        if (!hasVisibleProductRows()) {
            return false;
        }
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

    private void fillIfEditable(By locator, Number value) {
        if (value == null) {
            return;
        }
        for (WebElement input : all(locator)) {
            if (isEditable(input)) {
                input.clear();
                input.sendKeys(String.valueOf(value));
                return;
            }
        }
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

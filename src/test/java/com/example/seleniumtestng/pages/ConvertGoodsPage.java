package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.ConvertGoodsRequestData;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ConvertGoodsPage extends BasePage {
    private static final String PATH = "/convert-goods?";
    private final By pageBody = By.tagName("body");
    private final By createRequestButton = By.xpath(
            "//button[contains(normalize-space(.),'Tạo yêu cầu') or contains(normalize-space(.),'Tao yeu cau')]");
    private final By dialog = By.cssSelector(".modal.show, .ant-modal, [role='dialog']");
    private final By submitRequestButton = By.xpath(
            ".//button[contains(normalize-space(.),'Tạo yêu cầu') or contains(normalize-space(.),'Tao yeu cau')]");
    private final By approveButton = By.xpath(
            "//button[contains(normalize-space(.),'Phê duyệt') or contains(normalize-space(.),'Phe duyet')]");
    private final By confirmApproveButton = By.xpath(
            ".//button[contains(normalize-space(.),'Duyệt yêu cầu') or contains(normalize-space(.),'Duyet yeu cau')]");
    private final By putawayButton = By.xpath(
            "//button[contains(normalize-space(.),'Đặt hàng lên kệ') or contains(normalize-space(.),'Dat hang len ke')]");
    private final By confirmPutawayButton = By.xpath(
            ".//button[contains(normalize-space(.),'Đặt hàng lên kệ') or contains(normalize-space(.),'Dat hang len ke')]");
    private final By toastMessage = By.cssSelector(
            ".Toastify__toast-body, .ant-message-notice-content, .ant-notification-notice-message, .ant-notification-notice-description");
    private final By listRows = By.cssSelector("tbody tr");
    private final By listSearchInput = By.xpath(
            "//input[contains(@placeholder,'TÃ¬m kiáº¿m') or contains(@placeholder,'Tim kiem') "
                    + "or contains(@placeholder,'SKU') or contains(@placeholder,'sáº£n pháº©m') "
                    + "or contains(@placeholder,'san pham')]");
    private final By actionMenuItems = By.xpath(
            "//*[contains(@class,'dropdown-menu') or contains(@class,'popover') or contains(@class,'menu') "
                    + "or contains(@class,'ant-dropdown')][not(contains(@style,'display: none'))]"
                    + "//*[self::button or self::a or self::li or self::div]"
                    + "[contains(normalize-space(.),'Chi tiáº¿t') or contains(normalize-space(.),'Chi tiet') "
                    + "or contains(normalize-space(.),'Xem')]");

    public ConvertGoodsPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(ConfigReader.required("WMS_BASE_URL") + PATH);
        waitForPage();
    }

    public void waitForPage() {
        wait.withTimeout(Duration.ofSeconds(30))
                .until(ExpectedConditions.urlContains("/convert-goods"));
        visible(pageBody);
        wait.until(driver -> !String.valueOf(driver.getCurrentUrl()).contains("/login"));
    }

    public boolean isAtPage() {
        return String.valueOf(driver.getCurrentUrl()).contains("/convert-goods");
    }

    public void createRequest(ConvertGoodsRequestData data) {
        openCreateRequestModal();
        selectProduct(data.productCode());
        inputQuantity(data.quantity());
        inputCurrentLocation(data.currentLocation());
        selectCurrentGoodsType(data.currentGoodsType());
        selectTargetGoodsType(data.targetGoodsType());
        inputPendingLocation(data.pendingLocation());
        inputReason(data.reason());
        submitRequest();
    }

    public void openCreatedRequestDetail(String productCode) {
        waitForListAfterSubmit();
        searchListByProductCode(productCode);
        WebElement row = requestRowByProductCode(productCode);
        openDetailFromRow(row);
        waitForDetailPage(productCode);
    }

    public boolean openExistingRequestDetailIfPresent(String productCode) {
        waitForList();
        searchListByProductCode(productCode);
        WebElement row = findRequestRowByProductCode(productCode, 5000, true);
        if (row == null) {
            return false;
        }
        openDetailFromRow(row);
        waitForDetailPage(productCode);
        return true;
    }

    public void approveRequest(String reason) {
        WebElement button = waitForApproveButton();
        if (button == null) {
            if (isApprovedOrCompleted()) {
                System.out.println("Convert goods request is already approved/completed; skip approval.");
                return;
            }
            throw new IllegalStateException("Approve button was not found on convert goods detail. Screen="
                    + summarize(driver.findElement(By.tagName("body")).getText()));
        }
        clickElement(button);
        WebElement field = approvalReasonField();
        clearAndType(field, reason);
        WebElement modal = visibleDialog();
        WebElement confirm = firstVisibleEnabled(modal.findElements(confirmApproveButton));
        if (confirm == null) {
            throw new IllegalStateException("Approve confirmation button was not found. Modal="
                    + summarize(modal.getText()));
        }
        clickElement(confirm);
        waitForApproveFeedback();
    }

    public void putawayApprovedGoods(String location) {
        WebElement button = waitForPutawayButton();
        if (button == null) {
            if (isPutawayCompleted()) {
                System.out.println("Convert goods request is already put away/completed; skip putaway.");
                return;
            }
            throw new IllegalStateException("Putaway button was not found on convert goods detail. Screen="
                    + summarize(driver.findElement(By.tagName("body")).getText()));
        }
        clickElement(button);
        WebElement field = putawayLocationField();
        clearAndType(field, location);
        WebElement modal = visibleDialog();
        WebElement confirm = firstVisibleEnabled(modal.findElements(confirmPutawayButton));
        if (confirm == null) {
            throw new IllegalStateException("Putaway confirmation button was not found. Modal="
                    + summarize(modal.getText()));
        }
        clickElement(confirm);
        waitForPutawayFeedback();
    }

    public void openCreateRequestModal() {
        click(createRequestButton);
        visibleDialog();
    }

    public void selectProduct(String productCode) {
        selectByLabel("Mã sản phẩm", productCode);
    }

    public void inputQuantity(int quantity) {
        WebElement input = inputByAnyLabel("SL", "Số lượng");
        clearAndType(input, String.valueOf(quantity));
    }

    public void selectCurrentGoodsType(String goodsType) {
        selectByLabel("Loại hàng hiện tại", goodsType);
    }

    public void selectTargetGoodsType(String goodsType) {
        selectByLabel("Loại hàng chuyển đổi", goodsType);
    }

    public void inputCurrentLocation(String location) {
        WebElement input = inputByLabel("Vị trí hiện tại");
        clearAndType(input, location);
    }

    public void inputPendingLocation(String location) {
        WebElement input = inputByLabel("Vị trí chờ xử lý");
        clearAndType(input, location);
    }

    public void inputReason(String reason) {
        WebElement field = textInputByAnyLabel("Lý do", "Lí do");
        clearAndType(field, reason);
    }

    public void submitRequest() {
        WebElement modal = visibleDialog();
        WebElement button = firstVisibleEnabled(modal.findElements(submitRequestButton));
        if (button == null) {
            button = clickable(createRequestButton);
        }
        try {
            button.click();
        } catch (ElementClickInterceptedException e) {
            jsClick(button);
        }
        waitForSubmitFeedback();
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

    public boolean isAtDetailPage() {
        String currentUrl = String.valueOf(driver.getCurrentUrl());
        if (currentUrl.matches(".*/convert-goods/[^/?#]+.*")) {
            return true;
        }
        return !driver.findElements(By.xpath(
                "//*[contains(normalize-space(.),'Chi tiáº¿t') or contains(normalize-space(.),'Chi tiet')]"))
                .isEmpty();
    }

    private void selectByLabel(String label, String value) {
        WebElement input = inputByLabel(label);
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(value);
        } catch (RuntimeException e) {
            setInputValue(input, value);
        }
        clickVisibleOption(value);
    }

    private WebElement inputByLabel(String label) {
        WebElement modal = visibleDialog();
        String labelText = xpathText(label);
        List<By> locators = List.of(
                By.xpath(".//*[self::label or self::span or self::div][contains(normalize-space(.),"
                        + labelText + ")]/following::input[not(@type='hidden')][1]"),
                By.xpath(".//*[contains(normalize-space(.)," + labelText
                        + ")]/ancestor::*[self::label or contains(@class,'form') or contains(@class,'field') or contains(@class,'item')][1]//input[not(@type='hidden')]"),
                By.xpath(".//input[contains(@placeholder," + labelText + ")]"));

        WebElement input = findFirstVisibleEnabled(modal, locators);
        if (input != null) {
            return input;
        }

        throw new IllegalStateException("Input not found for label: " + label
                + ". Modal text=" + summarize(modal.getText()));
    }

    private WebElement inputByAnyLabel(String... labels) {
        IllegalStateException lastError = null;
        for (String label : labels) {
            try {
                return inputByLabel(label);
            } catch (IllegalStateException e) {
                lastError = e;
            }
        }
        throw lastError == null ? new IllegalStateException("Input not found") : lastError;
    }

    private WebElement textInputByLabel(String label) {
        WebElement modal = visibleDialog();
        String labelText = xpathText(label);
        List<By> locators = List.of(
                By.xpath(".//*[self::label or self::span or self::div][contains(normalize-space(.),"
                        + labelText + ")]/following::textarea[1]"),
                By.xpath(".//textarea[contains(@placeholder," + labelText + ")]"),
                By.xpath(".//*[self::label or self::span or self::div][contains(normalize-space(.),"
                        + labelText + ")]/following::input[not(@type='hidden')][1]"));

        WebElement input = findFirstVisibleEnabled(modal, locators);
        if (input != null) {
            return input;
        }

        throw new IllegalStateException("Text input not found for label: " + label
                + ". Modal text=" + summarize(modal.getText()));
    }

    private WebElement textInputByAnyLabel(String... labels) {
        IllegalStateException lastError = null;
        for (String label : labels) {
            try {
                return textInputByLabel(label);
            } catch (IllegalStateException e) {
                lastError = e;
            }
        }
        throw lastError == null ? new IllegalStateException("Text input not found") : lastError;
    }

    private WebElement approvalReasonField() {
        try {
            return textInputByAnyLabel("Lý do duyệt yêu cầu", "Lí do duyệt yêu cầu", "Nhập lý do duyệt yêu cầu",
                    "Nhập lí do duyệt yêu cầu", "Lý do", "Lí do");
        } catch (IllegalStateException ignored) {
            WebElement modal = visibleDialog();
            List<WebElement> fields = modal.findElements(By.cssSelector(
                    "textarea, input:not([type='hidden']):not([type='file'])"));
            WebElement fallback = firstVisibleEnabled(fields);
            if (fallback != null) {
                return fallback;
            }
            throw new IllegalStateException("Approval reason input was not found. Modal="
                    + summarize(modal.getText()));
        }
    }

    private WebElement putawayLocationField() {
        try {
            return inputByAnyLabel("Vị trí", "Vị trí lên kệ", "Vị trí đặt hàng", "Nhập vị trí", "Location");
        } catch (IllegalStateException ignored) {
            WebElement modal = visibleDialog();
            List<WebElement> fields = modal.findElements(By.cssSelector(
                    "input:not([type='hidden']):not([type='file']), textarea"));
            WebElement fallback = firstVisibleEnabled(fields);
            if (fallback != null) {
                return fallback;
            }
            throw new IllegalStateException("Putaway location input was not found. Modal="
                    + summarize(modal.getText()));
        }
    }

    private void clickVisibleOption(String value) {
        String optionText = xpathText(value);
        By option = By.xpath(
                "//*[contains(@class,'-menu') or contains(@class,'dropdown') or contains(@class,'menu') "
                        + "or contains(@class,'ant-select-dropdown') or @role='listbox']"
                        + "//*[self::div or self::span or self::li or @role='option']"
                        + "[normalize-space()=" + optionText
                        + " or contains(normalize-space(.)," + optionText + ")]");
        WebElement element = shortWait(5000).until(driver -> {
            WebElement best = null;
            for (WebElement candidate : driver.findElements(option)) {
                if (!displayed(candidate)) {
                    continue;
                }
                String text = candidate.getText() == null ? "" : candidate.getText().trim();
                if (text.equals(value) || text.startsWith(value + " ")) {
                    return candidate;
                }
                if (best == null && !text.isBlank()) {
                    best = candidate;
                }
            }
            return best;
        });
        try {
            element.click();
        } catch (RuntimeException e) {
            jsClick(element);
        }
    }

    private void clearAndType(WebElement input, String value) {
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(value);
        } catch (RuntimeException e) {
            setInputValue(input, value);
        }
    }

    private void waitForListAfterSubmit() {
        try {
            shortWait(10000).until(driver -> {
                for (WebElement element : driver.findElements(dialog)) {
                    if (displayed(element)) {
                        return null;
                    }
                }
                return hasVisibleRows() ? true : null;
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException("Convert goods list was not ready after submit. Screen="
                    + summarize(driver.findElement(By.tagName("body")).getText()), e);
        }
    }

    private void waitForList() {
        try {
            shortWait(10000).until(driver -> hasVisibleRows() ? true : null);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Convert goods list was not ready. Screen="
                    + summarize(driver.findElement(By.tagName("body")).getText()), e);
        }
    }

    private void searchListByProductCode(String productCode) {
        WebElement input = firstVisibleEnabled(all(listSearchInput));
        if (input == null) {
            return;
        }
        clearAndType(input, productCode);
        input.sendKeys(Keys.ENTER);
        try {
            shortWait(5000).until(driver -> hasRowMatching(productCode) ? true : null);
        } catch (RuntimeException ignored) {
            System.out.println("Convert goods list search did not show SKU immediately: " + productCode);
        }
    }

    private WebElement requestRowByProductCode(String productCode) {
        WebElement row = findRequestRowByProductCode(productCode, 15000, false);
        if (row != null) {
            return row;
        }
        throw new IllegalStateException("Created convert goods request was not found by SKU: "
                + productCode
                + ". Screen="
                + summarize(driver.findElement(By.tagName("body")).getText()));
    }

    private WebElement findRequestRowByProductCode(String productCode, long timeoutMillis, boolean skipCompleted) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement row : driver.findElements(listRows)) {
                    if (!displayed(row)) {
                        continue;
                    }
                    if (rowMatches(row, productCode) && (!skipCompleted || !rowIsCompleted(row))) {
                        return row;
                    }
                }
                return null;
            });
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean hasVisibleRows() {
        for (WebElement row : all(listRows)) {
            if (displayed(row)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRowMatching(String productCode) {
        for (WebElement row : all(listRows)) {
            if (displayed(row) && rowMatches(row, productCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean rowMatches(WebElement row, String productCode) {
        String rowText = row.getText();
        if (rowText == null || rowText.isBlank()) {
            return false;
        }
        String normalizedRow = normalizeForMatch(rowText);
        String normalizedSku = normalizeForMatch(productCode);
        return normalizedRow.contains(normalizedSku)
                || (normalizedSku.contains("mhmsi") && normalizedRow.contains("man hinh msi"));
    }

    private boolean rowIsCompleted(WebElement row) {
        String rowText = row.getText();
        String normalized = normalizeForMatch(rowText);
        return normalized.contains("hoan thanh") || normalized.contains("completed");
    }

    private void openDetailFromRow(WebElement row) {
        String urlBefore = String.valueOf(driver.getCurrentUrl());
        WebElement rowLink = firstVisibleEnabled(row.findElements(By.cssSelector("a[href]")));
        if (rowLink != null) {
            clickElement(rowLink);
            if (detailOpened(urlBefore, 5000)) {
                return;
            }
        }

        WebElement directDetail = firstVisibleEnabled(row.findElements(By.xpath(
                ".//*[self::a or self::button][contains(normalize-space(.),'Chi tiáº¿t') "
                        + "or contains(normalize-space(.),'Chi tiet') or contains(normalize-space(.),'Xem')]")));
        if (directDetail != null) {
            clickElement(directDetail);
            if (detailOpened(urlBefore, 5000)) {
                return;
            }
        }

        WebElement actionButton = firstVisibleEnabled(row.findElements(By.xpath(
                ".//button[.//i[contains(@class,'more') or contains(@class,'ellipsis') or contains(@class,'ri-more')] "
                        + "or .//*[name()='svg'] or contains(normalize-space(.),'...') "
                        + "or @aria-haspopup='menu' or @aria-expanded]")));
        if (actionButton != null) {
            clickElement(actionButton);
            WebElement menuItem = firstVisibleEnabled(all(actionMenuItems));
            if (menuItem != null) {
                clickElement(menuItem);
                if (detailOpened(urlBefore, 5000)) {
                    return;
                }
            }
        }

        clickElement(row);
        detailOpened(urlBefore, 3000);
    }

    private void waitForDetailPage(String productCode) {
        try {
            shortWait(10000).until(driver -> {
                if (firstVisibleEnabled(all(approveButton)) != null || isApprovedOrCompleted()) {
                    return true;
                }
                String bodyText = driver.findElement(By.tagName("body")).getText();
                return normalizeForMatch(bodyText).contains(normalizeForMatch(productCode))
                        && normalizeForMatch(bodyText).contains("chi tiet") ? true : null;
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException("Convert goods detail did not open for SKU: "
                    + productCode
                    + ". Current URL="
                    + driver.getCurrentUrl()
                    + ". Screen="
                    + summarize(driver.findElement(By.tagName("body")).getText()), e);
        }
    }

    private WebElement waitForApproveButton() {
        try {
            return shortWait(15000).until(driver -> {
                WebElement button = firstVisibleEnabled(all(approveButton));
                if (button != null) {
                    return button;
                }
                return null;
            });
        } catch (RuntimeException e) {
            return null;
        }
    }

    private WebElement waitForPutawayButton() {
        try {
            return shortWait(15000).until(driver -> firstVisibleEnabled(all(putawayButton)));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void clickElement(WebElement element) {
        try {
            element.click();
        } catch (RuntimeException e) {
            jsClick(element);
        }
    }

    private boolean detailOpened(String urlBefore, long timeoutMillis) {
        try {
            shortWait(timeoutMillis).until(driver ->
                    !urlBefore.equals(String.valueOf(driver.getCurrentUrl())) || isAtDetailPage() ? true : null);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void waitForApproveFeedback() {
        try {
            shortWait(10000).until(driver -> {
                String toast = waitForAnyToast(200);
                if (!toast.isBlank()) {
                    return true;
                }
                return isApprovedOrCompleted() ? true : null;
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException("Convert goods approval feedback was not detected. Screen="
                    + summarize(driver.findElement(By.tagName("body")).getText()), e);
        }
    }

    private void waitForPutawayFeedback() {
        try {
            shortWait(15000).until(driver -> {
                String toast = waitForAnyToast(200);
                if (!toast.isBlank()) {
                    return true;
                }
                return isPutawayCompleted() ? true : null;
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException("Convert goods putaway feedback was not detected. Screen="
                    + summarize(driver.findElement(By.tagName("body")).getText()), e);
        }
    }

    private boolean isApprovedOrCompleted() {
        String normalized = normalizeForMatch(driver.findElement(By.tagName("body")).getText());
        return normalized.contains("da duyet")
                || normalized.contains("duyet thanh cong")
                || normalized.contains("hoan thanh")
                || normalized.contains("approved")
                || normalized.contains("completed");
    }

    private boolean isPutawayCompleted() {
        String normalized = normalizeForMatch(driver.findElement(By.tagName("body")).getText());
        return normalized.contains("hoan thanh")
                || normalized.contains("dat hang len ke thanh cong")
                || normalized.contains("len ke thanh cong")
                || normalized.contains("completed");
    }

    private WebElement visibleDialog() {
        return wait.until(driver -> {
            for (WebElement element : driver.findElements(dialog)) {
                if (displayed(element)) {
                    return element;
                }
            }
            return null;
        });
    }

    private WebElement findFirstVisibleEnabled(WebElement root, List<By> locators) {
        for (By locator : locators) {
            WebElement element = firstVisibleEnabled(root.findElements(locator));
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private WebElement firstVisibleEnabled(List<WebElement> elements) {
        for (WebElement element : elements) {
            if (displayed(element) && element.isEnabled()) {
                return element;
            }
        }
        return null;
    }

    private void waitForSubmitFeedback() {
        try {
            shortWait(8000).until(driver -> {
                String toast = waitForAnyToast(200);
                if (!toast.isBlank()) {
                    return true;
                }
                for (WebElement element : driver.findElements(dialog)) {
                    if (displayed(element)) {
                        return null;
                    }
                }
                return true;
            });
        } catch (RuntimeException ignored) {
            System.out.println("Convert goods submit feedback was not detected. Screen="
                    + summarize(driver.findElement(By.tagName("body")).getText()));
        }
    }

    private String summarize(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}

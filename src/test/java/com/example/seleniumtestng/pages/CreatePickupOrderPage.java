package com.example.seleniumtestng.pages;

import java.util.List;
import java.util.StringJoiner;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class CreatePickupOrderPage extends BasePage {
    private final By pickUpTypeField = By.xpath("//div[normalize-space()='Chọn loại bảng kê' or normalize-space()='Chon loai bang ke']");
    private final By pickUpStrategyField = By.xpath("//div[normalize-space()='Chọn loại chiến lược' or normalize-space()='Chon loai chien luoc']");
    private final By chooseCustomerField = By.xpath("//div[contains(.,'Chọn khách hàng') or contains(.,'Chon khach hang')]/ancestor::div[contains(@class,'-control')]");
    private final By chooseCustomerInput = By.xpath("//div[contains(.,'Chọn khách hàng') or contains(.,'Chon khach hang')]/following::input[1]");
    private final By customizeBtn = By.xpath("//button[normalize-space()='Tuỳ chỉnh' or normalize-space()='Tùy chỉnh' or normalize-space()='Tuy chinh']");
    private final By inputOrderField = By.xpath("//input[contains(@placeholder,'mã đơn hàng') or contains(@placeholder,'ma don hang')]");
    private final By customizeModalTitle = By.xpath("//*[contains(normalize-space(.),'Tuỳ chỉnh điều kiện tạo bảng kê') or contains(normalize-space(.),'Tùy chỉnh điều kiện tạo bảng kê') or contains(normalize-space(.),'Tuy chinh dieu kien tao bang ke')]");
    private final By customizeModalConfirmBtn = By.xpath("//*[contains(normalize-space(.),'Tuỳ chỉnh điều kiện tạo bảng kê') or contains(normalize-space(.),'Tùy chỉnh điều kiện tạo bảng kê') or contains(normalize-space(.),'Tuy chinh dieu kien tao bang ke')]/following::button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan'][1]");
    private final By orderConditionDropdown = By.xpath("//*[normalize-space()='Mã đơn hàng' or normalize-space()='Ma don hang']");
    private final By orderListConditionOption = By.xpath("//*[normalize-space()='DS mã đơn hàng' or normalize-space()='DS ma don hang']");
    private final By inputOrderListBtn = By.xpath("//button[normalize-space()='Nhập mã đơn' or normalize-space()='Nhap ma don']");
    private final By orderListModalTitle = By.xpath("//*[normalize-space()='Nhập danh sách đơn hàng' or normalize-space()='Nhap danh sach don hang']");
    private final By orderListTextarea = By.xpath("//textarea[contains(@placeholder,'Nhập danh sách mã đơn hàng') or contains(@placeholder,'Nhap danh sach ma don hang')]");
    private final By orderListConfirmBtn = By.xpath("//*[normalize-space()='Nhập danh sách đơn hàng' or normalize-space()='Nhap danh sach don hang']/following::button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan'][1]");
    private final By confirmAddOrderBtn = By.xpath("//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");
    private final By createPickUpBtn = By.xpath("//button[normalize-space()='Tạo bảng kê' or normalize-space()='Tao bang ke']");
    private final By notificationSuccess = By.xpath("//div[contains(@class,'Toastify__toast-body')]//div[normalize-space()='Tạo bảng kê thành công !' or contains(.,'Tạo bảng kê thành công') or contains(.,'Tao bang ke thanh cong')]");

    public CreatePickupOrderPage(WebDriver driver) {
        super(driver);
    }

    public void selectPickUpType(String typeName) {
        click(pickUpTypeField);
        jsClick(visible(exactOption(typeName)));
    }

    public void selectPickUpStrategy(String strategyName) {
        click(pickUpStrategyField);
        jsClick(visible(exactOption(strategyName)));
    }

    public void selectCustomerWms(String customerName) {
        click(chooseCustomerField);
        WebElement input = visible(chooseCustomerInput);
        input.clear();
        input.sendKeys(customerName);
        visible(optionContains(customerName));
        input.sendKeys(Keys.ENTER);
    }

    public void addOrderCustomize(String trackingNumber) {
        addTrackingNumber(trackingNumber);
        WebElement createButton = clickable(createPickUpBtn);
        jsClick(createButton);
    }

    public void addOrdersCustomize(List<String> trackingNumbers) {
        jsClick(clickable(customizeBtn));
        selectOrderListConditionIfNeeded();
        jsClick(clickable(inputOrderListBtn));
        visible(orderListModalTitle);
        typeOrderListTextarea(commaSeparated(trackingNumbers));
        clickOrderListConfirm();
        jsClick(clickable(customizeModalConfirmBtn));
        WebElement createButton = clickable(createPickUpBtn);
        jsClick(createButton);
    }

    private void addTrackingNumber(String trackingNumber) {
        jsClick(clickable(customizeBtn));
        type(inputOrderField, trackingNumber);
        jsClick(clickable(confirmAddOrderBtn));
        clickable(createPickUpBtn);
    }

    public void verifyPickUpOrderCreated() {
        visible(notificationSuccess);
    }

    private By exactOption(String value) {
        return By.xpath("//*[contains(@class,'-menu')]//*[normalize-space(.)='" + value + "']");
    }

    private By optionContains(String value) {
        return By.xpath("//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'" + value + "')]");
    }

    private void selectOrderListConditionIfNeeded() {
        if (isVisible(inputOrderListBtn, 1000)) {
            return;
        }
        visible(customizeModalTitle);
        WebElement dropdown = visible(orderConditionDropdown);
        clickDropdown(dropdown);
        WebElement option = visible(orderListConditionOption);
        jsClick(option);
        visible(inputOrderListBtn);
    }

    private String commaSeparated(List<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                joiner.add(value.trim());
            }
        }
        return joiner.toString();
    }

    private void typeOrderListTextarea(String orderCodes) {
        WebElement textarea = visible(orderListTextarea);
        textarea.click();
        textarea.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        textarea.sendKeys(orderCodes);

        String actualValue = textarea.getDomProperty("value");
        if (!orderCodes.equals(actualValue)) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];"
                            + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                            + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    textarea,
                    orderCodes);
            actualValue = textarea.getDomProperty("value");
        }

        if (!orderCodes.equals(actualValue)) {
            throw new IllegalStateException("Textarea danh sách đơn hàng nhập sai. Expected: "
                    + orderCodes + " | Actual: " + actualValue);
        }
        System.out.println("Order codes typed into WMS pickup modal: " + actualValue);
    }

    private void clickOrderListConfirm() {
        WebElement confirmButton = clickable(orderListConfirmBtn);
        try {
            confirmButton.click();
        } catch (RuntimeException e) {
            jsClick(confirmButton);
        }
        wait.until(driver -> {
            for (WebElement element : driver.findElements(orderListTextarea)) {
                try {
                    if (element.isDisplayed()) {
                        return false;
                    }
                } catch (RuntimeException ignored) {
                }
            }
            return true;
        });
        visible(inputOrderListBtn);
    }

    private boolean isVisible(By locator, long timeoutMillis) {
        try {
            return findVisible(locator, timeoutMillis) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private WebElement findVisible(By locator, long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(locator)) {
                    if (element.isDisplayed()) {
                        return element;
                    }
                }
                return null;
            });
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void clickDropdown(WebElement dropdown) {
        try {
            new Actions(driver).moveToElement(dropdown, 70, 0).click().perform();
        } catch (RuntimeException e) {
            jsClick(dropdown);
        }
    }
}

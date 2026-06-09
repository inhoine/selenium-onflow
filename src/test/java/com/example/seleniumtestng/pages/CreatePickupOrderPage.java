package com.example.seleniumtestng.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CreatePickupOrderPage extends BasePage {
    private final By pickUpTypeField = By.xpath("//div[normalize-space()='Chọn loại bảng kê' or normalize-space()='Chon loai bang ke']");
    private final By pickUpStrategyField = By.xpath("//div[normalize-space()='Chọn loại chiến lược' or normalize-space()='Chon loai chien luoc']");
    private final By chooseCustomerField = By.xpath("//div[contains(.,'Chọn khách hàng') or contains(.,'Chon khach hang')]/ancestor::div[contains(@class,'-control')]");
    private final By chooseCustomerInput = By.xpath("//div[contains(.,'Chọn khách hàng') or contains(.,'Chon khach hang')]/following::input[1]");
    private final By customizeBtn = By.xpath("//button[normalize-space()='Tuỳ chỉnh' or normalize-space()='Tùy chỉnh' or normalize-space()='Tuy chinh']");
    private final By inputOrderField = By.xpath("//input[contains(@placeholder,'mã đơn hàng') or contains(@placeholder,'ma don hang')]");
    private final By confirmAddOrderBtn = By.xpath("//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");
    private final By createPickUpBtn = By.xpath("//button[normalize-space()='Tạo bảng kê' or normalize-space()='Tao bang ke']");
    private final By notificationSuccess = By.xpath("//div[contains(@class,'Toastify__toast-body')]//div[contains(.,'Tạo bảng kê thành công') or contains(.,'Tao bang ke thanh cong')]");

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
        jsClick(clickable(customizeBtn));
        type(inputOrderField, trackingNumber);
        jsClick(clickable(confirmAddOrderBtn));
        WebElement createButton = clickable(createPickUpBtn);
        jsClick(createButton);
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
}

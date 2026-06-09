package com.example.seleniumtestng.pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class CreateOrderOmsPage extends BasePage {
    private final By createOrderButton = By.xpath("(//button[contains(normalize-space(.),'Tạo đơn hàng') or contains(normalize-space(.),'Tao don hang')])[1]");
    private final By createOrderMenu = By.xpath("//button[@role='menuitem' and (@title='Tạo đơn hàng' or @title='Tao don hang')]");
    private final By customerField = By.xpath("//div[contains(.,'Chọn khách hàng') or contains(.,'Chon khach hang')]/ancestor::div[contains(@class,'-control')]");
    private final By customerInput = By.xpath("//div[contains(.,'Chọn khách hàng') or contains(.,'Chon khach hang')]/following::input[1]");
    private final By saleStoreField = By.xpath("//div[contains(.,'Chọn kênh bán hàng') or contains(.,'Chon kenh ban hang')]/ancestor::div[contains(@class,'-control')]");
    private final By saleStoreInput = By.xpath("//div[contains(.,'Chọn kênh bán hàng') or contains(.,'Chon kenh ban hang')]/following::input[1]");
    private final By choosePickupField = By.xpath("//div[contains(.,'Chọn địa chỉ lấy hàng') or contains(.,'Chon dia chi lay hang')]/ancestor::div[contains(@class,'-control')]");
    private final By choosePickupInput = By.xpath("//div[contains(.,'Chọn địa chỉ lấy hàng') or contains(.,'Chon dia chi lay hang')]/following::input[1]");
    private final By addNewProductBtn = By.xpath("//button[normalize-space()='Thêm sản phẩm' or normalize-space()='Them san pham']");
    private final By productDropdowns = By.xpath("//div[contains(.,'Chọn sản phẩm') or contains(.,'Chon san pham')]/ancestor::div[contains(@class,'-control')]");
    private final By productQty = By.xpath("//input[contains(@placeholder,'số lượng') or contains(@placeholder,'so luong')]");
    private final By orderNumberField = By.xpath("//input[contains(@placeholder,'mã đơn hàng') or contains(@placeholder,'ma don hang')]");
    private final By continueBtn = By.xpath("//button[normalize-space()='Tiếp theo' or normalize-space()='Tiep theo']");
    private final By createBtn = By.xpath("//button[contains(@class,'btn-success') and (contains(normalize-space(.),'Tạo đơn') or contains(normalize-space(.),'Tao don'))]");
    private final By confirmAndCreateBtn = By.xpath("//button[normalize-space()='Tạo và xử lý đơn hàng' or normalize-space()='Tao va xu ly don hang']");

    public CreateOrderOmsPage(WebDriver driver) {
        super(driver);
    }

    public void accessCreateOrder() {
        click(createOrderButton);
        click(createOrderMenu);
    }

    public void selectCustomerOms(String customerName) {
        selectAutocompleteOption(customerField, customerInput, customerName);
    }

    public void selectSaleStore(String saleStoreName) {
        selectAutocompleteOption(saleStoreField, saleStoreInput, saleStoreName);
    }

    public void selectChoosePickup(String pickupCode) {
        selectAutocompleteOption(choosePickupField, choosePickupInput, pickupCode);
    }

    public void addProductToCreateOrder(String productKeyword, int quantity) {
        WebElement dropdown = last(productDropdowns);
        jsClick(dropdown);
        WebElement input = dropdown.findElement(By.cssSelector("input"));
        input.clear();
        input.sendKeys(productKeyword);
        WebElement option = visible(optionContains(productKeyword));
        option.click();

        List<WebElement> quantityInputs = all(productQty);
        WebElement quantityInput = quantityInputs.get(quantityInputs.size() - 1);
        quantityInput.clear();
        quantityInput.sendKeys(String.valueOf(quantity));
    }

    public void addNewProductRow() {
        int before = all(productDropdowns).size();
        click(addNewProductBtn);
        wait.until(driver -> all(productDropdowns).size() > before);
    }

    public String inputOrderNumber() {
        String orderNumber = "ORD-" + System.currentTimeMillis();
        type(orderNumberField, orderNumber);
        return orderNumber;
    }

    public void confirmCreateOrder() {
        click(continueBtn);
        wait.until(ExpectedConditions.elementToBeClickable(createBtn));
        WebElement confirm = clickable(createBtn);
        jsClick(confirm);
        click(confirmAndCreateBtn);
    }

    public void verifyCreatedOrder(String orderNumber) {
        visible(By.xpath("//a[contains(@id,'partner_tracking_code') and normalize-space()='" + orderNumber + "']"));
    }

    public String getTrackingCodeByOrderNumber(String orderNumber) {
        WebElement trackingCode = visible(By.xpath("//a[normalize-space()='" + orderNumber + "']/ancestor::div[contains(@class,'flex-column')]//p[contains(@id,'tracking_code')]"));
        return trackingCode.getText().trim();
    }

    private void selectAutocompleteOption(By fieldLocator, By inputLocator, String value) {
        click(fieldLocator);
        WebElement input = visible(inputLocator);
        input.clear();
        input.sendKeys(value);
        visible(optionContains(value));
        input.sendKeys(Keys.ENTER);
    }

    private By optionContains(String value) {
        return By.xpath("//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'" + value + "')]");
    }

    private WebElement last(By locator) {
        return wait.until(driver -> {
            List<WebElement> elements = all(locator);
            return elements.isEmpty() ? null : elements.get(elements.size() - 1);
        });
    }
}

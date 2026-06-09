package com.example.seleniumtestng.pages;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CreateInboundProductPage extends BasePage {
    private final By dropdownInboundBtn = By.xpath("//button[contains(.,'Tạo phiếu') or contains(.,'Tao phieu')]");
    private final By createInboundMenuItem = By.xpath("//button[contains(.,'Tạo phiếu nhập') or contains(.,'Tao phieu nhap')]");
    private final By warehouseField = By.xpath("//div[contains(.,'Chọn địa chỉ lấy hàng') or contains(.,'Chon dia chi lay hang')]/ancestor::div[contains(@class,'-control')]");
    private final By warehouseInput = By.xpath("//div[contains(.,'Chọn địa chỉ lấy hàng') or contains(.,'Chon dia chi lay hang')]/following::input[1]");
    private final By supplierField = By.xpath("//div[contains(.,'Chọn nhà cung cấp') or contains(.,'Chon nha cung cap')]/ancestor::div[contains(@class,'-control')]");
    private final By supplierInput = By.xpath("//div[contains(.,'Chọn nhà cung cấp') or contains(.,'Chon nha cung cap')]/following::input[1]");
    private final By referenceField = By.cssSelector("input[name='shipmentReferenceCode']");
    private final By addProductBtn = By.xpath("//*[normalize-space()='Thêm sản phẩm' or normalize-space()='Them san pham']");
    private final By addNewProductBtn = By.xpath("//button[normalize-space()='Thêm sản phẩm mới' or normalize-space()='Them san pham moi']");
    private final By productDropdowns = By.xpath("//div[contains(.,'Chọn sản phẩm') or contains(.,'Chon san pham')]/ancestor::div[contains(@class,'-control')]");
    private final By confirmItemInboundBtn = By.xpath("//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");
    private final By lengthField = By.xpath("//input[contains(@placeholder,'Dài') or contains(@placeholder,'Dai')]");
    private final By widthField = By.xpath("//input[contains(@placeholder,'Rộng') or contains(@placeholder,'Rong')]");
    private final By heightField = By.xpath("//input[contains(@placeholder,'Cao')]");
    private final By createInboundBtn = By.xpath("//button[normalize-space()='Tạo mới' or normalize-space()='Tao moi']");
    private final By confirmInboundBtn = By.xpath("//button[normalize-space()='Tạo và duyệt phiếu nhập' or normalize-space()='Tao va duyet phieu nhap']");
    private final By inboundCodeText = By.xpath("//h5[contains(.,'Mã nhập kho') or contains(.,'Ma nhap kho')]");

    public CreateInboundProductPage(WebDriver driver) {
        super(driver);
    }

    public void openCreateInboundForm() {
        click(dropdownInboundBtn);
        click(createInboundMenuItem);
    }

    public void selectWarehouse(String keyword) {
        selectReactOption(warehouseField, warehouseInput, keyword);
    }

    public void selectSupplier(String keyword) {
        selectReactOption(supplierField, supplierInput, keyword);
    }

    public String inputReference() {
        String reference = "REF-" + String.valueOf(System.currentTimeMillis()).substring(7);
        type(referenceField, reference);
        return reference;
    }

    public void clickAddProduct() {
        click(addProductBtn);
    }

    public void addProductToInbound(String productKeyword, int quantity) {
        WebElement dropdown = last(productDropdowns);
        jsClick(dropdown);
        WebElement input = dropdown.findElement(By.cssSelector("input"));
        input.sendKeys(productKeyword);
        visible(By.xpath("//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'" + productKeyword + "')]"));
        input.sendKeys(Keys.ENTER);

        List<WebElement> qtyInputs = all(By.xpath("//input[contains(@name,'productQty')]"));
        WebElement qtyInput = qtyInputs.get(qtyInputs.size() - 1);
        qtyInput.clear();
        qtyInput.sendKeys(String.valueOf(quantity));
    }

    public void addNewProductRow() {
        int before = all(productDropdowns).size();
        click(addNewProductBtn);
        wait.until(driver -> all(productDropdowns).size() > before);
    }

    public void confirmItems() {
        click(confirmItemInboundBtn);
    }

    public void inputProductDimensions(int length, int width, int height) {
        type(lengthField, String.valueOf(length));
        type(widthField, String.valueOf(width));
        type(heightField, String.valueOf(height));
    }

    public void confirmCreateInbound() {
        click(createInboundBtn);
        click(confirmInboundBtn);
    }

    public String getInboundCode() {
        String text = visible(inboundCodeText).getText();
        Matcher matcher = Pattern.compile("NHIV\\d+").matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("Inbound code not found in text: " + text);
        }
        return matcher.group();
    }

    private void selectReactOption(By field, By inputLocator, String keyword) {
        click(field);
        WebElement input = visible(inputLocator);
        input.sendKeys(keyword);
        visible(By.xpath("//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'" + keyword + "')]"));
        input.sendKeys(Keys.ENTER);
    }

    private WebElement last(By locator) {
        return wait.until(driver -> {
            List<WebElement> elements = all(locator);
            return elements.isEmpty() ? null : elements.get(elements.size() - 1);
        });
    }
}

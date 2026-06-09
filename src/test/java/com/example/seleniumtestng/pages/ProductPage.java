package com.example.seleniumtestng.pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ProductPage extends BasePage {
    private final By createProductDropdownBtn = By.xpath("//button[contains(.,'Tạo sản phẩm') or contains(.,'Tao san pham')]");
    private final By createProductMenuItem = By.xpath("//a[@title='Tạo sản phẩm'] | //button[@title='Tạo sản phẩm']");
    private final By skuInput = By.cssSelector("input[placeholder*='SKU']");
    private final By nameInput = By.xpath("//input[contains(@placeholder,'tên sản phẩm') or contains(@placeholder,'ten san pham')]");
    private final By brandDropdownField = By.xpath("//div[contains(@class,'-control')][.//input][1]");
    private final By brandOption = By.xpath("//*[contains(@class,'-menu')]//*[contains(@class,'-option')][1]");
    private final By openCategoryPopupBtn = By.xpath("//input[contains(@placeholder,'danh mục') or contains(@placeholder,'danh muc')]/following::button[contains(.,'Chọn') or contains(.,'Chon')][1]");
    private final By categoryOption = By.xpath("//div[contains(@class,'simplebar-content')]//*[normalize-space()='Watches']");
    private final By confirmCategoryBtn = By.xpath("//div[contains(@class,'modal-content')]//button[contains(.,'Chọn') or contains(.,'Chon')]");
    private final By costPriceField = By.xpath("//input[contains(@placeholder,'giá nhập') or contains(@placeholder,'gia nhap')]");
    private final By sellPriceField = By.xpath("//input[contains(@placeholder,'giá bán') or contains(@placeholder,'gia ban')]");
    private final By weightField = By.xpath("//input[contains(@placeholder,'Khối lượng') or contains(@placeholder,'Khoi luong')]");
    private final By lengthField = By.xpath("//input[contains(@placeholder,'Chiều dài') or contains(@placeholder,'Chieu dai')]");
    private final By widthField = By.xpath("//input[contains(@placeholder,'Chiều rộng') or contains(@placeholder,'Chieu rong')]");
    private final By heightField = By.xpath("//input[contains(@placeholder,'Chiều cao') or contains(@placeholder,'Chieu cao')]");
    private final By batchCheckbox = By.cssSelector("input#batch_lot");
    private final By descriptionField = By.xpath("//textarea[contains(@placeholder,'mô tả') or contains(@placeholder,'mo ta')]");
    private final By createProductBtn = By.xpath("//button[contains(.,'Tạo sản phẩm') or contains(.,'Tao san pham')]");
    private final By validationErrorLocator = By.xpath("//*[contains(@class,'Toastify__toast-body') or contains(@class,'ant-form-item-explain') or contains(@class,'error') or contains(@class,'invalid')][normalize-space(string())!='']");
    private final By searchInput = By.xpath("//input[contains(@placeholder,'SKU') and contains(@placeholder,'Barcode')]");

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    public void accessCreateProduct() {
        click(createProductDropdownBtn);
        click(createProductMenuItem);
    }

    public ProductData inputProductInfo(String sku, String productName) {
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);
        String finalSku = sku == null ? "SKU-" + suffix : sku;
        String finalProductName = productName == null ? "Prod-Auto-" + suffix : productName;
        fillBasicInfo(finalSku, finalProductName);
        return new ProductData(finalSku, finalProductName);
    }

    public void fillBasicInfo(String sku, String productName) {
        type(skuInput, sku);
        type(nameInput, productName);
    }

    public void selectBrand() {
        click(brandDropdownField);
        jsClick(visible(brandOption));
    }

    public void selectCategory() {
        click(openCategoryPopupBtn);
        click(categoryOption);
        click(confirmCategoryBtn);
    }

    public void fillCostPrice(String value) {
        type(costPriceField, value);
    }

    public void fillSellPrice(String value) {
        type(sellPriceField, value);
    }

    public void fillWeight(String value) {
        type(weightField, value);
    }

    public void fillDimensions(String length, String width, String height) {
        type(lengthField, length);
        type(widthField, width);
        type(heightField, height);
    }

    public void enableBatchManagement() {
        WebElement checkbox = visible(batchCheckbox);
        if (!checkbox.isSelected()) {
            jsClick(checkbox);
        }
    }

    public void fillDescription() {
        type(descriptionField, "This is an auto-generated product description.");
    }

    public void submitProduct() {
        List<WebElement> buttons = all(createProductBtn);
        WebElement submit = buttons.get(buttons.size() - 1);
        wait.until(ExpectedConditions.elementToBeClickable(submit));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", submit);
        submit.click();
    }

    public void searchProduct(String sku) {
        type(searchInput, sku);
    }

    public void verifyProductDisplayed(String sku) {
        By productLocator = By.xpath("//p[@title='" + sku + "'] | //*[normalize-space()='" + sku + "']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(productLocator));
    }

    public String getCreateProductErrorText() {
        for (WebElement element : all(validationErrorLocator)) {
            try {
                String text = element.getText().trim();
                if (element.isDisplayed() && !text.isEmpty() && text.length() < 300) {
                    return text;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return "";
    }

    public String waitForCreateProductErrorText() {
        return wait.until(driver -> {
            String text = getCreateProductErrorText();
            return text.isBlank() ? null : text;
        });
    }

    public static final class ProductData {
        private final String sku;
        private final String productName;

        public ProductData(String sku, String productName) {
            this.sku = sku;
            this.productName = productName;
        }

        public String sku() {
            return sku;
        }

        public String productName() {
            return productName;
        }
    }
}

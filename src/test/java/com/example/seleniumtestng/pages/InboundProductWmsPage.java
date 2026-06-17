package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.models.POSku;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class InboundProductWmsPage extends BasePage {
    private final By scanPoField = By.xpath("//input[contains(@placeholder,'PO')]");
    private final By scanBoxField = By.xpath("//input[contains(@placeholder,'kiện') or contains(@placeholder,'kien')]");
    private final By selectProductBtn = By.xpath("//button[i[contains(@class,'ri-more-fill')]]");
    private final By goodQtyField = By.cssSelector("input[name='quantity_goods_normal']");
    private final By barcodeField = By.cssSelector("input[name='manufacturer_barcode']");
    private final By batchLotField = By.cssSelector("input[name='batch_lot_code']");
    private final By lengthField = By.cssSelector("input[name='goods_d']");
    private final By widthField = By.cssSelector("input[name='goods_w']");
    private final By heightField = By.cssSelector("input[name='goods_h']");
    private final By weightField = By.cssSelector("input[name='goods_weight']");
    private final By confirmInspectBtn = By.xpath("//button[contains(.,'Kiểm hàng') or contains(.,'Kiem hang')]");

    public InboundProductWmsPage(WebDriver driver) {
        super(driver);
    }

    public void scanPo(String poCode) {
        clearAndEnter(visible(scanPoField), poCode);
    }

    public void scanBox(String boxCode) {
        clearAndEnter(visible(scanBoxField), boxCode);
    }

    public void waitProductActionReady() {
        wait.until(ExpectedConditions.elementToBeClickable(selectProductBtn));
    }

    public void inspectProduct() {
        click(selectProductBtn);
        List<WebElement> inspectButtons = all(By.xpath("//button[@role='menuitem' and (contains(.,'Kiểm hàng') or contains(.,'Kiem hang'))]"));
        for (WebElement button : inspectButtons) {
            if (isDisplayed(button)) {
                button.click();
                return;
            }
        }
        throw new IllegalStateException("Visible inspect product button not found");
    }

    public void inputGoodQuantity(int quantity) {
        type(goodQtyField, String.valueOf(quantity));
    }

    public void inputBarcode() {
        type(barcodeField, "AUTO" + System.currentTimeMillis());
    }

    public void inputBatchLotIfPresent() {
        List<WebElement> fields = all(batchLotField);
        if (!fields.isEmpty()) {
            fields.get(0).clear();
            fields.get(0).sendKeys(String.valueOf(System.currentTimeMillis()));
        }
    }

    public void inputProductDimensions(POSku sku) {
        fillIfEditable(lengthField, sku.goodsD());
        fillIfEditable(widthField, sku.goodsW());
        fillIfEditable(heightField, sku.goodsH());
        fillIfEditable(weightField, sku.goodsWeight());
    }

    public void confirmInspect() {
        click(confirmInspectBtn);
        visible(scanBoxField);
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

    private boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isEditable(WebElement input) {
        try {
            return input.isDisplayed() && input.isEnabled() && input.getDomAttribute("readonly") == null;
        } catch (RuntimeException e) {
            return false;
        }
    }
}

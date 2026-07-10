package com.example.seleniumtestng.flows;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.InboundProductData;
import com.example.seleniumtestng.pages.CreateInboundProductPage;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class InboundCreationFlow {
    private final WebDriver driver;

    public InboundCreationFlow(WebDriver driver) {
        this.driver = driver;
    }

    public String createApprovedInbound(List<InboundProductData> products) {
        driver.get(url("OMS", "/login"));
        AuthHelper.loginOms(driver);
        new WebDriverWait(driver, ConfigReader.timeout()).until(ExpectedConditions.urlContains("/dashboard"));

        driver.get(url("OMS", "/list-shipment-inbound?"));
        CreateInboundProductPage createInbound = new CreateInboundProductPage(driver);
        createInbound.openCreateInboundForm();
        createInbound.selectWarehouse(ConfigReader.required("INBOUND_WAREHOUSE_CODE"));
        createInbound.selectSupplier(ConfigReader.required("INBOUND_SUPPLIER"));
        createInbound.inputReference();
        addInboundProducts(createInbound, products);
        createInbound.confirmItems();
        createInbound.inputProductDimensions(
                ConfigReader.requiredInt("INBOUND_LENGTH"),
                ConfigReader.requiredInt("INBOUND_WIDTH"),
                ConfigReader.requiredInt("INBOUND_HEIGHT"));
        createInbound.confirmCreateInbound();

        String inboundCode = createInbound.getInboundCode();
        if (!inboundCode.matches("NHIV\\d+")) {
            throw new IllegalStateException("Invalid inbound code: " + inboundCode);
        }
        return inboundCode;
    }

    private void addInboundProducts(CreateInboundProductPage createInbound, List<InboundProductData> products) {
        createInbound.clickAddProduct();
        for (int index = 0; index < products.size(); index++) {
            if (index > 0) {
                createInbound.addNewProductRow();
            }
            InboundProductData product = products.get(index);
            createInbound.addProductToInbound(product.getSku(), product.getQuantity());
        }
    }

    private String url(String app, String path) {
        return ConfigReader.required(app.toUpperCase() + "_BASE_URL") + path;
    }
}

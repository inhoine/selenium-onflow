package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.pages.CreateInboundProductPage;
import com.example.seleniumtestng.pages.InboundProductWmsPage;
import com.example.seleniumtestng.utils.AuthHelper;
import com.example.seleniumtestng.utils.POSku;
import com.example.seleniumtestng.utils.ScanTable;
import com.example.seleniumtestng.utils.WmsApiClient;
import java.util.List;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.annotations.Test;

@SuppressWarnings("null")
public class InboundProductTest extends BaseTest {
    @Test
    public void createAndInspectInboundProduct() {
        driver.get(url("OMS", "/login"));
        AuthHelper.loginOms(driver);
        new org.openqa.selenium.support.ui.WebDriverWait(driver, ConfigReader.timeout())
                .until(ExpectedConditions.urlContains("/dashboard"));

        driver.get(url("OMS", "/list-shipment-inbound?"));
        CreateInboundProductPage createInbound = new CreateInboundProductPage(driver);
        createInbound.openCreateInboundForm();
        createInbound.selectWarehouse("PK100270");
        createInbound.selectSupplier("Supplier A");
        createInbound.inputReference();
        createInbound.clickAddProduct();
        createInbound.addProductToInbound("SKU-579065", 200);
        // createInbound.addNewProductRow();
        // createInbound.addProductToInbound("A-16", 150);
        createInbound.confirmItems();
        createInbound.inputProductDimensions(30, 20, 40);
        createInbound.confirmCreateInbound();

        String inboundCode = createInbound.getInboundCode();

        driver.get(url("WMS", "/login"));
        String token = AuthHelper.loginWms(driver);

        WmsApiClient wmsApiClient = new WmsApiClient();
        wmsApiClient.receivedPoAtWarehouse(inboundCode, token);

        driver.get(url("WMS", "/inspection"));
        new ScanTable(driver).scan("PACK02");

        InboundProductWmsPage inboundWms = new InboundProductWmsPage(driver);
        inboundWms.scanPo(inboundCode);

        List<POSku> poSkus = wmsApiClient.getPoSkus(inboundCode, token);
        for (POSku sku : poSkus) {
            inboundWms.scanBox(sku.boxCode());
            inboundWms.waitProductActionReady();
            inboundWms.inspectProduct();
            inboundWms.inputGoodQuantity(sku.quantityInbound());
            inboundWms.inputBarcode();
            inboundWms.inputBatchLotIfPresent();
            inboundWms.inputProductDimensions(sku);
            inboundWms.confirmInspect();
        }

        wmsApiClient.updatePutaway(inboundCode, token);
    }
}

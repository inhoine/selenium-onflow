package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.models.InboundProductData;
import com.example.seleniumtestng.models.POSku;
import com.example.seleniumtestng.pages.CreateInboundProductPage;
import com.example.seleniumtestng.pages.InboundProductWmsPage;
import com.example.seleniumtestng.utils.ScanTable;
import com.example.seleniumtestng.utils.TestDataReader;
import java.util.List;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class InboundProductTest extends BaseTest {
    @Test
    public void createAndInspectInboundProduct() {
        List<InboundProductData> inboundProducts = TestDataReader.inboundProducts();

        driver.get(url("OMS", "/login"));
        AuthHelper.loginOms(driver);
        new org.openqa.selenium.support.ui.WebDriverWait(driver, ConfigReader.timeout())
                .until(ExpectedConditions.urlContains("/dashboard"));

        driver.get(url("OMS", "/list-shipment-inbound?"));
        CreateInboundProductPage createInbound = new CreateInboundProductPage(driver);
        createInbound.openCreateInboundForm();
        createInbound.selectWarehouse(ConfigReader.required("INBOUND_WAREHOUSE_CODE"));
        createInbound.selectSupplier(ConfigReader.required("INBOUND_SUPPLIER"));
        createInbound.inputReference();
        addInboundProducts(createInbound, inboundProducts);
        createInbound.confirmItems();
        createInbound.inputProductDimensions(
                ConfigReader.requiredInt("INBOUND_LENGTH"),
                ConfigReader.requiredInt("INBOUND_WIDTH"),
                ConfigReader.requiredInt("INBOUND_HEIGHT"));
        createInbound.confirmCreateInbound();

        String inboundCode = createInbound.getInboundCode();
        Assert.assertTrue(inboundCode.matches("NHIV\\d+"), "Invalid inbound code: " + inboundCode);

        driver.get(url("WMS", "/login"));
        String token = AuthHelper.loginWms(driver);

        WmsApiClient wmsApiClient = new WmsApiClient();
        wmsApiClient.receivedPoAtWarehouse(inboundCode, token);

        driver.get(url("WMS", "/inspection"));
        new ScanTable(driver).scan(ConfigReader.required("INBOUND_PACKING_TABLE_CODE"));

        InboundProductWmsPage inboundWms = new InboundProductWmsPage(driver);
        inboundWms.scanPo(inboundCode);

        List<POSku> poSkus = wmsApiClient.getPoSkus(inboundCode, token);
        Assert.assertFalse(poSkus.isEmpty(), "Expected SKU data for inbound " + inboundCode);
        assertInboundProductsCreated(inboundProducts, poSkus);
        for (POSku sku : poSkus) {
            inspectInboundQuantities(inboundWms, sku);
        }

        int putawayTasksUpdated = wmsApiClient.updatePutaway(inboundCode, token);
        Assert.assertTrue(putawayTasksUpdated > 0, "No putaway task updated for " + inboundCode);
    }

    private void addInboundProducts(
            CreateInboundProductPage createInbound,
            List<InboundProductData> products) {
        createInbound.clickAddProduct();
        for (int index = 0; index < products.size(); index++) {
            if (index > 0) {
                createInbound.addNewProductRow();
            }
            InboundProductData product = products.get(index);
            createInbound.addProductToInbound(product.getSku(), product.getQuantity());
        }
    }

    private void assertInboundProductsCreated(
            List<InboundProductData> expectedProducts,
            List<POSku> actualProducts) {
        Assert.assertEquals(
                actualProducts.size(),
                expectedProducts.size(),
                "Unexpected number of products in created inbound");

        for (InboundProductData expected : expectedProducts) {
            POSku actual = actualProducts.stream()
                    .filter(product -> expected.getSku().equalsIgnoreCase(product.partnerCode()))
                    .findFirst()
                    .orElse(null);
            Assert.assertNotNull(actual, "SKU not found in created inbound: " + expected.getSku());
            Assert.assertEquals(
                    actual.quantityInbound(),
                    expected.getQuantity(),
                    "Unexpected inbound quantity for SKU " + expected.getSku());
        }
    }

    private void inspectInboundQuantities(InboundProductWmsPage inboundWms, POSku sku) {
        int[] quantities = splitQuantity(sku.quantityInbound(), InspectionQuantityType.values().length);
        InspectionQuantityType[] types = InspectionQuantityType.values();

        for (int index = 0; index < types.length; index++) {
            int quantity = quantities[index];
            if (quantity <= 0) {
                continue;
            }

            inboundWms.scanBox(sku.boxCode());
            inboundWms.waitProductActionReady();
            inboundWms.inspectProduct();
            inputInspectionQuantity(inboundWms, types[index], quantity);
            inboundWms.inputBarcode();
            inboundWms.inputBatchLotIfPresent();
            inboundWms.inputProductDimensions(sku);
            inboundWms.confirmInspect();
        }
    }

    private int[] splitQuantity(int totalQuantity, int parts) {
        int[] quantities = new int[parts];
        int baseQuantity = totalQuantity / parts;
        int remainder = totalQuantity % parts;

        for (int index = 0; index < parts; index++) {
            quantities[index] = baseQuantity + (index < remainder ? 1 : 0);
        }

        return quantities;
    }

    private void inputInspectionQuantity(
            InboundProductWmsPage inboundWms,
            InspectionQuantityType type,
            int quantity) {
        switch (type) {
            case GOOD:
                inboundWms.inputGoodQuantity(quantity);
                break;
            case DAMAGED_TYPE_1:
                inboundWms.inputDamagedType1Quantity(quantity);
                break;
            case DAMAGED_TYPE_2:
                inboundWms.inputDamagedType2Quantity(quantity);
                break;
            case DAMAGED_TYPE_3:
                inboundWms.inputDamagedType3Quantity(quantity);
                break;
            case LOST:
                inboundWms.inputLostQuantity(quantity);
                break;
            default:
                throw new IllegalArgumentException("Unsupported inspection quantity type: " + type);
        }
    }

    private enum InspectionQuantityType {
        GOOD,
        DAMAGED_TYPE_1,
        DAMAGED_TYPE_2,
        DAMAGED_TYPE_3,
        LOST
    }
}

package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.AuthHelper;
import com.example.seleniumtestng.models.OrderProductData;
import com.example.seleniumtestng.pages.CreateOrderOmsPage;
import com.example.seleniumtestng.pages.CreatePickupOrderPage;
import com.example.seleniumtestng.utils.TestDataReader;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CreateOrderTest extends BaseTest {
    @Test
    public void createB2cOrderAndPickupOrder() {
        int orderCount = ConfigReader.requiredInt("CREATE_ORDER_COUNT");
        List<OrderProductData> orderProducts = TestDataReader.orderProducts();
        List<String> trackingCodes = new ArrayList<>();

        driver.get(url("OMS", "/login"));
        AuthHelper.loginOms(driver);
        new WebDriverWait(driver, ConfigReader.timeout()).until(ExpectedConditions.urlContains("/dashboard"));

        for (int index = 1; index <= orderCount; index++) {
            CreatedOrder createdOrder = createB2cOrder(index, orderProducts);
            trackingCodes.add(createdOrder.trackingCode);
            Assert.assertFalse(createdOrder.trackingCode.isBlank(),
                    "Tracking code is blank for order " + createdOrder.orderNumber);
            System.out.println("Created B2C order " + index + "/" + orderCount
                    + " | orderNumber=" + createdOrder.orderNumber
                    + " | trackingCode=" + createdOrder.trackingCode);
        }
        System.out.println("Tracking codes sent to WMS pickup modal: " + String.join(",", trackingCodes));

        driver.get(url("WMS", "/login"));
        AuthHelper.loginWms(driver);
        driver.get(url("WMS", "/pickup-order"));

        CreatePickupOrderPage pickupOrderPage = new CreatePickupOrderPage(driver);
        pickupOrderPage.selectPickUpType(ConfigReader.required("CREATE_ORDER_PICKUP_TYPE"));
        pickupOrderPage.selectPickUpStrategy(ConfigReader.required("CREATE_ORDER_PICKUP_STRATEGY"));
        pickupOrderPage.selectCustomerWms(ConfigReader.required("CREATE_ORDER_WMS_CUSTOMER"));
        pickupOrderPage.addOrdersCustomize(trackingCodes);
        String successMessage = pickupOrderPage.getPickUpOrderCreatedMessage();
        Assert.assertFalse(successMessage.isBlank(), "Pickup order success message is blank");
    }

    private CreatedOrder createB2cOrder(int orderIndex, List<OrderProductData> products) {
        driver.get(url("OMS", "/orders-b2c?"));
        CreateOrderOmsPage createOrder = new CreateOrderOmsPage(driver);
        createOrder.accessCreateOrder();
        createOrder.selectCustomerOms(ConfigReader.required("CREATE_ORDER_CUSTOMER"));
        createOrder.selectSaleStore(ConfigReader.required("CREATE_ORDER_SALES_CHANNEL"));
        createOrder.selectChoosePickup(ConfigReader.required("CREATE_ORDER_PICKUP_CODE"));
        addOrderProducts(createOrder, products);
        String orderNumber = createOrder.inputOrderNumber(orderIndex);
        createOrder.confirmCreateOrder();
        createOrder.verifyCreatedOrder(orderNumber);
        return new CreatedOrder(orderNumber, createOrder.getTrackingCodeByOrderNumber(orderNumber));
    }

    private void addOrderProducts(CreateOrderOmsPage createOrder, List<OrderProductData> products) {
        for (int index = 0; index < products.size(); index++) {
            if (index > 0) {
                createOrder.addNewProductRow();
            }
            OrderProductData product = products.get(index);
            createOrder.addProductToCreateOrder(product.getSku(), product.getQuantity());
        }
    }

    private static class CreatedOrder {
        private final String orderNumber;
        private final String trackingCode;

        private CreatedOrder(String orderNumber, String trackingCode) {
            this.orderNumber = orderNumber;
            this.trackingCode = trackingCode;
        }
    }
}

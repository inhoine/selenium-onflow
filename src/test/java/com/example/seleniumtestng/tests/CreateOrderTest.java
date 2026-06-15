package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.pages.CreateOrderOmsPage;
import com.example.seleniumtestng.pages.CreatePickupOrderPage;
import com.example.seleniumtestng.utils.AuthHelper;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

@SuppressWarnings("null")
public class CreateOrderTest extends BaseTest {
    @Test
    public void createB2cOrderAndPickupOrder() {
        int CREATE_ORDER_COUNT= 4;

        int orderCount = CREATE_ORDER_COUNT;
        List<String> trackingCodes = new ArrayList<>();

        driver.get(url("OMS", "/login"));
        AuthHelper.loginOms(driver);
        new WebDriverWait(driver, ConfigReader.timeout()).until(ExpectedConditions.urlContains("/dashboard"));

        for (int index = 1; index <= orderCount; index++) {
            CreatedOrder createdOrder = createB2cOrder(index);
            trackingCodes.add(createdOrder.trackingCode);
            System.out.println("Created B2C order " + index + "/" + orderCount
                    + " | orderNumber=" + createdOrder.orderNumber
                    + " | trackingCode=" + createdOrder.trackingCode);
        }
        System.out.println("Tracking codes sent to WMS pickup modal: " + String.join(",", trackingCodes));

        driver.get(url("WMS", "/login"));
        AuthHelper.loginWms(driver);
        driver.get(url("WMS", "/pickup-order"));

        CreatePickupOrderPage pickupOrderPage = new CreatePickupOrderPage(driver);
        pickupOrderPage.selectPickUpType("Đơn hàng B2C");
        pickupOrderPage.selectPickUpStrategy("Lấy theo sản phẩm");
        pickupOrderPage.selectCustomerWms("THANH_AUTO");
        pickupOrderPage.addOrdersCustomize(trackingCodes);
        pickupOrderPage.verifyPickUpOrderCreated();
    }

    private CreatedOrder createB2cOrder(int orderIndex) {
        driver.get(url("OMS", "/orders-b2c?"));
        CreateOrderOmsPage createOrder = new CreateOrderOmsPage(driver);
        createOrder.accessCreateOrder();
        createOrder.selectCustomerOms("Thành Ngọc");
        createOrder.selectSaleStore("B2C");
        createOrder.selectChoosePickup("PK100270");
        createOrder.addProductToCreateOrder("SKU-579065", 10);
        // createOrder.addNewProductRow();
        // createOrder.addProductToCreateOrder("MHMSI", 2);
        String orderNumber = createOrder.inputOrderNumber(orderIndex);
        createOrder.confirmCreateOrder();
        createOrder.verifyCreatedOrder(orderNumber);
        return new CreatedOrder(orderNumber, createOrder.getTrackingCodeByOrderNumber(orderNumber));
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

package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.pages.CreateOrderOmsPage;
import com.example.seleniumtestng.pages.CreatePickupOrderPage;
import com.example.seleniumtestng.utils.AuthHelper;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

public class CreateOrderTest extends BaseTest {
    @Test
    public void createB2cOrderAndPickupOrder() {
        driver.get(url("OMS", "/login"));
        AuthHelper.loginOms(driver);
        new WebDriverWait(driver, ConfigReader.timeout()).until(ExpectedConditions.urlContains("/dashboard"));

        driver.get(url("OMS", "/orders-b2c?"));
        CreateOrderOmsPage createOrder = new CreateOrderOmsPage(driver);
        createOrder.accessCreateOrder();
        createOrder.selectCustomerOms("Thành Ngọc");
        createOrder.selectSaleStore("B2C");
        createOrder.selectChoosePickup("PK100270");
        createOrder.addProductToCreateOrder("SKU-200163", 2);
        createOrder.addNewProductRow();
        createOrder.addProductToCreateOrder("MHMSI", 2);
        String orderNumber = createOrder.inputOrderNumber();
        createOrder.confirmCreateOrder();
        createOrder.verifyCreatedOrder(orderNumber);
        String trackingCode = createOrder.getTrackingCodeByOrderNumber(orderNumber);

        driver.get(url("WMS", "/login"));
        AuthHelper.loginWms(driver);
        driver.get(url("WMS", "/pickup-order"));

        CreatePickupOrderPage pickupOrderPage = new CreatePickupOrderPage(driver);
        pickupOrderPage.selectPickUpType("Đơn hàng B2C");
        pickupOrderPage.selectPickUpStrategy("Lấy theo sản phẩm");
        pickupOrderPage.selectCustomerWms("THANH_AUTO");
        pickupOrderPage.addOrderCustomize(trackingCode);
        pickupOrderPage.verifyPickUpOrderCreated();
    }
}

package com.example.seleniumtestng.flows;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.CreatedOrder;
import com.example.seleniumtestng.models.OrderProductData;
import com.example.seleniumtestng.pages.CreateOrderOmsPage;
import com.example.seleniumtestng.pages.CreatePickupOrderPage;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OrderCreationFlow {
    private final WebDriver driver;

    public OrderCreationFlow(WebDriver driver) {
        this.driver = driver;
    }

    public List<CreatedOrder> createB2cOrders(int orderCount, List<OrderProductData> products) {
        driver.get(url("OMS", "/auth/login"));
        AuthHelper.loginOms(driver);
        new WebDriverWait(driver, ConfigReader.timeout()).until(ExpectedConditions.urlContains("/dashboard"));

        List<CreatedOrder> createdOrders = new ArrayList<>();
        for (int index = 1; index <= orderCount; index++) {
            CreatedOrder createdOrder = createB2cOrder(index, products);
            createdOrders.add(createdOrder);
            System.out.println("Created B2C order " + index + "/" + orderCount
                    + " | orderNumber=" + createdOrder.orderNumber()
                    + " | trackingCode=" + createdOrder.trackingCode());
        }
        return createdOrders;
    }

    public void createPickupOrder(List<String> trackingCodes) {
        driver.get(url("WMS", "/login"));
        AuthHelper.loginWms(driver);
        driver.get(url("WMS", "/pickup-order"));

        CreatePickupOrderPage pickupOrderPage = new CreatePickupOrderPage(driver);
        pickupOrderPage.selectBtnPickUp();
        pickupOrderPage.configureRequiredSetup(
                ConfigReader.required("CREATE_ORDER_PICKUP_TYPE"),
                ConfigReader.get("CREATE_ORDER_PICKUP_STRATEGY"),
                ConfigReader.get("CREATE_ORDER_ORDER_SIZE"));
        pickupOrderPage.selectCustomerWms(ConfigReader.required("CREATE_ORDER_WMS_CUSTOMER"));
        pickupOrderPage.addOrdersCustomize(trackingCodes);
        String successMessage = pickupOrderPage.getPickUpOrderCreatedMessage();
        if (successMessage.isBlank()) {
            throw new IllegalStateException("Pickup order success message is blank");
        }
    }

    private CreatedOrder createB2cOrder(int orderIndex, List<OrderProductData> products) {
        driver.get(url("OMS", "/outbound/orders/b2c?"));
        CreateOrderOmsPage createOrder = new CreateOrderOmsPage(driver);
        createOrder.accessCreateOrder();
        createOrder.selectCustomerOms(ConfigReader.required("CREATE_ORDER_CUSTOMER"));
        createOrder.selectSaleStore(ConfigReader.required("CREATE_ORDER_SALES_CHANNEL"));
        createOrder.selectChoosePickup(ConfigReader.required("CREATE_ORDER_PICKUP_CODE"));
        createOrder.continueToProductStep();
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

    private String url(String app, String path) {
        return ConfigReader.required(app.toUpperCase() + "_BASE_URL") + path;
    }
}

package com.example.seleniumtestng.flows;

import com.example.seleniumtestng.clients.WmsApiClient;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.CreatedOrder;
import com.example.seleniumtestng.models.OrderProductData;
import com.example.seleniumtestng.pages.CreatePickupOrderPage;
import com.example.seleniumtestng.pages.order.CreateOrderOmsPage;
import com.example.seleniumtestng.pages.order.OrderListingOmsPage;
import com.example.seleniumtestng.utils.Log;
import com.example.seleniumtestng.utils.TestDataReader;
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
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null || !currentUrl.contains("onflow.vn") || currentUrl.contains("/login")) {
            driver.get(url("OMS", "/auth/login"));
            AuthHelper.loginOms(driver);
        }
        new WebDriverWait(driver, ConfigReader.timeout()).until(ExpectedConditions.urlContains("/dashboard"));

        List<CreatedOrder> createdOrders = new ArrayList<>();
        for (int index = 1; index <= orderCount; index++) {
            CreatedOrder createdOrder = createB2cOrder(index, products);
            createdOrders.add(createdOrder);
            Log.info("Created B2C order {}/{}: orderNumber={}, trackingCode={}",
                    index, orderCount, createdOrder.orderNumber(), createdOrder.trackingCode());
        }
        return createdOrders;
    }

    public String createPickupOrder(List<String> trackingCodes) {
        if (trackingCodes == null || trackingCodes.isEmpty()) {
            throw new IllegalArgumentException("trackingCodes must not be empty to create pickup order");
        }
        driver.get(url("WMS", "/login"));
        String token = AuthHelper.loginWms(driver);
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
        Log.info("Pickup created message: {}", successMessage);

        String pickupCode = pickupOrderPage.getCreatedPickupCodeIfPresent();
        if (pickupCode != null && !pickupCode.isBlank()) {
            Log.info("Detected created pickupCode from UI: {}", pickupCode);
            return pickupCode;
        }

        WmsApiClient client = new WmsApiClient();
        String firstTracking = trackingCodes.get(0);
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String codeFromOrder = client.getOrderPickupCode(firstTracking, token);
                if (codeFromOrder != null && !codeFromOrder.isBlank()) {
                    Log.info("Detected created pickupCode from WMS order API (attempt {}): {}", attempt, codeFromOrder);
                    return codeFromOrder;
                }
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.warn("Querying pickupCode from order API attempt {} failed: {}", attempt, e.getMessage());
            }
        }

        return null;
    }

    public String createOrdersAndPickupOnDemand(int orderCount) {
        List<OrderProductData> orderProducts = TestDataReader.orderProducts();
        List<CreatedOrder> createdOrders = createB2cOrders(orderCount, orderProducts);
        List<String> trackingCodes = new ArrayList<>();
        for (CreatedOrder createdOrder : createdOrders) {
            trackingCodes.add(createdOrder.trackingCode());
        }
        return createPickupOrder(trackingCodes);
    }

    private CreatedOrder createB2cOrder(int orderIndex, List<OrderProductData> products) {
        driver.get(url("OMS", "/outbound/orders/b2c?"));
        CreateOrderOmsPage createOrder = new CreateOrderOmsPage(driver);
        OrderListingOmsPage orderListing = new OrderListingOmsPage(driver);
        createOrder.accessCreateOrder();
        createOrder.selectCustomerOms(ConfigReader.required("CREATE_ORDER_CUSTOMER"));
        createOrder.selectSaleStore(ConfigReader.required("CREATE_ORDER_SALES_CHANNEL"));
        createOrder.selectChoosePickup(ConfigReader.required("CREATE_ORDER_PICKUP_CODE"));
        createOrder.continueToProductStep();
        addOrderProducts(createOrder, products);
        String orderNumber = createOrder.inputOrderNumber(orderIndex);
        createOrder.confirmCreateOrder();
        orderListing.verifyCreatedOrder(orderNumber);
        String trackingCode = orderListing.getTrackingCodeByOrderNumber(orderNumber);
        return new CreatedOrder(orderNumber, trackingCode);
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

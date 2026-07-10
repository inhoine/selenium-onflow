package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.OrderCreationFlow;
import com.example.seleniumtestng.models.CreatedOrder;
import com.example.seleniumtestng.models.OrderProductData;
import com.example.seleniumtestng.utils.TestDataReader;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CreateOrderTest extends BaseTest {
    @Test
    public void createB2cOrderAndPickupOrder() {
        int orderCount = ConfigReader.requiredInt("CREATE_ORDER_COUNT");
        List<OrderProductData> orderProducts = TestDataReader.orderProducts();
        OrderCreationFlow orderCreationFlow = new OrderCreationFlow(driver);
        List<CreatedOrder> createdOrders = orderCreationFlow.createB2cOrders(orderCount, orderProducts);
        List<String> trackingCodes = new ArrayList<>();

        for (CreatedOrder createdOrder : createdOrders) {
            trackingCodes.add(createdOrder.trackingCode());
            Assert.assertFalse(createdOrder.trackingCode().isBlank(),
                    "Tracking code is blank for order " + createdOrder.orderNumber());
        }
        System.out.println("Tracking codes sent to WMS pickup modal: " + String.join(",", trackingCodes));

        if (!ConfigReader.getBoolean("CREATE_ORDER_CREATE_PICKUP", true)) {
            return;
        }

        orderCreationFlow.createPickupOrder(trackingCodes);
    }
}

package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.flows.OrderCreationFlow;
import com.example.seleniumtestng.models.CreatedOrder;
import com.example.seleniumtestng.models.OrderProductData;
import com.example.seleniumtestng.utils.Log;
import com.example.seleniumtestng.utils.TestDataReader;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

@Epic("OMS/WMS - Quản Lý Đơn Hàng")
@Feature("Tạo Đơn Hàng & Yêu Cầu Xuất (Order & Pickup Creation)")
public class CreateOrderTest extends BaseTest {

    @Test(description = "Verify creation of B2C orders in OMS and linked Pickup Order in WMS")
    @Story("Tạo đơn B2C trên OMS và tạo phiếu xuất kho Pickup trên WMS")
    @Severity(SeverityLevel.BLOCKER)
    public void createB2cOrderAndPickupOrder() {
        int orderCount = ConfigReader.requiredInt("CREATE_ORDER_COUNT");
        List<OrderProductData> orderProducts = TestDataReader.orderProducts();
        OrderCreationFlow orderCreationFlow = new OrderCreationFlow(driver);

        Log.info("Bắt đầu kịch bản tạo đơn: orderCount={}, productsCount={}", orderCount, orderProducts.size());

        // 1. Tạo đơn hàng B2C trên OMS
        List<CreatedOrder> createdOrders = createOrdersStep(orderCreationFlow, orderCount, orderProducts);

        // 2. Xác thực và thu thập mã vận đơn
        List<String> trackingCodes = validateTrackingCodesStep(createdOrders, orderCount);

        // 3. Gom bảng kê Pickup trên WMS (nếu được kích hoạt)
        if (!ConfigReader.getBoolean("CREATE_ORDER_CREATE_PICKUP", true)) {
            Log.info("Bỏ qua bước tạo bảng kê WMS do CREATE_ORDER_CREATE_PICKUP=false");
            return;
        }

        createPickupStep(orderCreationFlow, trackingCodes);
    }

    @Step("1. Tạo {orderCount} đơn hàng B2C trên hệ thống OMS")
    private List<CreatedOrder> createOrdersStep(OrderCreationFlow flow, int orderCount,
            List<OrderProductData> products) {
        List<CreatedOrder> createdOrders = flow.createB2cOrders(orderCount, products);
        Assert.assertNotNull(createdOrders, "Danh sách đơn hàng tạo ra không được null");
        Assert.assertEquals(createdOrders.size(), orderCount, "Số lượng đơn hàng tạo ra không đúng kỳ vọng");
        return createdOrders;
    }

    @Step("2. Xác thực thông tin đơn hàng và mã vận đơn (Tracking Codes)")
    private List<String> validateTrackingCodesStep(List<CreatedOrder> createdOrders, int expectedCount) {
        List<String> trackingCodes = new ArrayList<>();
        for (CreatedOrder createdOrder : createdOrders) {
            String trackingCode = createdOrder.trackingCode();
            Assert.assertNotNull(trackingCode, "Tracking code is null for order " + createdOrder.orderNumber());
            Assert.assertFalse(trackingCode.isBlank(),
                    "Tracking code is blank for order " + createdOrder.orderNumber());
            trackingCodes.add(trackingCode);
        }
        Assert.assertEquals(trackingCodes.size(), expectedCount, "Số lượng tracking codes phải khớp với số lượng đơn");
        Log.info("Danh sách mã vận đơn được tạo thành công: {}", String.join(", ", trackingCodes));
        return trackingCodes;
    }

    @Step("3. Tạo bảng kê xuất kho (Pickup Order) trên WMS liên kết {trackingCodes.size()} đơn vừa tạo")
    private String createPickupStep(OrderCreationFlow flow, List<String> trackingCodes) {
        String pickupCode = flow.createPickupOrder(trackingCodes);
        // Assert.assertNotNull(pickupCode, "Bảng kê Pickup phải được tạo thành công
        // (pickupCode không được null)");
        // Assert.assertFalse(pickupCode.isBlank(), "Mã bảng kê Pickup không được
        // rỗng");
        // Log.info("Hoàn tất tạo bảng kê xuất kho WMS: pickupCode={}", pickupCode);
        return pickupCode;
    }
}

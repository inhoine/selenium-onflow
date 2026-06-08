const { until } = require("selenium-webdriver");
const BaseTest = require("./BaseTest");
const login_oms = require("../utils/login_oms");
const login_wms = require("../utils/login_wms");
const CreateOrderOMS = require("../pages/CreateOrderOMS");
const CreatePickupOrderPage = require("../pages/CreatePickUpOrderPage");
const config = require("../config");

const OMS_BASE_URL = config.urls.oms;
const WMS_BASE_URL = config.urls.wms;

const OMS_LOGIN_URL = `${OMS_BASE_URL}/login`;
const OMS_ORDERS_URL = `${OMS_BASE_URL}/orders-b2c?`;
const WMS_LOGIN_URL = `${WMS_BASE_URL}/login`;
const WMS_PICKUP_ORDER_URL = `${WMS_BASE_URL}/pickup-order`;

async function createOrderTest() {
  const baseTest = new BaseTest("chrome", config.defaultTimeout);
  const driver = await baseTest.setup();

  try {
    await baseTest.goTo(OMS_LOGIN_URL);
    await login_oms(driver);
    await driver.wait(until.urlContains("/dashboard"), config.defaultTimeout);

    await baseTest.goTo(OMS_ORDERS_URL);

    const createOrder = new CreateOrderOMS(driver);
    await createOrder.accessCreateOrder();
    await createOrder.selectCustomerOMS("Thành Ngọc");
    await createOrder.selectSaleStore("B2C");
    await createOrder.selectChoosePickup("PK100270");
    await createOrder.addProductToCreateOrder("SKU-200163", 2);

    const orderNumber = await createOrder.inputOrderNumber();
    await createOrder.confirmCreateOrder();
    await createOrder.verifyCreatedOrder(orderNumber);

    const trackingCode =
      await createOrder.getTrackingCodeByOrderNumber(orderNumber);
    console.log("Tracking code for order", orderNumber, "is:", trackingCode);

    await baseTest.goTo(WMS_LOGIN_URL);
    const token = await login_wms(driver);
    console.log("WMS token received");

    await baseTest.goTo(WMS_PICKUP_ORDER_URL);

    const createPickupOrderPage = new CreatePickupOrderPage(driver);
    await createPickupOrderPage.selectPickUpType("Đơn hàng B2C");
    await createPickupOrderPage.selectPickUpStrategy("Lấy theo sản phẩm");
    await createPickupOrderPage.selectCustomerWMS("THANH_AUTO");
    await createPickupOrderPage.addOrderCustomize(trackingCode);
    await createPickupOrderPage.verifyPickUpOrderCreated();

    return { orderNumber, trackingCode, token };
  } catch (error) {
    console.error("createOrderTest failed:", error);
    throw error;
  } finally {
    await baseTest.teardown();
  }
}

if (require.main === module) {
  createOrderTest().catch((error) => {
    console.error("Create order test failed:", error);
    process.exit(1);
  });
}

module.exports = createOrderTest;

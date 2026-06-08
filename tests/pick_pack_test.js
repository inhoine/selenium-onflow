const { until } = require("selenium-webdriver");
const chrome = require("selenium-webdriver/chrome");
const BaseTest = require("./BaseTest");
const login_wms = require("../utils/login_wms");
const PickAndPackOrderPage = require("../pages/PickAndPackOrderPage");
const {
  getPickupDetail,
  getPickupPackingOrders,
  mapTrolleyPicking,
  pickAllProductsInPickup,
  commitPickingPickup,
} = require("../utils/wms_api");

const WMS_BASE_URL = "https://stg-wms.onflow.vn";
const LOGIN_URL = `${WMS_BASE_URL}/login`;
const EQUIPMENTS_URL = `${WMS_BASE_URL}/equipments?page=1&page_size=50`;
const PICKUP_DETAIL_URL = (pickupId) =>
  `${WMS_BASE_URL}/pickup-detail/${pickupId}`;
const PICKUP_ID = "648195";
const PACKING_MATERIAL_CODE = "40x20x20";

function buildChromeOptions() {
  const options = new chrome.Options();
  options.addArguments("--no-first-run");
  options.addArguments("--no-default-browser-check");
  return options;
}

async function clearBrowserStorage(driver) {
  await driver.executeScript("localStorage.clear(); sessionStorage.clear();");
}

async function createEquipment(pickAndPackOrder) {
  const equipmentCode = await pickAndPackOrder.addEquipment(
    "Xe chứa",
    "Không ưu tiên",
  );
  await pickAndPackOrder.verifyToastMessage(
    "Thêm thiết bị chứa hàng thành công",
  );
  console.log("Equipment code:", equipmentCode);
  return equipmentCode;
}

async function executePickPackFlow(pickAndPackOrder, pickupId, token) {
  await pickAndPackOrder.receivePackingTrolley(pickupId);
  await pickAndPackOrder.verifyToastMessage("Nhận bảng kê thành công");

  const packingOrders = await getPickupPackingOrders(pickupId, token);
  await pickAndPackOrder.scanTablePacking();
  await pickAndPackOrder.scanPickUpOrder(pickupId);
  await pickAndPackOrder.packBySystemSuggestion(
    packingOrders,
    PACKING_MATERIAL_CODE,
  );
}

async function pickPackTest() {
  const options = buildChromeOptions();
  const baseTest = new BaseTest("chrome", 15000, options);
  const driver = await baseTest.setup();

  try {
    await driver.manage().deleteAllCookies();
    await baseTest.goTo(LOGIN_URL);
    await clearBrowserStorage(driver);

    const token = await login_wms(driver);
    console.log("WMS TOKEN:", token);

    const pickAndPackOrder = new PickAndPackOrderPage(driver);

    await baseTest.goTo(EQUIPMENTS_URL);
    const equipmentCode = await createEquipment(pickAndPackOrder);

    await baseTest.goTo(PICKUP_DETAIL_URL(PICKUP_ID));
    const pickupItems = await getPickupDetail(PICKUP_ID, token);
    console.log("Pickup items:", pickupItems);

    await mapTrolleyPicking(PICKUP_ID, equipmentCode, token);
    console.log("Map trolley success");

    await pickAllProductsInPickup(PICKUP_ID, token);
    console.log("Pick all products completed");

    await commitPickingPickup(PICKUP_ID, equipmentCode, token);
    console.log("Commit picking completed");

    await executePickPackFlow(pickAndPackOrder, PICKUP_ID, token);
    console.log("Pick & pack flow completed");
  } catch (error) {
    console.error("Pick & pack test failed:", error);
    throw error;
  } finally {
    // await baseTest.teardown();
  }
}

pickPackTest();

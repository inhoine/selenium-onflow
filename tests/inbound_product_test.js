const { until } = require("selenium-webdriver");
const BaseTest = require("./BaseTest");
const login_oms = require("../utils/login_oms");
const CreateInboundProductPage = require("../pages/CreateInboundProductPage");
const InboundProductWmsPage = require("../pages/InboundProductWmsPage");
const { receivedPOAtWarehouse, updatePutaway } = require("../utils/wms_api");
const login_wms = require("../utils/login_wms");
const scanTable = require("../utils/scan_table");
const getPOSkus = require("../utils/get_po_skus");
const config = require("../config");

async function getWmsToken(driver) {
  await driver.wait(async () => {
    const token = await driver.executeScript(
      'return localStorage.getItem("token");',
    );
    return token !== null;
  }, config.defaultTimeout);

  return driver.executeScript("return localStorage.getItem('token');");
}

async function inboundProductTest() {
  const baseTest = new BaseTest("chrome", config.defaultTimeout);
  const driver = await baseTest.setup();

  try {
    await baseTest.goTo(`${config.urls.oms}/login`);
    await login_oms(driver);
    await driver.wait(until.urlContains("/dashboard"), config.defaultTimeout);

    await baseTest.goTo(`${config.urls.oms}/list-shipment-inbound?`);

    const createInboundProductPage = new CreateInboundProductPage(driver);
    await createInboundProductPage.clickCreateInboundBtn();
    await createInboundProductPage.clickCreateInboundMenuItem();
    await createInboundProductPage.selectWarehouse();
    await createInboundProductPage.selectSupplier();
    await createInboundProductPage.inputReference();
    await createInboundProductPage.clickAddProductBtn();
    await createInboundProductPage.addProductToInbound("SKU-200163", 200);
    await createInboundProductPage.clickAddNewProductBtn();
    await createInboundProductPage.addProductToInbound("A-16", 150);
    await createInboundProductPage.clickItemsConfirmInbound();
    await createInboundProductPage.inputProductDimensions(30, 20, 40);
    await createInboundProductPage.confirmCreateInbound();

    const inboundCode = await createInboundProductPage.getInboundCode();
    console.log("Saved inbound code:", inboundCode);

    await baseTest.goTo(`${config.urls.wms}/login`);
    const inboundProductWmsPage = new InboundProductWmsPage(driver);
    await login_wms(driver);

    await driver.wait(
      until.urlContains("/user-setting"),
      config.defaultTimeout,
    );
    const token = await getWmsToken(driver);
    console.log("WMS TOKEN:", token);

    await receivedPOAtWarehouse(inboundCode, token);
    await baseTest.goTo(`${config.urls.wms}/inspection`);

    await scanTable(driver, "PACK02");
    await inboundProductWmsPage.scanPo(inboundCode);

    const poSkus = await getPOSkus(inboundCode, token);
    for (const sku of poSkus) {
      await inboundProductWmsPage.scanBox(sku.boxCode);
      await inboundProductWmsPage.waitProductActionReady();
      await inboundProductWmsPage.inspectProduct();
      await inboundProductWmsPage.inputGoodQuantity(sku.quantityInbound);
      await inboundProductWmsPage.inputBarcode();
      await inboundProductWmsPage.inputBatchLot();
      await inboundProductWmsPage.inputProductDimensions(sku);
      await inboundProductWmsPage.confirmInspect();
    }

    await updatePutaway(inboundCode, token);
    console.log("Inbound product flow completed for PO:", inboundCode);
  } catch (error) {
    console.error("Inbound product test failed:", error);
    throw error;
  } finally {
    await baseTest.teardown();
  }
}

if (require.main === module) {
  inboundProductTest().catch((error) => {
    console.error("Inbound product test failed:", error);
    process.exit(1);
  });
}

module.exports = inboundProductTest;

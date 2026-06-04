const { until } = require("selenium-webdriver");
const BaseTest = require("./BaseTest");
const ProductPage = require("../pages/ProductPage");
const ListingProductPage = require("../pages/ListingProductPage");
const AssignProductPage = require("../pages/AssignProductPage");
const login_oms = require("../utils/login_oms");
const login_ops = require("../utils/login_ops");

const urls = {
  omsLogin: "https://stg-oms.onflow.vn/login",
  omsProductList:
    "https://stg-oms.onflow.vn/products?page=1&page_size=50&return_type=list_linked_platform&status_group=all&type_filter=all",
  opsLogin: "https://stg-ops.onflow.vn/login",
};

async function createProductTest() {
  const baseTest = new BaseTest();
  const driver = await baseTest.setup();

  try {
    await baseTest.goTo(urls.omsLogin);
    await login_oms(driver);

    await driver.wait(until.urlContains("/dashboard"), 15000);
    await baseTest.goTo(urls.omsProductList);

    const productPage = new ProductPage(driver);
    await productPage.accessCreateProduct();

    const productData = await productPage.inputProductInfor();
    await productPage.selectBrand();
    await productPage.selectCategory();
    await productPage.priceProduct();
    await productPage.demensionProduct();
    await productPage.inboundManagement({ serial: false, batch: true });
    await productPage.descriptionProduct();
    await productPage.submitProduct();

    await productPage.searchProduct(productData.sku);
    await productPage.verifyProductDisplayed(productData.sku);

    const listingProductPage = new ListingProductPage(driver);
    await listingProductPage.accessListingProduct();
    await listingProductPage.selectSalesChannel();
    await listingProductPage.selectChannelOption("B2C");
    await listingProductPage.clickContinue();
    await listingProductPage.clickSellProduct();

    await baseTest.goTo(urls.opsLogin);
    await login_ops(driver);

    const assignProductPage = new AssignProductPage(driver);
    await assignProductPage.closeChangePwdModal();
    await assignProductPage.closeNotificationModal();
    await assignProductPage.getProductWithID();
    await assignProductPage.searchProduct(productData.sku);
    await assignProductPage.waitForSearchResults();
    await assignProductPage.assignProductToWarehouse();
  } catch (error) {
    console.error("Product test failed:", error);
  } finally {
    // await baseTest.teardown();
  }
}

createProductTest();

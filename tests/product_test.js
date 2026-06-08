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

async function switchToMainWindow(driver, mainWindowHandle) {
  const handles = await driver.getAllWindowHandles();
  if (!handles.includes(mainWindowHandle)) {
    throw new Error("Main window handle not found among open windows");
  }
  await driver.switchTo().window(mainWindowHandle);
}

async function closeExtraWindows(driver, mainWindowHandle) {
  const handles = await driver.getAllWindowHandles();
  for (const handle of handles) {
    if (handle === mainWindowHandle) {
      continue;
    }
    await driver.switchTo().window(handle);
    await driver.close();
  }
  await switchToMainWindow(driver, mainWindowHandle);
}

async function loginToOms(driver) {
  await driver.get(urls.omsLogin);
  await login_oms(driver);
  await driver.wait(until.urlContains("/dashboard"), 15000);
  await driver.get(urls.omsProductList);
}

async function createProductPositive(driver, options = {}, mainWindowHandle) {
  if (mainWindowHandle) {
    await switchToMainWindow(driver, mainWindowHandle);
  }

  const productPage = new ProductPage(driver);
  await productPage.accessCreateProduct();

  const productData = await productPage.inputProductInfor({
    sku: options.customSku,
    productName: options.customProductName,
  });
  await productPage.selectBrand();
  await productPage.selectCategory();
  await productPage.fillCostPrice(options.customCostPrice ?? "200000");
  await productPage.fillSellPrice(options.customSellPrice ?? "200000");
  await productPage.fillWeight(options.customWeight ?? "300");
  await productPage.fillDimensions(
    options.customDimensions ?? { length: "20", width: "20", height: "20" },
  );
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

  console.log("Navigating to OPS login to complete assign flow");
  await driver.get(urls.opsLogin);
  await login_ops(driver);
  console.log("OPS login complete");

  const assignProductPage = new AssignProductPage(driver);
  await assignProductPage.closeChangePwdModal();
  await assignProductPage.closeNotificationModal();
  await assignProductPage.getProductWithID();
  console.log(`Starting OPS assignment for SKU: ${productData.sku}`);
  await assignProductPage.searchProduct(productData.sku);
  await assignProductPage.waitForSearchResults();
  await assignProductPage.assignProductToWarehouse();

  return productData;
}

async function runProductScenario(driver, scenario) {
  const productPage = new ProductPage(driver);
  await productPage.accessCreateProduct();

  const randomNUM = Date.now().toString().slice(-6);
  const sku =
    scenario.customSku ?? (scenario.skipSku ? "" : `SKU-${randomNUM}`);
  const productName =
    scenario.customProductName ??
    (scenario.skipName ? "" : `Prod-Auto-${randomNUM}`);

  await productPage.fillBasicInfo({ sku, productName });

  if (!scenario.skipBrand) {
    await productPage.selectBrand();
  }

  if (!scenario.skipCategory) {
    await productPage.selectCategory();
  }

  await productPage.fillCostPrice(
    scenario.customCostPrice ?? (scenario.skipCostPrice ? null : "200000"),
  );
  await productPage.fillSellPrice(
    scenario.customSellPrice ?? (scenario.skipSellPrice ? null : "250000"),
  );
  await productPage.fillWeight(
    scenario.customWeight ?? (scenario.skipWeight ? null : "300"),
  );
  await productPage.fillDimensions(
    scenario.customDimensions ??
      (scenario.skipDimensions
        ? {}
        : { length: "20", width: "20", height: "20" }),
  );

  await productPage.descriptionProduct();
  await productPage.submitProduct();

  if (scenario.shouldSucceed) {
    await productPage.searchProduct(sku);
    await productPage.verifyProductDisplayed(sku);
    return { sku, productName };
  }

  await productPage.waitForCreateProductFailure(10000);
  const errorText = await productPage.getCreateProductErrorText();
  const shouldMatch = scenario.expectedErrorContains;
  const hasExpectedText = shouldMatch
    ? errorText.toLowerCase().includes(shouldMatch.toLowerCase())
    : true;

  if (!hasExpectedText && errorText) {
    throw new Error(
      `Expected validation message containing '${shouldMatch}', got '${errorText}'`,
    );
  }

  if (!errorText) {
    console.warn(
      `⚠️ ${scenario.name}: no visible validation message found, but form remained on create product page`,
    );
  }
}

async function runProductTestSuite() {
  const baseTest = new BaseTest();
  const driver = await baseTest.setup();
  const results = [];

  const scenarios = [
    {
      name: "Positive case - create product with all required fields",
      shouldSucceed: true,
    },
    {
      name: "Negative case - missing SKU",
      skipSku: true,
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Negative case - missing product name",
      skipName: true,
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Negative case - missing brand",
      skipBrand: true,
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Negative case - missing category",
      skipCategory: true,
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Negative case - missing cost price",
      skipCostPrice: true,
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Negative case - missing sell price",
      skipSellPrice: true,
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Negative case - missing weight",
      skipWeight: true,
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Negative case - missing dimensions",
      skipDimensions: true,
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Edge case - duplicate SKU",
      useDuplicateSku: true,
      shouldSucceed: false,
      expectedErrorContains: "already exists",
    },
    {
      name: "Edge case - product name with special characters",
      customProductName: "Prod-Auto-!@#$%^&*()_+[]{};:'\"<>?,./",
      shouldSucceed: true,
    },
    {
      name: "Edge case - sell price lower than cost price",
      customCostPrice: "200000",
      customSellPrice: "150000",
      shouldSucceed: true,
      expectedErrorContains: "giá",
    },
    {
      name: "Edge case - invalid weight value",
      customWeight: "0",
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Edge case - invalid dimension value",
      customDimensions: { length: "0", width: "20", height: "20" },
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
  ];

  try {
    await loginToOms(driver);
    const mainWindowHandle = await driver.getWindowHandle();
    let existingSku = null;

    for (const scenario of scenarios) {
      try {
        if (scenario.useDuplicateSku) {
          if (!existingSku) {
            throw new Error(
              "Duplicate SKU scenario requires a previously created SKU",
            );
          }
          scenario.customSku = existingSku;
        }

        if (scenario.shouldSucceed) {
          const created = await createProductPositive(
            driver,
            scenario,
            mainWindowHandle,
          );
          if (!existingSku && created && created.sku) {
            existingSku = created.sku;
          }
        } else {
          await runProductScenario(driver, scenario);
        }

        results.push({ name: scenario.name, status: "passed" });
      } catch (error) {
        results.push({
          name: scenario.name,
          status: "failed",
          error: error.message,
        });
      } finally {
        try {
          await closeExtraWindows(driver, mainWindowHandle);
          await switchToMainWindow(driver, mainWindowHandle);
          await driver.get(urls.omsProductList);
        } catch (navigationError) {
          console.warn(
            "Unable to reload OMS product list after scenario:",
            navigationError,
          );
        }
      }
    }
  } catch (error) {
    console.error("Product test suite failed during setup:", error);
    process.exit(1);
  } finally {
    // await baseTest.teardown();
  }

  console.log("\n=== Product test suite summary ===");
  results.forEach((result) => {
    console.log(
      `- ${result.name}: ${result.status}${result.error ? ` - ${result.error}` : ""}`,
    );
  });

  const failedCount = results.filter(
    (result) => result.status === "failed",
  ).length;
  if (failedCount > 0) {
    console.error(`\n${failedCount} scenario(s) failed.`);
    process.exit(1);
  }
}

runProductTestSuite();

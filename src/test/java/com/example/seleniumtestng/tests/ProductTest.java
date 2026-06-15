package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.BaseTest;
import com.example.seleniumtestng.pages.AssignProductPage;
import com.example.seleniumtestng.pages.ListingProductPage;
import com.example.seleniumtestng.pages.ProductPage;
import com.example.seleniumtestng.utils.AuthHelper;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("null")
public class ProductTest extends BaseTest {
    private static final String PRODUCT_LIST_PATH = "/products?page=1&page_size=50&return_type=list_linked_platform&status_group=all&type_filter=all";
    private static final String REQUIRED_INFO_ERROR = "Vui lòng điền đầy đủ thông tin";

    @BeforeMethod(alwaysRun = true)
    public void loginToOms() {
        driver.get(url("OMS", "/login"));
        AuthHelper.loginOms(driver);
        new org.openqa.selenium.support.ui.WebDriverWait(driver, com.example.seleniumtestng.config.ConfigReader.timeout())
                .until(ExpectedConditions.urlContains("/dashboard"));
        driver.get(url("OMS", PRODUCT_LIST_PATH));
    }

    @Test
    public void createProductWithRequiredFields() {
        String mainWindowHandle = driver.getWindowHandle();
        ProductPage productPage = new ProductPage(driver);
        productPage.accessCreateProduct();
        ProductPage.ProductData product = productPage.inputProductInfo(null, null);
        productPage.selectBrand();
        productPage.selectCategory();
        productPage.fillCostPrice("200000");
        productPage.fillSellPrice("250000");
        productPage.fillWeight("300");
        productPage.fillDimensions("20", "20", "20");
        productPage.enableBatchManagement();
        productPage.fillDescription();
        productPage.submitProduct();
        productPage.searchProduct(product.sku());
        productPage.verifyProductDisplayed(product.sku());

        ListingProductPage listingProductPage = new ListingProductPage(driver);
        listingProductPage.accessListingProduct();
        listingProductPage.selectSalesChannel();
        listingProductPage.selectChannelOption("B2C");
        listingProductPage.clickContinue();
        listingProductPage.clickSellProduct();
        listingProductPage.closeExtraWindows(mainWindowHandle);

        driver.get(url("OPS", "/login"));
        AuthHelper.loginOps(driver);

        AssignProductPage assignProductPage = new AssignProductPage(driver);
        assignProductPage.closeChangePwdModal();
        assignProductPage.closeNotificationModal();
        assignProductPage.openProductsByUserId("294");
        assignProductPage.searchProduct(product.sku());
        assignProductPage.waitForSearchResults();
        assignProductPage.assignProductToWarehouse();
    }

    @DataProvider(name = "invalidProductScenarios")
    public Object[][] invalidProductScenarios() {
        return new Object[][]{
                {"missing SKU", "", "Prod-Auto-Negative", true, true, "200000", "250000", "300", true},
                {"missing product name", "SKU-" + System.currentTimeMillis(), "", true, true, "200000", "250000", "300", true},
                {"missing brand", "SKU-" + System.currentTimeMillis(), "Prod-Auto-Negative", false, true, "200000", "250000", "300", true},
                {"missing category", "SKU-" + System.currentTimeMillis(), "Prod-Auto-Negative", true, false, "200000", "250000", "300", true},
                {"missing cost price", "SKU-" + System.currentTimeMillis(), "Prod-Auto-Negative", true, true, "", "250000", "300", true},
                {"missing sell price", "SKU-" + System.currentTimeMillis(), "Prod-Auto-Negative", true, true, "200000", "", "300", true},
                {"missing weight", "SKU-" + System.currentTimeMillis(), "Prod-Auto-Negative", true, true, "200000", "250000", "", true},
                {"missing dimensions", "SKU-" + System.currentTimeMillis(), "Prod-Auto-Negative", true, true, "200000", "250000", "300", false}
        };
    }

    @Test(dataProvider = "invalidProductScenarios")
    public void createProductValidationScenarios(
            String name,
            String sku,
            String productName,
            boolean selectBrand,
            boolean selectCategory,
            String costPrice,
            String sellPrice,
            String weight,
            boolean fillDimensions) {
        ProductPage productPage = new ProductPage(driver);
        productPage.accessCreateProduct();
        productPage.fillBasicInfo(sku, productName);
        if (selectBrand) {
            productPage.selectBrand();
        }
        if (selectCategory) {
            productPage.selectCategory();
        }
        productPage.fillCostPrice(costPrice);
        productPage.fillSellPrice(sellPrice);
        productPage.fillWeight(weight);
        if (fillDimensions) {
            productPage.fillDimensions("20", "20", "20");
        }
        productPage.fillDescription();
        productPage.submitProduct();

        String errorText = productPage.waitForCreateProductErrorText();
        Assert.assertTrue(
                errorText.contains(REQUIRED_INFO_ERROR),
                name + " should show required information error. Actual: " + errorText);
    }
}

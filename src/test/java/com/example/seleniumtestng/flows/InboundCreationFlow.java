package com.example.seleniumtestng.flows;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.models.InboundPackageData;
import com.example.seleniumtestng.models.InboundProductData;
import com.example.seleniumtestng.pages.inbound.CreateInboundOmsPage;
import com.example.seleniumtestng.utils.Log;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class InboundCreationFlow {
    private final WebDriver driver;

    public InboundCreationFlow(WebDriver driver) {
        this.driver = driver;
    }

    public String createApprovedInbound(List<InboundProductData> products) {
        return createApprovedInboundPackages(List.of(new InboundPackageData(products)));
    }

    public String createApprovedInboundPackages(List<InboundPackageData> packages) {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null || !currentUrl.contains("onflow.vn") || currentUrl.contains("/login")) {
            driver.get(url("OMS", "/auth/login"));
            AuthHelper.loginOms(driver);
        }
        new WebDriverWait(driver, ConfigReader.timeout()).until(ExpectedConditions.urlContains("/dashboard"));

        return createApprovedInboundPackagesFromCurrentSession(packages);
    }

    public String createApprovedInboundPackagesFromCurrentSession(List<InboundPackageData> packages) {
        driver.get(url("OMS", "/inbound/shipments?"));
        CreateInboundOmsPage createInbound = new CreateInboundOmsPage(driver);
        createInbound.openCreateInboundForm();
        createInbound.selectWarehouse(ConfigReader.required("INBOUND_WAREHOUSE_CODE"));
        createInbound.selectSupplier(ConfigReader.required("INBOUND_SUPPLIER"));
        createInbound.inputReference();
        createInbound.continueIfPresent();
        addInboundPackages(createInbound, packages);
        createInbound.continueIfPresent();
        createInbound.confirmCreateInbound();

        String inboundCode = createInbound.getInboundCode();
        if (!inboundCode.matches("NHIV\\d+")) {
            throw new IllegalStateException("Invalid inbound code: " + inboundCode);
        }
        Log.info("Created and approved OMS inbound PO: {}", inboundCode);
        return inboundCode;
    }

    private void addInboundPackages(CreateInboundOmsPage createInbound, List<InboundPackageData> packages) {
        for (int packageIndex = 0; packageIndex < packages.size(); packageIndex++) {
            if (packageIndex > 0) {
                createInbound.addPackageRow();
            }
            createInbound.clickAddProductForPackage(packageIndex);
            addInboundProducts(createInbound, packages.get(packageIndex).getProducts());
            createInbound.confirmItems();
            createInbound.inputPackageDimensions(
                    packageIndex,
                    ConfigReader.requiredInt("INBOUND_LENGTH"),
                    ConfigReader.requiredInt("INBOUND_WIDTH"),
                    ConfigReader.requiredInt("INBOUND_HEIGHT"));
        }
    }

    private void addInboundProducts(CreateInboundOmsPage createInbound, List<InboundProductData> products) {
        for (int index = 0; index < products.size(); index++) {
            if (index > 0) {
                createInbound.addNewProductRow();
            }
            InboundProductData product = products.get(index);
            createInbound.addProductToInbound(product.getSku(), product.getQuantity());
        }
    }

    private String url(String app, String path) {
        return ConfigReader.required(app.toUpperCase() + "_BASE_URL") + path;
    }
}

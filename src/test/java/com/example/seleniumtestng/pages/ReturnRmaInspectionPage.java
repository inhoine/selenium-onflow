package com.example.seleniumtestng.pages;

import org.openqa.selenium.WebDriver;

/**
 * Backward-compatible facade for RMA inspection.
 * Delegates to {@link com.example.seleniumtestng.pages.rma.ReturnRmaInspectionPage}.
 */
public class ReturnRmaInspectionPage extends com.example.seleniumtestng.pages.rma.ReturnRmaInspectionPage {
    public ReturnRmaInspectionPage(WebDriver driver) {
        super(driver);
    }
}

package com.example.seleniumtestng.pages.order;

import com.example.seleniumtestng.pages.BasePage;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object representing the OMS Outbound Order Listing page (/outbound/orders/b2c).
 * Handles order table verification, search, and tracking code extraction.
 */
public class OrderListingOmsPage extends BasePage {

    public OrderListingOmsPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Verifies that the order with the given orderNumber is visible in the OMS orders table.
     */
    public void verifyCreatedOrder(String orderNumber) {
        visible(orderRowLocator(orderNumber));
    }

    /**
     * Checks if the order is visible within the given timeout.
     */
    public boolean isOrderVisible(String orderNumber, long timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.visibilityOfElementLocated(orderRowLocator(orderNumber)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieves the tracking code (mã vận đơn) associated with the given order number.
     * Waits until the tracking code is populated asynchronously.
     */
    public String getTrackingCodeByOrderNumber(String orderNumber) {
        By shipmentTracking = By.xpath(
                "//tr[.//span[contains(@class,'code-text') and normalize-space()=" + xpathText(orderNumber) + "]"
                        + " or .//*[normalize-space()=" + xpathText(orderNumber)
                        + "]]//a[contains(@href,'/outbound/shipments/')]");
        try {
            WebElement trackingCodeEl = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.visibilityOfElementLocated(shipmentTracking));
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> trackingCodeEl.getText() != null && !trackingCodeEl.getText().trim().isBlank());
            return trackingCodeEl.getText().trim();
        } catch (Exception e) {
            By legacyTracking = By.xpath("//a[normalize-space()=" + xpathText(orderNumber)
                    + "]/ancestor::div[contains(@class,'flex-column')]//p[contains(@id,'tracking_code')]");
            WebElement trackingCode = visible(legacyTracking);
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> trackingCode.getText() != null && !trackingCode.getText().trim().isBlank());
            return trackingCode.getText().trim();
        }
    }

    private By orderRowLocator(String orderNumber) {
        return By.xpath(
                "//tr[.//span[contains(@class,'code-text') and normalize-space()=" + xpathText(orderNumber) + "]"
                        + " or .//text()[normalize-space()=" + xpathText(orderNumber) + "]]"
                        + " | //span[contains(@class,'code-text') and normalize-space()=" + xpathText(orderNumber) + "]"
                        + " | //a[contains(@id,'partner_tracking_code') and normalize-space()=" + xpathText(orderNumber)
                        + "]");
    }
}


package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.utils.Log;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ReceivePackingTrolleyPage extends BasePage {
    private final By scanPackingTrolleyField = By.xpath("//input[@placeholder='Quét mã XE/ bảng kê cần đóng gói']");
    private final By receivePackingTrolleyBtn = By.xpath(
            "//button[normalize-space()='Nhận bảng kê' or normalize-space()='Nhan bang ke']");

    public ReceivePackingTrolleyPage(WebDriver driver) {
        super(driver);
    }

    public void receivePackingTrolley(String pickupId) {
        driver.get(ConfigReader.required("WMS_BASE_URL") + "/receive-packing-trolley");
        WebElement input = visible(scanPackingTrolleyField);
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        input.sendKeys(pickupId, Keys.ENTER);
        try {
            WebElement btn = shortWait(1500).until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(receivePackingTrolleyBtn));
            try {
                btn.click();
            } catch (Exception e) {
                jsClick(btn);
            }
        } catch (Exception ignored) {
            // Already submitted by ENTER or button not required
        }
    }

    public boolean verifyToastMessageIfPresent(String message, long timeoutMillis) {
        try {
            String text = shortWait(timeoutMillis)
                    .until(driver -> {
                        List<WebElement> toasts = driver.findElements(By.xpath(
                                "//div[contains(@class,'Toastify__toast-body')]//div[contains(normalize-space(.),'"
                                        + message + "')]"));
                        for (WebElement toast : toasts) {
                            try {
                                if (toast.isDisplayed()) {
                                    return toast.getText();
                                }
                            } catch (RuntimeException ignored) {
                            }
                        }
                        return null;
                    });
            Log.info("Toast detected: {}", text);
            return true;
        } catch (RuntimeException ignored) {
            Log.info("Toast not found, continue: {}", message);
            return false;
        }
    }
}

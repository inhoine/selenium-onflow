package com.example.seleniumtestng.utils;

import com.example.seleniumtestng.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ScanTable extends BasePage {
    private final By tableInput = By.xpath("//input[@placeholder='Quét hoặc nhập mã bàn']");

    public ScanTable(WebDriver driver) {
        super(driver);
    }

    public void scan(String tableCode) {
        WebElement input = visible(tableInput);
        input.clear();
        input.sendKeys(tableCode, Keys.ENTER);
        System.out.println("Scanned table code: " + tableCode);
    }

    public boolean scanIfPresent(String tableCode) {
        try {
            WebElement input = shortWait(2000).until(driver -> {
                for (WebElement element : all(tableInput)) {
                    if (element.isDisplayed() && element.isEnabled()) {
                        return element;
                    }
                }
                return null;
            });
            input.clear();
            input.sendKeys(tableCode, Keys.ENTER);
            System.out.println("Scanned table code: " + tableCode);
            return true;
        } catch (RuntimeException e) {
            System.out.println("Skip table scan because a packing table already appears to be selected");
            return false;
        }
    }
}

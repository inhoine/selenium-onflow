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
}

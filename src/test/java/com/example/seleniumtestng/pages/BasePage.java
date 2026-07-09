package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, ConfigReader.timeout());
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement clickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected List<WebElement> all(By locator) {
        return driver.findElements(locator);
    }

    protected void click(By locator) {
        clickable(locator).click();
    }

    protected void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", element);
    }

    protected void setInputValue(WebElement element, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});"
                        + "const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;"
                        + "setter.call(arguments[0], arguments[1]);"
                        + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                element,
                value);
    }

    protected void setFlatpickrDate(WebElement element, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});"
                        + "if (arguments[0]._flatpickr) {"
                        + "  arguments[0]._flatpickr.set('minDate', null);"
                        + "  arguments[0]._flatpickr.set('maxDate', null);"
                        + "  arguments[0]._flatpickr.setDate(arguments[1], true, 'd-m-Y');"
                        + "}",
                element,
                value);
    }

    protected void type(By locator, String value) {
        WebElement element = visible(locator);
        element.clear();
        if (value != null) {
            element.sendKeys(value);
        }
    }

    protected void clearAndEnter(WebElement element, String value) {
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        element.sendKeys(value, Keys.ENTER);
    }

    protected WebDriverWait shortWait(long millis) {
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofMillis(millis));
        shortWait.pollingEvery(Duration.ofMillis(50));
        return shortWait;
    }
}

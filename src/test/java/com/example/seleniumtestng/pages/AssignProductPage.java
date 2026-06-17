package com.example.seleniumtestng.pages;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AssignProductPage extends BasePage {
    private final By modalChangePwd = By.xpath("//button[.//span[text()='Để sau']]");
    private final By modalNotification = By.xpath("//div[contains(@class,'modal-content')]//button[text()='Đóng']");
    private final By searchResultRows = By.cssSelector("tbody tr");
    private final By noDataMessage = By.xpath("//*[contains(normalize-space(.),'No data found')]");
    private final By checkboxSelectAll = By.cssSelector("input#product-checked-all");
    private final By approveAssignBtn = By.xpath("//button[.//span[text()='Phê duyệt']]");
    private final By confirmApproveBtn = By.xpath("//div[contains(@class,'modal-content')]//button[text()='Phê duyệt']");
    private final By confirmBtn = By.xpath("//div[contains(@class,'modal-content')]//button[text()='Xác nhận']");

    public AssignProductPage(WebDriver driver) {
        super(driver);
    }

    public void closeChangePwdModal() {
        clickOptional(modalChangePwd, 5000);
    }

    public void closeNotificationModal() {
        WebElement button = findOptional(modalNotification, 5000);
        if (button != null) {
            button.click();
            try {
                shortWait(5000).until(ExpectedConditions.stalenessOf(button));
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void openProductsByUserId(String userId) {
        driver.get(com.example.seleniumtestng.config.ConfigReader.required("OPS_BASE_URL") + "/products?user_id=" + userId);
        wait.until(driver -> String.valueOf(driver.getCurrentUrl()).contains("/products"));
        wait.until(driver -> !driver.findElements(By.cssSelector("input")).isEmpty());
    }

    public void searchProduct(String sku) {
        WebElement searchInput = wait.until(driver -> {
            for (By locator : searchInputCandidates()) {
                List<WebElement> elements = driver.findElements(locator);
                for (WebElement element : elements) {
                    try {
                        if (element.isDisplayed()) {
                            return element;
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
            }
            return null;
        });

        searchInput.clear();
        searchInput.sendKeys(sku);
        ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", searchInput);
        searchInput.sendKeys(Keys.ENTER);
    }

    public void waitForSearchResults() {
        wait.until(driver -> {
            for (WebElement row : driver.findElements(searchResultRows)) {
                try {
                    if (row.isDisplayed()) {
                        return true;
                    }
                } catch (RuntimeException ignored) {
                }
            }
            for (WebElement element : driver.findElements(noDataMessage)) {
                try {
                    if (element.isDisplayed()) {
                        return true;
                    }
                } catch (RuntimeException ignored) {
                }
            }
            return false;
        });
    }

    public void assignProductToWarehouse() {
        sleep(1500);
        jsClick(clickable(checkboxSelectAll));
        jsClick(clickable(approveAssignBtn));
        jsClick(visible(confirmApproveBtn));
        WebElement finalConfirm = visible(confirmBtn);
        jsClick(finalConfirm);
        try {
            shortWait(10000).until(ExpectedConditions.stalenessOf(finalConfirm));
        } catch (RuntimeException ignored) {
        }
        sleep(5000);
    }

    private List<By> searchInputCandidates() {
        return List.of(
                By.xpath("//input[contains(@placeholder,'Tìm kiếm') or contains(@placeholder,'tìm kiếm') or contains(@aria-label,'Tìm kiếm') or contains(@aria-label,'tìm kiếm')]"),
                By.cssSelector("input[placeholder='Tìm kiếm...']"),
                By.cssSelector("input[type='search']"),
                By.xpath("//input[contains(@placeholder,'SKU') or contains(@placeholder,'sku') or contains(@aria-label,'SKU') or contains(@aria-label,'sku')]"));
    }

    private void clickOptional(By locator, long timeoutMillis) {
        WebElement element = findOptional(locator, timeoutMillis);
        if (element != null) {
            element.click();
        }
    }

    private WebElement findOptional(By locator, long timeoutMillis) {
        try {
            return new WebDriverWait(driver, Duration.ofMillis(timeoutMillis)).until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting", e);
        }
    }
}

package com.example.seleniumtestng.pages;

import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ListingProductPage extends BasePage {
    private final By listingProductBtn = By.xpath("//a[contains(@id,'listing_product')]");
    private final By channelDropdown = By.xpath("//div[text()='Chọn kênh bán hàng']/ancestor::div[contains(@class,'-control')]");
    private final By continueBtn = By.xpath("//button[contains(.,'Tiếp theo')]");
    private final By createListingStep = By.xpath("//*[contains(normalize-space(.),'Tạo sản phẩm')]");
    private final By loadingText = By.xpath("//*[normalize-space()='Đang tải']");
    private final By sellProductBtn = By.xpath("//button[contains(.,'Đăng bán')]");
    private final By sellProductButtonLike = By.xpath("//*[self::button or @role='button' or contains(@class,'btn')][contains(normalize-space(.),'Đăng bán')]");
    private WebElement lastSalesChannelDropdown;

    public ListingProductPage(WebDriver driver) {
        super(driver);
    }

    public void accessListingProduct() {
        sleep(2000);
        Set<String> originalHandles = driver.getWindowHandles();
        WebElement listingButton = visible(listingProductBtn);
        try {
            listingButton.click();
        } catch (RuntimeException e) {
            jsClick(listingButton);
        }

        try {
            shortWait(10000).until(driver -> driver.getWindowHandles().size() > originalHandles.size());
        } catch (RuntimeException ignored) {
        }

        for (String handle : driver.getWindowHandles()) {
            if (!originalHandles.contains(handle)) {
                driver.switchTo().window(handle);
                System.out.println("Switched to new listing tab");
                return;
            }
        }
        System.out.println("No new tab opened for listing; staying in current tab");
    }

    public void selectSalesChannel() {
        lastSalesChannelDropdown = visibleFirst(
                channelDropdown,
                By.xpath("//div[contains(normalize-space(.),'Chọn kênh bán hàng')]/ancestor::div[contains(@class,'-control')][1]"),
                By.xpath("//input[contains(@id,'react-select')]/ancestor::div[contains(@class,'-control')][1]"));
        try {
            lastSalesChannelDropdown.click();
        } catch (RuntimeException e) {
            jsClick(lastSalesChannelDropdown);
        }
        sleep(500);
        System.out.println("Clicked sales channel dropdown");
    }

    public void selectChannelOption(String channelName) {
        By optionLocator = By.xpath("//div[contains(@class,'-menu')]//div[contains(@class,'-option') and contains(.,'" + channelName + "')]");
        WebElement option = findVisibleOption(optionLocator, 3000);
        if (option == null) {
            typeChannelName(channelName);
            option = findVisibleOption(optionLocator, 5000);
        }

        if (option != null) {
            jsClick(option);
        } else {
            driver.switchTo().activeElement().sendKeys(Keys.ENTER);
        }
        System.out.println("Selected channel: " + channelName);
    }

    public void clickContinue() {
        WebElement continueButton = clickable(continueBtn);
        try {
            continueButton.click();
        } catch (RuntimeException e) {
            jsClick(continueButton);
        }
        visible(createListingStep);
        waitForListingForm();
    }

    public void clickSellProduct() {
        WebElement sellButton = waitForSellButton(60000);
        if (sellButton == null) {
            driver.navigate().refresh();
            visible(createListingStep);
            sellButton = waitForSellButton(60000);
        }
        if (sellButton == null) {
            throw new IllegalStateException("Không tìm thấy nút Đăng bán trên trang listing. URL hiện tại: " + driver.getCurrentUrl());
        }
        try {
            sellButton.click();
        } catch (RuntimeException e) {
            jsClick(sellButton);
        }
        sleep(2000);
    }

    public void closeExtraWindows(String mainWindowHandle) {
        List<String> handles = List.copyOf(driver.getWindowHandles());
        for (String handle : handles) {
            if (!handle.equals(mainWindowHandle)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(mainWindowHandle);
    }

    private WebElement visibleFirst(By... locators) {
        return wait.until(driver -> {
            for (By locator : locators) {
                for (WebElement element : driver.findElements(locator)) {
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
    }

    private WebElement findVisibleOption(By locator, long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(locator)) {
                    try {
                        if (element.isDisplayed()) {
                            return element;
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void waitForListingForm() {
        waitForSellButton(60000);
    }

    private WebElement waitForSellButton(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                WebElement button = firstDisplayed(sellProductBtn, sellProductButtonLike);
                if (button != null && button.isEnabled()) {
                    return button;
                }
                if (isLoadingVisible()) {
                    return null;
                }
                return button;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private WebElement firstDisplayed(By... locators) {
        for (By locator : locators) {
            for (WebElement element : driver.findElements(locator)) {
                try {
                    if (element.isDisplayed()) {
                        return element;
                    }
                } catch (RuntimeException ignored) {
                }
            }
        }
        return null;
    }

    private boolean isLoadingVisible() {
        for (WebElement element : driver.findElements(loadingText)) {
            try {
                if (element.isDisplayed()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private void typeChannelName(String channelName) {
        WebElement input = null;
        if (lastSalesChannelDropdown != null) {
            List<WebElement> inputs = lastSalesChannelDropdown.findElements(By.cssSelector("input"));
            if (!inputs.isEmpty()) {
                input = inputs.get(0);
            }
        }
        if (input == null) {
            input = driver.switchTo().activeElement();
        }
        input.sendKeys(channelName);
        sleep(500);
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

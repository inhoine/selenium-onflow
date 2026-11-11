package com.example.seleniumtestng.pages.inbound;

import com.example.seleniumtestng.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Handles serial scanning modal interactions during WMS Inbound Inspection:
 * - Scanning serials for products requiring serial numbers
 * - Adding duplicate serials for negative verification
 * - Verifying serial table updates and confirming modal
 */
public class InboundSerialModal extends BasePage {

    private final By scanSerialBtn = By.xpath("//button[contains(.,'Quét mã serial') or contains(.,'Quet ma serial')]");
    private final By serialModal = By.xpath(
            "//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]");
    private final By serialInput = By.xpath(
            "//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]//input[contains(@placeholder,'serial') or contains(@placeholder,'Serial')]");
    private final By addSerialBtn = By.xpath(
            "//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]//button[contains(.,'Thêm') or contains(.,'Them')]");
    private final By confirmSerialBtn = By.xpath(
            "//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");
    private final By serialRows = By.xpath(
            "//*[contains(@class,'modal') and .//*[contains(.,'Quét serial') or contains(.,'Quet serial')]]//tbody/tr");

    public InboundSerialModal(WebDriver driver) {
        super(driver);
    }

    public void inputSerialsIfPresent(int quantity, String sku) {
        inputSerialsIfPresent(quantity, sku, Math.max(1, quantity));
    }

    public void inputSerialsIfPresent(int quantity, String sku, int serialCount) {
        WebElement button = firstVisibleEnabled(scanSerialBtn);
        if (button == null) {
            return;
        }

        openSerialModal(button);
        for (int index = 1; index <= serialCount; index++) {
            addSerialToOpenModal(serialValue(sku, index), true);
        }
        if (serialCount >= Math.max(1, quantity)) {
            confirmSerialModal();
        } else {
            confirmSerialModalIfPossible();
        }
    }

    public void inputDuplicateSerialsIfPresent(String sku) {
        WebElement button = firstVisibleEnabled(scanSerialBtn);
        if (button == null) {
            return;
        }

        openSerialModal(button);
        String serial = serialValue(sku, 1);
        addSerialToOpenModal(serial, true);
        addSerialToOpenModal(serial, false);
        clickVisibleEnabled(confirmSerialBtn);
        try {
            shortWait(2000).until(ExpectedConditions.invisibilityOfElementLocated(serialModal));
        } catch (RuntimeException ignored) {
        }
    }

    public boolean isSerialModalVisible() {
        return firstVisibleEnabled(serialModal) != null;
    }

    public void openSerialModal(WebElement button) {
        jsClick(button);
        visible(serialModal);
    }

    public void addSerialToOpenModal(String serial, boolean waitForAdd) {
        int rowCountBeforeAdd = visibleCount(serialRows);
        WebElement input = visible(serialInput);
        setInputValue(input, serial);
        click(addSerialBtn);
        if (waitForAdd) {
            waitUntilSerialAdded(serial, rowCountBeforeAdd);
        } else {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void confirmSerialModal() {
        clickVisibleEnabled(confirmSerialBtn);
        shortWait(3000).until(ExpectedConditions.invisibilityOfElementLocated(serialModal));
    }

    public void confirmSerialModalIfPossible() {
        clickVisibleEnabled(confirmSerialBtn);
        try {
            shortWait(2000).until(ExpectedConditions.invisibilityOfElementLocated(serialModal));
        } catch (RuntimeException ignored) {
        }
    }

    public String serialValue(String sku, int index) {
        return "SN" + (System.currentTimeMillis() % 10000000) + String.format("%02d", index);
    }

    private void waitUntilSerialAdded(String serial, int previousRowCount) {
        shortWait(3000).until(driver -> {
            WebElement input = firstVisibleEnabled(serialInput);
            boolean inputCleared = input != null && inputValue(input).isBlank();
            boolean rowAdded = visibleCount(serialRows) > previousRowCount;
            boolean serialVisible = visibleText(serialModal).contains(serial);
            return inputCleared || rowAdded || serialVisible ? true : null;
        });
    }

    private void clickVisibleEnabled(By locator) {
        WebElement element = shortWait(3000).until(driver -> {
            WebElement candidate = firstVisibleEnabled(locator);
            return candidate == null ? null : candidate;
        });
        jsClick(element);
    }

    private int visibleCount(By locator) {
        int count = 0;
        for (WebElement element : all(locator)) {
            if (isDisplayed(element)) {
                count++;
            }
        }
        return count;
    }

    private String visibleText(By locator) {
        for (WebElement element : all(locator)) {
            if (isDisplayed(element)) {
                return element.getText();
            }
        }
        return "";
    }

    private WebElement firstVisibleEnabled(By locator) {
        for (WebElement element : all(locator)) {
            if (isDisplayed(element) && element.isEnabled()) {
                return element;
            }
        }
        return null;
    }

    private boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String inputValue(WebElement input) {
        try {
            String value = input.getDomProperty("value");
            return value == null ? "" : value.trim();
        } catch (RuntimeException e) {
            return "";
        }
    }
}


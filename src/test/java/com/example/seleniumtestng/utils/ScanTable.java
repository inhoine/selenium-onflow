package com.example.seleniumtestng.utils;

import com.example.seleniumtestng.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ScanTable extends BasePage {
    private final By tableInput = By.xpath("//input[@placeholder='Quét hoặc nhập mã bàn']");
    private final By visibleModal = By.cssSelector(".modal.show");

    public ScanTable(WebDriver driver) {
        super(driver);
    }

    public void scan(String tableCode) {
        WebElement input = wait.until(driver -> findTableOrStationInputNow());
        scanIntoTableOrStationInput(input, tableCode);
    }

    public boolean scanIfPresent(String tableCode) {
        try {
            WebElement input = shortWait(2000).until(driver -> findTableOrStationInputNow());
            scanIntoTableOrStationInput(input, tableCode);
            return true;
        } catch (RuntimeException e) {
            System.out.println("Skip table scan because a packing table already appears to be selected");
            return false;
        }
    }

    private WebElement findTableOrStationInputNow() {
        for (WebElement element : all(tableInput)) {
            try {
                if (element.isDisplayed() && element.isEnabled()) {
                    return element;
                }
            } catch (RuntimeException ignored) {
            }
        }

        for (WebElement element : driver.findElements(By.cssSelector("input"))) {
            try {
                if (!element.isDisplayed() || !element.isEnabled()) {
                    continue;
                }
                String placeholder = normalizeForMatch(element.getAttribute("placeholder"));
                if ((placeholder.contains("ma ban") || placeholder.contains("ma tram") || placeholder.contains("tram"))
                        && !placeholder.contains("bang ke")) {
                    return element;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private void scanIntoTableOrStationInput(WebElement input, String tableCode) {
        input.clear();
        input.sendKeys(tableCode, Keys.ENTER);
        WebElement connectButton = findConnectTableOrStationButton(1000);
        if (connectButton != null) {
            jsClick(connectButton);
        }
        waitForTableOrStationModalToClose();
        System.out.println("Scanned table/station code: " + tableCode);
    }

    private WebElement findConnectTableOrStationButton(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement button : driver.findElements(By.cssSelector(".modal.show button"))) {
                    try {
                        if (!button.isDisplayed() || !button.isEnabled()) {
                            continue;
                        }
                        String text = normalizeForMatch(button.getText());
                        if (text.contains("ket noi tram dong hang")
                                || text.contains("ket noi ban dong goi")
                                || text.equals("xac nhan")) {
                            return button;
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

    private void waitForTableOrStationModalToClose() {
        try {
            shortWait(5000).until(driver -> {
                for (WebElement modal : driver.findElements(visibleModal)) {
                    try {
                        String text = normalizeForMatch(modal.getText());
                        if (modal.isDisplayed()
                                && (text.contains("quet ma tram dong hang")
                                || text.contains("ma tram dong hang")
                                || text.contains("ma ban"))) {
                            return null;
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
                return true;
            });
        } catch (RuntimeException ignored) {
            System.out.println("Table/station scan modal is still visible after scan attempt");
        }
    }
}

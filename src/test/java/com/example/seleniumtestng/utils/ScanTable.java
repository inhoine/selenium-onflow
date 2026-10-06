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
        if (!scanIntoTableOrStationInput(input, tableCode)) {
            throw new IllegalStateException("Table/station scan modal did not close after scanning code: " + tableCode);
        }
    }

    public boolean scanIfPresent(String tableCode) {
        try {
            WebElement input = shortWait(800).until(driver -> findTableOrStationInputNow());
            if (!scanIntoTableOrStationInput(input, tableCode)) {
                throw new IllegalStateException("Table/station scan modal did not close after scanning code: " + tableCode);
            }
            return true;
        } catch (RuntimeException e) {
            if (isTableOrStationModalVisible()) {
                throw e;
            }
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

    private boolean scanIntoTableOrStationInput(WebElement input, String tableCode) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                input = findTableOrStationInputNow();
                if (input == null) {
                    return !isTableOrStationModalVisible();
                }
                input.click();
                input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
                input.sendKeys(tableCode, Keys.ENTER);
                if (waitForTableOrStationModalToClose()) {
                    System.out.println("Scanned table/station code: " + tableCode);
                    return true;
                }

                input = findTableOrStationInputNow();
                if (input != null) {
                    setInputValue(input, tableCode);
                    input.sendKeys(Keys.ENTER);
                    WebElement connectButton = findConnectTableOrStationButton(500);
                    if (connectButton != null) {
                        jsClick(connectButton);
                    }
                    if (waitForTableOrStationModalToClose()) {
                        System.out.println("Scanned table/station code: " + tableCode);
                        return true;
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        System.out.println("Table/station scan modal is still visible after scan attempt");
        return false;
    }

    private WebElement findConnectTableOrStationButton(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement button : driver.findElements(By.cssSelector(".modal.show button, .modal.show [role='button']"))) {
                    try {
                        if (!button.isDisplayed() || !button.isEnabled()) {
                            continue;
                        }
                        String text = normalizeForMatch(button.getText());
                        if (text.contains("ket noi tram dong hang")
                                || text.contains("ket noi ban dong goi")
                                || text.contains("quet ma")
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

    private boolean waitForTableOrStationModalToClose() {
        try {
            return shortWait(2500).until(driver -> !isTableOrStationModalVisible() ? true : null);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isTableOrStationModalVisible() {
        for (WebElement modal : driver.findElements(visibleModal)) {
            try {
                String text = normalizeForMatch(modal.getText());
                if (modal.isDisplayed()
                        && (text.contains("quet ma tram dong hang")
                        || text.contains("ma tram dong hang")
                        || text.contains("quet ma ban")
                        || text.contains("ma ban kiem hang")
                        || text.contains("ma ban"))) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }
}

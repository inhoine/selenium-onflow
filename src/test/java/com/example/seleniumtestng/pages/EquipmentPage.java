package com.example.seleniumtestng.pages;

import java.text.Normalizer;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class EquipmentPage extends BasePage {
    private final By addEquipmentBtn = By.xpath("//button[normalize-space()='Thêm thiết bị chứa hàng' or normalize-space()='Them thiet bi chua hang']");
    private final By equipmentCodeField = By.xpath("//div[contains(.,'Thêm thiết bị mới') or contains(.,'Them thiet bi moi')]//input[contains(@placeholder,'mã thiết bị') or contains(@placeholder,'ma thiet bi')]");
    private final By equipmentGroupDropdown = By.xpath("//div[normalize-space()='Chọn nhóm thiết bị' or normalize-space()='Chon nhom thiet bi']");
    private final By equipmentTypeDropdown = By.xpath("//div[normalize-space()='Chọn loại thiết bị' or normalize-space()='Chon loai thiet bi']");
    private final By basketSizeDropdown = By.xpath("//*[normalize-space()='Chọn kích thước rổ' or normalize-space()='Chon kich thuoc ro' or normalize-space()='Chọn kích thước' or normalize-space()='Chon kich thuoc']/ancestor::div[contains(@class,'-control')][1]");
    private final By submitEquipmentBtn = By.cssSelector("button[type='submit']");

    public EquipmentPage(WebDriver driver) {
        super(driver);
    }

    public String addEquipment(String equipmentGroupName, String equipmentTypeName) {
        return addEquipment(equipmentGroupName, equipmentTypeName, null);
    }

    public String addEquipment(String equipmentGroupName, String equipmentTypeName, String equipmentSizeName) {
        String equipmentCode = "THIET-BI-" + (System.currentTimeMillis() % 1000);
        click(addEquipmentBtn);
        type(equipmentCodeField, equipmentCode);
        click(equipmentGroupDropdown);
        selectVisibleOption(equipmentGroupName);
        click(equipmentTypeDropdown);
        selectVisibleOption(equipmentTypeName);
        if (equipmentSizeName != null && !equipmentSizeName.trim().isEmpty()) {
            click(basketSizeDropdown);
            selectVisibleOption(equipmentSizeName);
        }
        click(submitEquipmentBtn);
        return equipmentCode;
    }

    public String waitForToast(String message) {
        By toast = By.xpath("//div[contains(@class,'Toastify__toast-body')]//div[contains(normalize-space(.)," + xpathText(message) + ")]");
        return visible(toast).getText().trim();
    }

    public String waitForAnyToast() {
        By toast = By.xpath("//div[contains(@class,'Toastify__toast-body')]//div[string-length(normalize-space(.)) > 0]");
        return visible(toast).getText().trim();
    }

    private void selectVisibleOption(String value) {
        WebElement option = findVisibleOption(value);
        if (option == null) {
            throw new IllegalStateException("Không tìm thấy option thiết bị: " + value
                    + ". Available: " + availableOptionsText());
        }
        jsClick(option);
    }

    private WebElement findVisibleOption(String value) {
        try {
            return shortWait(5000).until(driver -> {
                WebElement exactOption = findOption(value, true);
                return exactOption == null ? findOption(value, false) : exactOption;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private WebElement findOption(String value, boolean exact) {
        String expected = normalizeForMatch(value);
        List<WebElement> options = all(By.xpath("//*[contains(@class,'-menu')]//*[contains(@class,'-option') or @role='option']"));
        for (WebElement option : options) {
            try {
                if (!option.isDisplayed()) {
                    continue;
                }
                String actual = normalizeForMatch(option.getText());
                if (exact ? actual.equals(expected) : actual.contains(expected)) {
                    return option;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private String availableOptionsText() {
        StringBuilder builder = new StringBuilder();
        for (WebElement option : all(By.xpath("//*[contains(@class,'-menu')]//*[contains(@class,'-option') or @role='option']"))) {
            try {
                if (option.isDisplayed()) {
                    if (builder.length() > 0) {
                        builder.append(" | ");
                    }
                    builder.append(option.getText().trim());
                }
            } catch (RuntimeException ignored) {
            }
        }
        return builder.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase()
                .trim();
    }
}

package com.example.seleniumtestng.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class EquipmentPage extends BasePage {
    private final By addEquipmentBtn = By.xpath("//button[normalize-space()='Thêm thiết bị chứa hàng' or normalize-space()='Them thiet bi chua hang']");
    private final By equipmentCodeField = By.xpath("//div[contains(.,'Thêm thiết bị mới') or contains(.,'Them thiet bi moi')]//input[contains(@placeholder,'mã thiết bị') or contains(@placeholder,'ma thiet bi')]");
    private final By equipmentGroupDropdown = By.xpath("//div[normalize-space()='Chọn nhóm thiết bị' or normalize-space()='Chon nhom thiet bi']");
    private final By equipmentTypeDropdown = By.xpath("//div[normalize-space()='Chọn loại thiết bị' or normalize-space()='Chon loai thiet bi']");
    private final By submitEquipmentBtn = By.cssSelector("button[type='submit']");

    public EquipmentPage(WebDriver driver) {
        super(driver);
    }

    public String addEquipment(String equipmentGroupName, String equipmentTypeName) {
        String equipmentCode = "THIET-BI-" + (System.currentTimeMillis() % 100000);
        click(addEquipmentBtn);
        type(equipmentCodeField, equipmentCode);
        click(equipmentGroupDropdown);
        jsClick(visible(exactOption(equipmentGroupName)));
        click(equipmentTypeDropdown);
        jsClick(visible(exactOption(equipmentTypeName)));
        click(submitEquipmentBtn);
        return equipmentCode;
    }

    public String waitForToast(String message) {
        By toast = By.xpath("//div[contains(@class,'Toastify__toast-body')]//div[contains(normalize-space(.),'" + message + "')]");
        return visible(toast).getText().trim();
    }

    private By exactOption(String value) {
        return By.xpath("//*[contains(@class,'-menu')]//*[normalize-space(.)='" + value + "']");
    }
}

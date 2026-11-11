package com.example.seleniumtestng.pages.packing;

import com.example.seleniumtestng.pages.BasePage;
import com.example.seleniumtestng.utils.Log;
import java.text.Normalizer;
import java.util.Locale;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PackagingMaterialHelper extends BasePage {
    private static final long CUSTOMER_MATERIAL_TOAST_TIMEOUT_MS = 800;
    private static final String CUSTOMER_PACKING_MATERIAL_FALLBACK_CODE = "10x10x10";

    private final By packagingMaterialsField = By.xpath("//input[@placeholder='Quét hoặc nhập mã vật liệu đóng gói']");
    private final By fallbackPackagingMaterial = By.xpath(
            "//*[contains(normalize-space(.),'Băng keo') or contains(normalize-space(.),'Bang keo')]");
    private final AttachedDocumentModal attachedDocumentModal;

    public PackagingMaterialHelper(WebDriver driver, AttachedDocumentModal attachedDocumentModal) {
        super(driver);
        this.attachedDocumentModal = attachedDocumentModal;
    }

    public void scanPackagingMaterial(String materialCode) {
        scanOrClickPackagingMaterial(materialCode);
        if (isCustomerPackingMaterialToastPresent()
                && !CUSTOMER_PACKING_MATERIAL_FALLBACK_CODE.equalsIgnoreCase(materialCode)) {
            Log.info("Packing material {} is rejected because the order requires customer packing material. Retry with {}",
                    materialCode, CUSTOMER_PACKING_MATERIAL_FALLBACK_CODE);
            scanOrClickPackagingMaterial(CUSTOMER_PACKING_MATERIAL_FALLBACK_CODE);
        }
        attachedDocumentModal.confirmAttachedDocumentsPrintedIfPresent();
    }

    public boolean isPackagingMaterialPromptVisibleNow() {
        for (WebElement element : driver.findElements(packagingMaterialsField)) {
            try {
                if (element.isDisplayed() && element.isEnabled()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private void scanOrClickPackagingMaterial(String materialCode) {
        WebElement input = findVisiblePackagingMaterialInput(2500);
        if (input == null) {
            clickFallbackPackagingMaterial(materialCode);
            return;
        }
        clearAndEnter(input, materialCode);
    }

    private WebElement findVisiblePackagingMaterialInput(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : all(packagingMaterialsField)) {
                    try {
                        if (element.isDisplayed() && element.isEnabled()) {
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

    private void clickFallbackPackagingMaterial(String materialCode) {
        WebElement material = findVisible(By.xpath("//*[contains(normalize-space(.),'" + materialCode + "')]"), 1000);
        if (material == null) {
            material = visible(fallbackPackagingMaterial);
        }
        jsClick(material);
    }

    private boolean isCustomerPackingMaterialToastPresent() {
        try {
            return shortWait(CUSTOMER_MATERIAL_TOAST_TIMEOUT_MS).until(driver -> {
                for (WebElement toast : driver.findElements(By.xpath("//div[contains(@class,'Toastify__toast-body')]"))) {
                    try {
                        if (!toast.isDisplayed()) {
                            continue;
                        }
                        String normalizedText = normalizedToastText(toast.getText());
                        if (normalizedText.contains("don hang yeu cau nvl dong goi cua khach hang")
                                || (normalizedText.contains("nvl")
                                && normalizedText.contains("dong goi")
                                && normalizedText.contains("khach hang"))) {
                            return true;
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private WebElement findVisible(By locator, long timeoutMillis) {
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

    private String normalizedToastText(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}


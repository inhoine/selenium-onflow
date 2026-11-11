package com.example.seleniumtestng.pages.packing;

import com.example.seleniumtestng.pages.BasePage;
import com.example.seleniumtestng.utils.Log;
import java.text.Normalizer;
import java.util.Locale;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AttachedDocumentModal extends BasePage {
    private static final long ATTACHED_DOCUMENT_CONFIRM_TIMEOUT_MS = 1200;
    private static final long ATTACHED_DOCUMENT_CLOSE_TIMEOUT_MS = 3000;

    private final By visibleModal = By.cssSelector(".modal.show");
    private final By visibleModalCloseButton = By.xpath(
            "//*[contains(@class,'modal') and contains(@class,'show')]//button[contains(@class,'btn-close') or @aria-label='Close' or normalize-space()='×' or normalize-space()='Đóng' or normalize-space()='Dong']");

    public AttachedDocumentModal(WebDriver driver) {
        super(driver);
    }

    public void confirmAttachedDocumentsPrintedIfPresent() {
        WebElement confirmButton = findAttachedDocumentsPrintedConfirmButton(ATTACHED_DOCUMENT_CONFIRM_TIMEOUT_MS);
        if (confirmButton == null) {
            return;
        }

        jsClick(confirmButton);
        Log.info("Confirmed all attached documents were printed");
        try {
            shortWait(ATTACHED_DOCUMENT_CLOSE_TIMEOUT_MS)
                    .until(driver -> !isAttachedDocumentsModalVisibleNow() ? true : null);
        } catch (RuntimeException ignored) {
            Log.warn("Attached documents modal is still visible after confirming printed documents");
        }
    }

    public void closeBlockingModalIfPresent() {
        if (findVisible(visibleModal, 500) == null) {
            return;
        }

        if (!clickVisibleModalCloseButton()) {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        }

        try {
            shortWait(3000).until(driver -> driver.findElements(visibleModal).stream()
                    .noneMatch(element -> {
                        try {
                            return element.isDisplayed();
                        } catch (RuntimeException ignored) {
                            return false;
                        }
                    }));
        } catch (RuntimeException ignored) {
            Log.info("Modal still visible after close attempt; continue with input wait");
        }
    }

    public boolean isAttachedDocumentsModalVisibleNow() {
        for (WebElement modal : driver.findElements(visibleModal)) {
            try {
                if (modal.isDisplayed() && isAttachedDocumentsModal(modal)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private boolean isAttachedDocumentsModal(WebElement modal) {
        String text = normalizeText(modal.getText());
        return text.contains("danh sach tai lieu dinh kem")
                || text.contains("vui long in tat ca cac tai lieu dinh kem");
    }

    private WebElement findAttachedDocumentsPrintedConfirmButton(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> findAttachedDocumentsPrintedConfirmButtonNow());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private WebElement findAttachedDocumentsPrintedConfirmButtonNow() {
        for (WebElement modal : driver.findElements(visibleModal)) {
            try {
                if (!modal.isDisplayed() || !isAttachedDocumentsModal(modal)) {
                    continue;
                }
                for (WebElement button : modal.findElements(By.cssSelector("button"))) {
                    if (!button.isDisplayed() || !button.isEnabled()) {
                        continue;
                    }
                    if (normalizeText(button.getText()).contains("xac nhan da in het")) {
                        return button;
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private boolean clickVisibleModalCloseButton() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            WebElement closeButton = findVisible(visibleModalCloseButton, 1000);
            if (closeButton == null) {
                return false;
            }
            try {
                jsClick(closeButton);
                return true;
            } catch (StaleElementReferenceException ignored) {
                if (findVisible(visibleModal, 200) == null) {
                    return true;
                }
            }
        }
        return false;
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

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}


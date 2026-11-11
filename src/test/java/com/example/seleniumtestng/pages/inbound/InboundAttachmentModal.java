package com.example.seleniumtestng.pages.inbound;

import com.example.seleniumtestng.pages.BasePage;
import java.util.function.BooleanSupplier;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Handles damaged/redline photo attachment modal interactions during WMS Inbound Inspection:
 * - Detecting if the attachment / proof upload modal appears
 * - Scrolling to bottom and skipping attachment submission if optional
 * - Handling confirmation dialogs
 */
public class InboundAttachmentModal extends BasePage {

    private final By attachmentModal = By.xpath(
            "//*[.//button[contains(.,'Bỏ qua') or contains(.,'Bo qua')] and (contains(@class,'modal') or @role='dialog')]");
    private final By skipAttachmentBtn = By
            .xpath("//button[contains(normalize-space(.),'Bỏ qua') or contains(normalize-space(.),'Bo qua')]");
    private final By confirmSkipAttachmentBtn = By
            .xpath("//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");

    public InboundAttachmentModal(WebDriver driver) {
        super(driver);
    }

    public boolean isAttachmentModalVisible() {
        for (WebElement element : all(attachmentModal)) {
            if (isDisplayed(element)) {
                return true;
            }
        }
        return false;
    }

    public boolean skipAttachmentIfPresent() {
        if (!isAttachmentModalVisible()) {
            return false;
        }
        WebElement skipButton = firstVisibleEnabled(skipAttachmentBtn);
        if (skipButton == null) {
            scrollAttachmentModalToBottom();
            skipButton = firstVisibleEnabled(skipAttachmentBtn);
        }
        if (skipButton == null) {
            return false;
        }
        jsClick(skipButton);
        confirmSkipAttachmentIfPresent();
        return true;
    }

    public boolean waitUntilAttachmentModalClosed(
            BooleanSupplier isPostInspectReady,
            BooleanSupplier clickBackAfterSubmittedDetailIfPresent,
            long attachmentWaitMs) {
        try {
            shortWait(attachmentWaitMs).until(driver -> !isAttachmentModalVisible() ? true : null);
            boolean[] clickedBack = { false };
            return shortWait(attachmentWaitMs).until(driver -> {
                if (isPostInspectReady != null && isPostInspectReady.getAsBoolean()) {
                    return true;
                }
                if (!clickedBack[0] && clickBackAfterSubmittedDetailIfPresent != null && clickBackAfterSubmittedDetailIfPresent.getAsBoolean()) {
                    clickedBack[0] = true;
                }
                return null;
            });
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void scrollAttachmentModalToBottom() {
        for (WebElement element : all(attachmentModal)) {
            if (!isDisplayed(element)) {
                continue;
            }
            try {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollTop = arguments[0].scrollHeight;"
                                + "arguments[0].querySelectorAll('*').forEach(function(el) {"
                                + "  if (el.scrollHeight > el.clientHeight) el.scrollTop = el.scrollHeight;"
                                + "});",
                        element);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void confirmSkipAttachmentIfPresent() {
        try {
            clickConfirmDialogIfPresent();
        } catch (RuntimeException ignored) {
        }
    }

    public void clickConfirmDialogIfPresent() {
        WebElement confirmButton = shortWait(900).until(driver -> {
            WebElement button = firstVisibleEnabled(confirmSkipAttachmentBtn);
            return button == null ? null : button;
        });
        jsClick(confirmButton);
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
}


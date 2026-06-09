package com.example.seleniumtestng.pages;

import com.example.seleniumtestng.config.ConfigReader;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginPage extends BasePage {
    private final By emailInput = By.cssSelector("input[type='email'], input[placeholder*='email']");
    private final By passwordInput = By.cssSelector("input[type='password']");
    private final By submitButton = By.cssSelector("button[type='submit']");
    private final By continueLoginButton = By.xpath("//button[contains(normalize-space(.),'Tiếp tục đăng nhập')]");
    private final By confirmFcButton = By.xpath("//button[contains(normalize-space(.),'Bạn đã chọn FC') or contains(normalize-space(.),'Xác nhận')]");
    private final By logoutButton = By.xpath("//button[contains(normalize-space(.),'Đăng xuất') or contains(normalize-space(.),'Dang xuat')]");
    private final By errorMessages = By.xpath("//*[contains(@class,'ant-form-item-explain') or contains(@class,'error') or contains(@class,'invalid-feedback') or @role='alert'][normalize-space(string())!='']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void openWmsLogin() {
        driver.get(ConfigReader.required("WMS_BASE_URL") + "/login");
        waitForLoginForm();
    }

    public void waitForLoginForm() {
        visible(emailInput);
        visible(passwordInput);
    }

    public void login(String email, String password) {
        type(emailInput, email);
        type(passwordInput, password);
        click(submitButton);
    }

    public void continueLoginIfNeeded() {
        try {
            WebElement button = shortWait(5000).until(ExpectedConditions.elementToBeClickable(continueLoginButton));
            try {
                button.click();
            } catch (RuntimeException e) {
                jsClick(button);
            }
            System.out.println("Clicked continue login button");
        } catch (TimeoutException ignored) {
            System.out.println("Continue login modal not shown");
        }
    }

    public void selectFc() {
        String fcName = ConfigReader.getOrDefault("DEFAULT_FC_NAME", "FC HN");
        By fcOption = By.xpath("//span[normalize-space()='" + fcName + "'] | //*[normalize-space()='" + fcName + "']");
        WebElement warehouse = wait.until(ExpectedConditions.visibilityOfElementLocated(fcOption));
        clickWarehouseAndConfirm(warehouse);
    }

    public boolean selectFcIfPresent(long timeoutMillis) {
        String fcName = ConfigReader.getOrDefault("DEFAULT_FC_NAME", "FC HN");
        By fcOption = By.xpath("//span[normalize-space()='" + fcName + "'] | //*[normalize-space()='" + fcName + "']");
        try {
            WebElement warehouse = shortWait(timeoutMillis).until(ExpectedConditions.visibilityOfElementLocated(fcOption));
            clickWarehouseAndConfirm(warehouse);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public void waitForLoginSuccess() {
        wait.withTimeout(Duration.ofSeconds(30)).until(driver -> {
            String url = driver.getCurrentUrl();
            if (url.contains("/dashboard") || url.contains("/home") || url.contains("/warehouse") || url.contains("/user-setting")) {
                return true;
            }
            return !driver.findElements(logoutButton).isEmpty();
        });
    }

    public void waitForLoginFailure() {
        wait.until(driver -> driver.getCurrentUrl().contains("/login") || isLoginFormVisible());
    }

    public boolean isLoginFormVisible() {
        List<WebElement> emails = all(emailInput);
        List<WebElement> passwords = all(passwordInput);
        return !emails.isEmpty() && !passwords.isEmpty() && emails.get(0).isDisplayed() && passwords.get(0).isDisplayed();
    }

    public String getLoginErrorText() {
        for (WebElement element : all(errorMessages)) {
            try {
                String text = element.getText().trim();
                if (element.isDisplayed() && !text.isEmpty() && text.length() < 300) {
                    return text;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return "";
    }

    private void clickWarehouseAndConfirm(WebElement warehouse) {
        try {
            warehouse.click();
        } catch (RuntimeException e) {
            jsClick(warehouse);
        }

        WebElement confirm = wait.until(ExpectedConditions.visibilityOfElementLocated(confirmFcButton));
        try {
            confirm.click();
        } catch (RuntimeException e) {
            jsClick(confirm);
        }
    }
}

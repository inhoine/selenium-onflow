const { By, until } = require("selenium-webdriver");

class LoginPage {
  constructor(driver) {
    this.driver = driver;
  }

  url = "https://stg-wms.onflow.vn/login";

  usenameInput = By.css("input[placeholder='Nhập email'], input[type='email']");
  passwordInput = By.css(
    "input[placeholder='Nhập mật khẩu'], input[type='password']",
  );
  loginButton = By.css("button[type='submit']");
  continueLoginButton = By.xpath("//button[contains(.,'Tiếp tục đăng nhập')]");
  fcOption = By.xpath("//span[text()='FC HN']");
  confirmFcButton = By.xpath(
    "//button[contains(.,'Bạn đã chọn FC') or contains(.,'Xác nhận')]",
  );
  logoutButton = By.xpath("//button[contains(.,'Đăng xuất')]");
  loginFormLocator = By.xpath(
    "//form[.//input[@placeholder='Nhập email'] or .//input[@placeholder='Nhập mật khẩu']]",
  );
  loginErrorMessageLocator = By.xpath(
    ".//*[contains(@class,'ant-form-item-explain') or contains(@class,'error') or contains(@class,'invalid-feedback') or contains(@role,'alert') or contains(@data-testid,'message')][normalize-space(string())!='']",
  );

  async open() {
    await this.driver.get(this.url);
    await this.waitForLoginForm();
  }

  async waitForLoginForm() {
    const emailField = await this.driver.wait(
      until.elementLocated(this.usenameInput),
      20000,
    );
    await this.driver.wait(until.elementIsVisible(emailField), 20000);
    const passwordField = await this.driver.wait(
      until.elementLocated(this.passwordInput),
      20000,
    );
    await this.driver.wait(until.elementIsVisible(passwordField), 20000);
  }

  async login(email, password) {
    const emailField = await this.driver.findElement(this.usenameInput);
    const passwordField = await this.driver.findElement(this.passwordInput);
    const loginBtn = await this.driver.findElement(this.loginButton);

    await emailField.clear();
    await emailField.sendKeys(email);
    await passwordField.clear();
    await passwordField.sendKeys(password);
    await loginBtn.click();
  }

  async getLoginErrorText() {
    try {
      const form = await this.driver.findElement(this.loginFormLocator);
      const errorElements = await form.findElements(
        this.loginErrorMessageLocator,
      );
      for (const element of errorElements) {
        try {
          if (await element.isDisplayed()) {
            const text = await element.getText();
            if (text && text.trim() && text.trim().length < 300) {
              return text.trim();
            }
          }
        } catch (error) {
          // ignore stale elements or visibility errors
        }
      }
    } catch (error) {
      // fallback to page-wide search if login form is not found
    }

    const fallbackElements = await this.driver.findElements(
      this.loginErrorMessageLocator,
    );
    for (const element of fallbackElements) {
      try {
        if (await element.isDisplayed()) {
          const text = await element.getText();
          if (text && text.trim() && text.trim().length < 300) {
            return text.trim();
          }
        }
      } catch (error) {
        // ignore stale elements or visibility errors
      }
    }
    return "";
  }

  async waitForLoginFailure(timeout = 10000) {
    await this.driver.wait(async () => {
      const currentUrl = await this.driver.getCurrentUrl();
      if (currentUrl.includes("/login")) {
        return true;
      }
      const loginButtons = await this.driver.findElements(this.loginButton);
      return loginButtons.length > 0;
    }, timeout);
  }

  async continueLoginIfNeeded() {
    try {
      const continueBtn = await this.driver.wait(
        until.elementLocated(this.continueLoginButton),
        5000,
      );
      await this.driver.wait(until.elementIsVisible(continueBtn), 5000);
      await continueBtn.click();
    } catch (error) {
      // Continue login modal không xuất hiện, bỏ qua
    }
  }

  async clickElementViaJs(element) {
    await this.driver.executeScript(
      "arguments[0].scrollIntoView({block:'center'}); arguments[0].click();",
      element,
    );
  }

  async selectFc(fcName = "FC HN") {
    const fcOption = By.xpath(`//span[text()='${fcName}']`);
    const warehouse = await this.driver.wait(
      until.elementLocated(fcOption),
      15000,
    );
    await this.driver.wait(until.elementIsVisible(warehouse), 15000);

    try {
      await warehouse.click();
    } catch (error) {
      await this.clickElementViaJs(warehouse);
    }

    const confirmButton = await this.driver.wait(
      until.elementLocated(this.confirmFcButton),
      15000,
    );
    await this.driver.wait(until.elementIsVisible(confirmButton), 15000);

    try {
      await confirmButton.click();
    } catch (error) {
      await this.clickElementViaJs(confirmButton);
    }
  }

  async isLoginFormVisible() {
    try {
      const emailField = await this.driver.findElement(this.usenameInput);
      const passwordField = await this.driver.findElement(this.passwordInput);
      return (
        (await emailField.isDisplayed()) && (await passwordField.isDisplayed())
      );
    } catch (error) {
      return false;
    }
  }

  async waitForLoginSuccess() {
    await this.driver.wait(async () => {
      const currentUrl = await this.driver.getCurrentUrl();
      if (
        currentUrl.includes("/dashboard") ||
        currentUrl.includes("/home") ||
        currentUrl.includes("/wms") ||
        currentUrl.includes("/warehouse")
      ) {
        return true;
      }

      const logoutButtons = await this.driver.findElements(this.logoutButton);
      return logoutButtons.length > 0;
    }, 30000);
  }
}

module.exports = LoginPage;

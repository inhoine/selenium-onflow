const { By, until } = require("selenium-webdriver");
const BasePage = require("./BasePage");
const config = require("../config");

class LoginPage extends BasePage {
  constructor(driver) {
    super(driver);
  }

  url = `${config.urls.wms}/login`;

  usenameInput = By.css("input[placeholder='Nhập email'], input[type='email']");
  passwordInput = By.css(
    "input[placeholder='Nhập mật khẩu'], input[type='password']",
  );
  loginButton = By.css("button[type='submit']");
  continueLoginButton = By.xpath("//button[contains(.,'Tiếp tục đăng nhập')]");
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
    await this.waitForVisible(this.usenameInput, 20000);
    await this.waitForVisible(this.passwordInput, 20000);
  }

  async login(email, password) {
    await this.type(this.usenameInput, email);
    await this.type(this.passwordInput, password);
    const loginBtn = await this.find(this.loginButton);
    await loginBtn.click();
  }

  async getLoginErrorText() {
    try {
      const form = await this.find(this.loginFormLocator);
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

    const fallbackElements = await this.findAll(this.loginErrorMessageLocator);
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
      const loginButtons = await this.findAll(this.loginButton);
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

  async selectFc(fcName = config.defaultFcName) {
    const fcOption = By.xpath(`//span[text()='${fcName}']`);
    const warehouse = await this.driver.wait(
      until.elementLocated(fcOption),
      15000,
    );
    await this.driver.wait(until.elementIsVisible(warehouse), 15000);

    try {
      await warehouse.click();
    } catch (error) {
      await this.clickViaJs(warehouse);
    }

    const confirmButton = await this.driver.wait(
      until.elementLocated(this.confirmFcButton),
      15000,
    );
    await this.driver.wait(until.elementIsVisible(confirmButton), 15000);

    try {
      await confirmButton.click();
    } catch (error) {
      await this.clickViaJs(confirmButton);
    }
  }

  async isLoginFormVisible() {
    try {
      const emailField = await this.find(this.usenameInput);
      const passwordField = await this.find(this.passwordInput);
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

      const logoutButtons = await this.findAll(this.logoutButton);
      return logoutButtons.length > 0;
    }, 30000);
  }
}

module.exports = LoginPage;

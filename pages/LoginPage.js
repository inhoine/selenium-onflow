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

  async selectFc(fcName = "FC HN") {
    const fcOption = By.xpath(`//span[text()='${fcName}']`);
    const warehouse = await this.driver.wait(
      until.elementLocated(fcOption),
      15000,
    );
    await this.driver.wait(until.elementIsVisible(warehouse), 15000);
    await warehouse.click();

    const confirmButton = await this.driver.wait(
      until.elementLocated(this.confirmFcButton),
      15000,
    );
    await this.driver.wait(until.elementIsVisible(confirmButton), 15000);
    await confirmButton.click();
  }

  async waitForLoginSuccess() {
    await this.driver.wait(async () => {
      const currentUrl = await this.driver.getCurrentUrl();
      if (
        currentUrl.includes("/dashboard") ||
        currentUrl.includes("/home") ||
        currentUrl.includes("/wms")
      ) {
        return true;
      }

      const logoutButtons = await this.driver.findElements(this.logoutButton);
      return logoutButtons.length > 0;
    }, 20000);
  }
}

module.exports = LoginPage;

const { By, until, Key } = require("selenium-webdriver");

class CreatePickupOrderPage {
  constructor(driver) {
    this.driver = driver;
  }

  pickUpTypeField = By.xpath("//div[text()='Chọn loại bảng kê']");
  pickUpStrategyField = By.xpath("//div[text()='Chọn loại chiến lược']");
  dropDownOption = (typeName) =>
    By.xpath(
      `//*[contains(@class,'-menu')]//*[normalize-space(.)='${typeName}']`,
    );

  chooseCustomerField = By.xpath(
    "//div[text()='Chọn khách hàng']/ancestor::div[contains(@class,'-control')]",
  );
  chooseCustomerInput = By.xpath(
    "//div[text()='Chọn khách hàng']/following::input[1]",
  );
  chooseCustomerOption = (customerName) =>
    By.xpath(
      `//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'${customerName}')]`,
    );

  customizeBtn = By.xpath("//button[text()='Tuỳ chỉnh']");
  inputOrderField = By.xpath("//input[@placeholder='Theo mã đơn hàng']");
  confirmAddOrderBtn = By.xpath("//button[text()='Xác nhận']");
  createPickUpBtn = By.xpath("//button[text()='Tạo bảng kê']");

  notificationSuccess = By.xpath(
    "//div[contains(@class,'Toastify__toast-body')]//div[text()='Tạo bảng kê thành công !']",
  );

  async waitForElement(locator, timeout = 10000) {
    return await this.driver.wait(until.elementLocated(locator), timeout);
  }

  async waitForVisible(locator, timeout = 10000) {
    const element = await this.waitForElement(locator, timeout);
    await this.driver.wait(until.elementIsVisible(element), timeout);
    return element;
  }

  async waitForEnabled(locator, timeout = 10000) {
    const element = await this.waitForVisible(locator, timeout);
    await this.driver.wait(until.elementIsEnabled(element), timeout);
    return element;
  }

  async selectPickUpType(typeName) {
    const pickUpTypeFieldElement = await this.waitForEnabled(
      this.pickUpTypeField,
    );
    await pickUpTypeFieldElement.click();

    const pickUpListOption = await this.waitForVisible(
      this.dropDownOption(typeName),
      10000,
    );
    await this.driver.executeScript("arguments[0].click();", pickUpListOption);

    console.log(`Selected pickup type: ${typeName}`);
  }

  async selectPickUpStrategy(strategyName) {
    const pickUpStrategyFieldElement = await this.waitForEnabled(
      this.pickUpStrategyField,
    );
    await pickUpStrategyFieldElement.click();

    const pickUpListOption = await this.waitForVisible(
      this.dropDownOption(strategyName),
      10000,
    );
    await this.driver.executeScript("arguments[0].click();", pickUpListOption);

    console.log(`Selected pickup strategy: ${strategyName}`);
  }

  async selectCustomerWMS(customerName) {
    const chooseCustomerFieldElement = await this.waitForEnabled(
      this.chooseCustomerField,
    );
    await chooseCustomerFieldElement.click();

    const chooseCustomerInput = await this.waitForEnabled(
      this.chooseCustomerInput,
    );
    await chooseCustomerInput.clear();
    await chooseCustomerInput.sendKeys(customerName);

    await this.waitForVisible(this.chooseCustomerOption(customerName), 15000);
    await chooseCustomerInput.sendKeys(Key.ENTER);

    console.log(`Selected customer: ${customerName}`);
  }

  async addOrderCustomize(trackingNumber) {
    const customizeBtn = await this.waitForEnabled(this.customizeBtn);
    await this.driver.executeScript("arguments[0].click();", customizeBtn);

    const inputOrderField = await this.waitForEnabled(this.inputOrderField);
    await inputOrderField.sendKeys(trackingNumber);

    const confirmAddOrderBtn = await this.waitForEnabled(
      this.confirmAddOrderBtn,
    );
    await this.driver.executeScript(
      "arguments[0].click();",
      confirmAddOrderBtn,
    );

    const createPickUpBtn = await this.waitForEnabled(this.createPickUpBtn);
    await this.driver.executeScript(
      "arguments[0].scrollIntoView({block:'center'});",
      createPickUpBtn,
    );
    await this.driver.executeScript("arguments[0].click();", createPickUpBtn);
  }

  async verifyPickUpOrderCreated() {
    const notification = await this.waitForVisible(
      this.notificationSuccess,
      10000,
    );
    const text = await notification.getText();

    if (!text.includes("Tạo bảng kê thành công !")) {
      throw new Error(`Unexpected toast: ${text}`);
    }

    console.log("Toast:", text);
    console.log("Pickup order created successfully");
  }
}
module.exports = CreatePickupOrderPage;

const { By, until, Key } = require("selenium-webdriver");

class CreateOrderOMS {
  constructor(driver) {
    this.driver = driver;
  }

  createOrderButton = By.xpath(
    "//button[i[contains(@class,'ri-file-list-3-line')] and contains(.,'Tạo đơn hàng')]",
  );
  createOrderMenu = By.xpath(
    "//button[@role='menuitem' and @title='Tạo đơn hàng']",
  );

  customerField = By.xpath(
    "//div[text()='Chọn khách hàng']/ancestor::div[contains(@class,'-control')]",
  );
  customerInput = By.xpath(
    "//div[text()='Chọn khách hàng']/following::input[1]",
  );
  customerOption = (customerName) =>
    By.xpath(
      `//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'${customerName}')]`,
    );

  saleStoreField = By.xpath(
    "//div[text()='Chọn kênh bán hàng']/ancestor::div[contains(@class,'-control')]",
  );
  saleStoreInput = By.xpath(
    "//div[text()='Chọn kênh bán hàng']/following::input[1]",
  );
  saleStoreOption = (saleStoreName) =>
    By.xpath(
      `//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'${saleStoreName}')]`,
    );

  choosePickupField = By.xpath(
    "//div[text()='Chọn địa chỉ lấy hàng']/ancestor::div[contains(@class,'-control')]",
  );
  choosePickupInput = By.xpath(
    "//div[text()='Chọn địa chỉ lấy hàng']/following::input[1]",
  );
  choosePickupOption = (pickupCode) =>
    By.xpath(
      `//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'${pickupCode}')]`,
    );

  addNewProductBtn = By.xpath("//button[text()='Thêm sản phẩm']");
  productDropdowns = By.xpath(
    "//div[text()='Chọn sản phẩm']/ancestor::div[contains(@class,'-control')]",
  );
  productOption = (keyword) =>
    By.xpath(
      `//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'${keyword}')]`,
    );
  productQty = By.xpath("//input[contains(@placeholder,'Nhập số lượng')]");

  orderNumberField = By.xpath("//input[@placeholder='Nhập mã đơn hàng']");
  continueBtn = By.xpath("//button[text()='Tiếp theo']");
  createBtn = By.xpath(
    "//button[contains(@class,'btn-success') and contains(normalize-space(.),'Tạo đơn')]",
  );
  confirmAndCreateBtn = By.xpath("//button[text()='Tạo và xử lý đơn hàng']");

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

  async waitAndClick(locator, timeout = 10000) {
    const element = await this.waitForEnabled(locator, timeout);
    await element.click();
    return element;
  }

  async selectAutocompleteOption(
    fieldLocator,
    inputLocator,
    optionLocator,
    value,
  ) {
    await this.waitAndClick(fieldLocator);
    const input = await this.waitForEnabled(inputLocator);
    await input.clear();
    await input.sendKeys(value);

    const option = await this.waitForVisible(optionLocator(value), 15000);
    await option.click();
  }

  async accessCreateOrder() {
    await this.waitAndClick(
      By.xpath("(//button[contains(normalize-space(.),'Tạo đơn hàng')])[1]"),
    );
    await this.waitAndClick(this.createOrderMenu);
    console.log("Opened create order page");
  }

  async selectCustomerOMS(customerName) {
    await this.selectAutocompleteOption(
      this.customerField,
      this.customerInput,
      this.customerOption,
      customerName,
    );
    console.log(`Selected customer: ${customerName}`);
  }

  async selectSaleStore(saleStoreName) {
    await this.selectAutocompleteOption(
      this.saleStoreField,
      this.saleStoreInput,
      this.saleStoreOption,
      saleStoreName,
    );
    console.log(`Selected sale store: ${saleStoreName}`);
  }

  async selectChoosePickup(pickupCode) {
    await this.selectAutocompleteOption(
      this.choosePickupField,
      this.choosePickupInput,
      this.choosePickupOption,
      pickupCode,
    );
    console.log(`Selected pickup: ${pickupCode}`);
  }

  async getLastProductDropdown() {
    const dropdowns = await this.driver.wait(async () => {
      const elements = await this.driver.findElements(this.productDropdowns);
      return elements.length > 0 ? elements : false;
    }, 10000);

    return dropdowns[dropdowns.length - 1];
  }

  async addProductToCreateOrder(productKeyword, qty = 1) {
    const dropdown = await this.getLastProductDropdown();
    await this.driver.executeScript(
      "arguments[0].scrollIntoView({block:'center'});",
      dropdown,
    );
    await this.driver.executeScript("arguments[0].click();", dropdown);

    const input = await dropdown.findElement(By.css("input"));
    await input.clear();
    await input.sendKeys(productKeyword);

    const option = await this.waitForVisible(
      this.productOption(productKeyword),
    );
    await option.click();

    const qtyInputs = await this.driver.findElements(this.productQty);
    const qtyInput = qtyInputs[qtyInputs.length - 1];
    await qtyInput.clear();
    await qtyInput.sendKeys(String(qty));

    console.log(`Selected product ${productKeyword} with qty ${qty}`);
  }

  async clickAddNewProductBtn() {
    const beforeCount = (await this.driver.findElements(this.productDropdowns))
      .length;
    await this.waitAndClick(this.addNewProductBtn);
    await this.driver.wait(async () => {
      const rows = await this.driver.findElements(this.productDropdowns);
      return rows.length > beforeCount;
    }, 10000);
    console.log("Added new product row");
  }

  async inputOrderNumber() {
    const orderNumber = `ORD-${Date.now()}`;
    const orderNumberField = await this.waitForEnabled(this.orderNumberField);
    await orderNumberField.clear();
    await orderNumberField.sendKeys(orderNumber);
    console.log(`Input order number: ${orderNumber}`);
    return orderNumber;
  }

  async confirmCreateOrder() {
    await this.waitAndClick(this.continueBtn);

    await this.driver.wait(async () => {
      try {
        const btn = await this.driver.findElement(this.createBtn);
        return (await btn.isDisplayed()) && (await btn.isEnabled());
      } catch (error) {
        return false;
      }
    }, 30000);

    const confirmBtn = await this.waitForEnabled(this.createBtn, 30000);
    await this.driver.executeScript(
      "arguments[0].scrollIntoView({block:'center'});",
      confirmBtn,
    );
    await confirmBtn.click();

    await this.waitAndClick(this.confirmAndCreateBtn);
  }

  async verifyCreatedOrder(orderNumber) {
    const orderElement = await this.waitForVisible(
      By.xpath(
        `//a[contains(@id,'partner_tracking_code') and normalize-space(.)='${orderNumber}']`,
      ),
      20000,
    );
    console.log(`Created order found: ${orderNumber}`);
    return true;
  }

  async getTrackingCodeByOrderNumber(orderNumber) {
    const trackingCodeElement = await this.waitForVisible(
      By.xpath(
        `//a[normalize-space(.)='${orderNumber}']/ancestor::div[contains(@class,'flex-column')]//p[contains(@id,'tracking_code')]`,
      ),
      20000,
    );
    const trackingCode = await trackingCodeElement.getText();
    console.log(`Tracking code: ${trackingCode}`);
    return trackingCode;
  }
}

module.exports = CreateOrderOMS;

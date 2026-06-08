const { By, until } = require("selenium-webdriver");

class ProductPage {
  constructor(driver) {
    this.driver = driver;
  }

  createProductDropdownBtn = By.xpath("//button[contains(.,'Tạo sản phẩm')]");
  createProductMenuItem = By.xpath("//a[@title='Tạo sản phẩm']");

  skuInput = By.css("input[placeholder='Nhập SKU']");
  nameInput = By.css("input[placeholder='Nhập tên sản phẩm']");
  brandDropdownField = By.xpath(
    "//div[contains(@class,'css-1qp2wxd-control')]",
  );
  brandOption = By.xpath(
    "//div[contains(@class,'css-1y5o33s-menu')]//div[contains(@class,'css-d7l1ni-option')]",
  );

  openCategoryPopupBtn = By.xpath(
    "//input[@placeholder='Chọn danh mục']/following::button[contains(.,'Chọn')][1]",
  );

  categoryOption = By.xpath(
    "//div[contains(@class,'simplebar-content')]//div[text()='Watches']",
  );

  confirmCategoryBtn = By.xpath(
    "//div[contains(@class,'modal-content')]//button[contains(.,'Chọn')]",
  );

  costPriceField = By.xpath("//input[@placeholder='Nhập giá nhập']");
  sellPriceField = By.xpath("//input[@placeholder='Nhập giá bán']");

  weightField = By.xpath("//input[@placeholder='Khối lượng']");

  lengthField = By.xpath("//input[@placeholder='Chiều dài']");
  widthField = By.xpath("//input[@placeholder='Chiều rộng']");
  heightField = By.xpath("//input[@placeholder='Chiều cao']");

  serialCheckbox = By.xpath("//input[@id='serial']");

  batchCheckbox = By.xpath("//input[@id='batch_lot']");

  descriptionField = By.xpath("//textarea[@placeholder='Nhập mô tả sản phẩm']");

  createProductBtn = By.xpath("//button[contains(.,'Tạo sản phẩm')]");

  validationErrorLocator = By.xpath(
    "//*[contains(@class,'ant-form-item-explain') or contains(@class,'error') or contains(@class,'invalid') or contains(normalize-space(.),'Vui lòng') or contains(normalize-space(.),'không được') or contains(normalize-space(.),'bắt buộc')][normalize-space(string())!='']",
  );

  searchInput = By.xpath(
    "//input[@placeholder='Tìm kiếm theo SKU, tên sản phẩm, NHSIN, Barcode, SKU mẫu mã, NHSIN mẫu mã, Barcode mẫu mã']",
  );

  async waitAndClick(locator, timeout = 10000) {
    const element = await this.driver.wait(
      until.elementLocated(locator),
      timeout,
    );
    await this.driver.wait(until.elementIsVisible(element), timeout);
    await this.driver.wait(until.elementIsEnabled(element), timeout);
    await this.driver.executeScript(
      "arguments[0].scrollIntoView({block:'center'});",
      element,
    );
    await element.click();
    return element;
  }

  async accessCreateProduct() {
    await this.waitAndClick(this.createProductDropdownBtn);
    await this.waitAndClick(this.createProductMenuItem);
  }

  async fillBasicInfo({ sku, productName } = {}) {
    const skuInput = await this.driver.wait(
      until.elementLocated(this.skuInput),
      10000,
    );
    await this.driver.wait(until.elementIsVisible(skuInput), 10000);
    await skuInput.clear();
    if (sku !== undefined && sku !== null) {
      await skuInput.sendKeys(sku);
    }

    const nameInput = await this.driver.wait(
      until.elementLocated(this.nameInput),
      10000,
    );
    await this.driver.wait(until.elementIsVisible(nameInput), 10000);
    await nameInput.clear();
    if (productName !== undefined && productName !== null) {
      await nameInput.sendKeys(productName);
    }
  }

  async inputProductInfor({ sku, productName } = {}) {
    const randomNUM = Date.now().toString().slice(-6);
    const finalSku = sku ?? `SKU-${randomNUM}`;
    const finalProductName = productName ?? `Prod-Auto-${randomNUM}`;

    await this.fillBasicInfo({ sku: finalSku, productName: finalProductName });

    return {
      sku: finalSku,
      productName: finalProductName,
    };
  }

  async selectBrand() {
    await this.waitAndClick(this.brandDropdownField);

    const brandOption = await this.driver.wait(
      until.elementLocated(this.brandOption),
      10000,
    );
    await this.driver.wait(until.elementIsVisible(brandOption), 10000);
    await this.driver.executeScript(
      "arguments[0].scrollIntoView(true);",
      brandOption,
    );
    await this.driver.executeScript("arguments[0].click();", brandOption);
  }

  async selectCategory() {
    await this.waitAndClick(this.openCategoryPopupBtn);

    const category = await this.driver.wait(
      until.elementLocated(this.categoryOption),
      10000,
    );
    await this.driver.wait(until.elementIsVisible(category), 10000);
    await this.driver.executeScript(
      "arguments[0].scrollIntoView(true);",
      category,
    );
    await category.click();

    await this.waitAndClick(this.confirmCategoryBtn);
  }

  async fillCostPrice(value) {
    const costInput = await this.driver.wait(
      until.elementLocated(this.costPriceField),
      10000,
    );
    await costInput.clear();
    if (value !== undefined && value !== null) {
      await costInput.sendKeys(value);
    }
  }

  async fillSellPrice(value) {
    const sellInput = await this.driver.wait(
      until.elementLocated(this.sellPriceField),
      10000,
    );
    await sellInput.clear();
    if (value !== undefined && value !== null) {
      await sellInput.sendKeys(value);
    }
  }

  async priceProduct() {
    await this.fillCostPrice("200000");
    await this.fillSellPrice("200000");
  }

  async fillWeight(value) {
    const weightInput = await this.driver.wait(
      until.elementLocated(this.weightField),
      10000,
    );
    await weightInput.clear();
    if (value !== undefined && value !== null) {
      await weightInput.sendKeys(value);
    }
  }

  async fillDimensions({ length, width, height } = {}) {
    const lengthInput = await this.driver.wait(
      until.elementLocated(this.lengthField),
      10000,
    );
    await lengthInput.clear();
    if (length !== undefined && length !== null) {
      await lengthInput.sendKeys(length);
    }

    const widthInput = await this.driver.wait(
      until.elementLocated(this.widthField),
      10000,
    );
    await widthInput.clear();
    if (width !== undefined && width !== null) {
      await widthInput.sendKeys(width);
    }

    const heightInput = await this.driver.wait(
      until.elementLocated(this.heightField),
      10000,
    );
    await heightInput.clear();
    if (height !== undefined && height !== null) {
      await heightInput.sendKeys(height);
    }
  }

  async demensionProduct() {
    const weightInput = await this.driver.wait(
      until.elementLocated(this.weightField),
      10000,
    );
    await weightInput.sendKeys("300");

    const lengthInput = await this.driver.wait(
      until.elementLocated(this.lengthField),
      10000,
    );
    await lengthInput.sendKeys("20");

    const widthInput = await this.driver.wait(
      until.elementLocated(this.widthField),
      10000,
    );
    await widthInput.sendKeys("20");

    const heightInput = await this.driver.wait(
      until.elementLocated(this.heightField),
      10000,
    );
    await heightInput.sendKeys("20");
  }

  async checkCheckbox(locator) {
    const checkbox = await this.driver.wait(
      until.elementLocated(locator),
      10000,
    );

    await this.driver.wait(until.elementIsVisible(checkbox), 10000);

    const isChecked = await checkbox.isSelected();

    if (!isChecked) {
      await this.driver.executeScript(
        "arguments[0].scrollIntoView({block:'center'});",
        checkbox,
      );

      await this.driver.executeScript("arguments[0].click();", checkbox);
    }
  }

  async inboundManagement({ serial = false, batch = false } = {}) {
    if (serial) {
      await this.checkCheckbox(this.serialCheckbox);
    }

    if (batch) {
      await this.checkCheckbox(this.batchCheckbox);
    }
  }

  async descriptionProduct() {
    const description = await this.driver.wait(
      until.elementLocated(this.descriptionField),
      10000,
    );
    await description.sendKeys(
      "This is an auto-generated product description.",
    );
  }

  async submitProduct() {
    const submitButtons = await this.driver.findElements(this.createProductBtn);
    const submitButton = submitButtons[submitButtons.length - 1];
    await this.driver.wait(until.elementIsVisible(submitButton), 10000);
    await this.driver.wait(until.elementIsEnabled(submitButton), 10000);
    await this.driver.executeScript(
      "arguments[0].scrollIntoView({block:'center'});",
      submitButton,
    );
    await submitButton.click();
  }

  async getCreateProductErrorText() {
    const errorElements = await this.driver.findElements(
      this.validationErrorLocator,
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
        // ignore stale elements
      }
    }
    return "";
  }

  async waitForCreateProductFailure(timeout = 10000) {
    const start = Date.now();
    while (Date.now() - start < timeout) {
      const errorText = await this.getCreateProductErrorText();
      if (errorText) {
        return;
      }

      try {
        const submitButtons = await this.driver.findElements(
          this.createProductBtn,
        );
        if (submitButtons.length > 0) {
          return;
        }
      } catch (error) {
        // continue retry
      }

      await this.driver.sleep(500);
    }
  }

  async searchProduct(sku) {
    const searchInput = await this.driver.wait(
      until.elementLocated(this.searchInput),
      10000,
    );

    await this.driver.wait(until.elementIsVisible(searchInput), 10000);

    await searchInput.clear();

    await searchInput.sendKeys(sku);
  }

  async verifyProductDisplayed(sku) {
    const productLocator = By.xpath(`//p[@title='${sku}']`);

    await this.driver.wait(async () => {
      const products = await this.driver.findElements(productLocator);

      if (products.length === 0) {
        return false;
      }

      try {
        return await products[0].isDisplayed();
      } catch (error) {
        return false; // DOM re-render thì retry lại
      }
    }, 10000);

    console.log(`
=================================
PRODUCT CREATED SUCCESSFULLY
SKU: ${sku}
=================================
`);
  }
}
module.exports = ProductPage;

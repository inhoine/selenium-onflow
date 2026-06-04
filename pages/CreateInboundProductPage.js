const { By, until, Key } = require("selenium-webdriver");

class CreateInboundProductPage {
  constructor(driver) {
    this.driver = driver;
  }
  dropdownInboundBtn = By.xpath("//button[contains(.,'Tạo phiếu')]");
  createInboundMenuItem = By.xpath("//button[contains(.,'Tạo phiếu nhập')]");

  warehouseField = By.xpath(
    "//div[text()='Chọn địa chỉ lấy hàng']/ancestor::div[contains(@class,'-control')]",
  );

  warehouseInput = By.xpath(
    "//div[text()='Chọn địa chỉ lấy hàng']/following::input[1]",
  );

  warehouseOption = By.xpath(
    "//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'PK100270')]",
  );

  supplierField = By.xpath(
    "//div[text()='Chọn nhà cung cấp']/ancestor::div[contains(@class,'-control')]",
  );

  supplierInput = By.xpath(
    "//div[text()='Chọn nhà cung cấp']/following::input[1]",
  );

  supplierOption = By.xpath(
    "//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'Supplier A')]",
  );

  referenceField = By.xpath("//input[@name='shipmentReferenceCode']");

  addProductBtn = By.xpath("//p[text()='Thêm sản phẩm']");

  addProductField = By.xpath(
    "//div[text()='Chọn sản phẩm']/ancestor::div[contains(@class,'-control')]",
  );

  addNewProductBtn = By.xpath("//button[text()='Thêm sản phẩm mới']");

  productDropdowns = By.xpath(
    "//div[text()='Chọn sản phẩm']/ancestor::div[contains(@class,'-control')]",
  );

  productOption = (keyword) =>
    By.xpath(
      `//*[contains(@class,'-menu')]//*[contains(normalize-space(.),'${keyword}')]`,
    );

  productQtyInputByRow = (rowIndex) =>
    By.xpath(`//input[@name='listProduct.${rowIndex - 1}.productQty']`);

  confirmItemInboundBtn = By.xpath("//button[text()='Xác nhận']");

  lengthField = By.xpath("//input[@placeholder='Dài']");
  widthField = By.xpath("//input[@placeholder='Rộng']");
  heightField = By.xpath("//input[@placeholder='Cao']");

  createInboundBtn = By.xpath("//button[text()='Tạo mới']");
  confirmInboundBtn = By.xpath("//button[text()='Tạo và duyệt phiếu nhập']");

  inboundCodeText = By.xpath("//h5[contains(.,'Mã nhập kho')]");

  async clickCreateInboundBtn() {
    try {
      const dropdownBtn = await this.driver.wait(
        until.elementLocated(this.dropdownInboundBtn),
        5000,
      );
      await dropdownBtn.click();
      console.log("Clicked 'Tạo phiếu' button");
    } catch (error) {
      console.error(
        "Không tìm thấy nút 'Tạo phiếu' hoặc có lỗi khi click: ",
        error,
      );
    }
  }
  async clickCreateInboundMenuItem() {
    try {
      const menuItem = await this.driver.wait(
        until.elementLocated(this.createInboundMenuItem),
        5000,
      );
      await menuItem.click();
      console.log("Clicked 'Tạo phiếu nhập' menu item");
    } catch (error) {
      console.error(
        "Không tìm thấy menu item 'Tạo phiếu nhập' hoặc có lỗi khi click: ",
        error,
      );
    }
  }
  async selectWarehouse() {
    // mở dropdown
    const dropdown = await this.driver.wait(
      until.elementLocated(this.warehouseField),
      10000,
    );

    await this.driver.executeScript("arguments[0].click();", dropdown);

    console.log("Opened warehouse dropdown");

    // input search
    const input = await this.driver.wait(
      until.elementLocated(this.warehouseInput),
      10000,
    );

    // nhập keyword
    await input.sendKeys("PK100270");

    console.log("Typed warehouse keyword");

    // wait option render
    const option = await this.driver.wait(
      until.elementLocated(this.warehouseOption),
      10000,
    );

    await this.driver.wait(until.elementIsVisible(option), 10000);

    console.log("Warehouse option appeared");

    // press ENTER để select
    await input.sendKeys(Key.ENTER);

    console.log("Selected warehouse by ENTER");
  }

  async selectSupplier() {
    // mở dropdown
    const dropdown = await this.driver.wait(
      until.elementLocated(this.supplierField),
      10000,
    );
    await this.driver.executeScript("arguments[0].click();", dropdown);
    console.log("Opened supplier dropdown");

    // input search
    const input = await this.driver.wait(
      until.elementLocated(this.supplierInput),
      10000,
    );
    // nhập keyword
    await input.sendKeys("Supplier A");
    console.log("Typed supplier keyword");
    // wait option render
    const option = await this.driver.wait(
      until.elementLocated(this.supplierOption),
      10000,
    );
    await this.driver.wait(until.elementIsVisible(option), 10000);
    console.log("Supplier option appeared");
    // press ENTER để select
    await input.sendKeys(Key.ENTER);
    console.log("Selected supplier by ENTER");
  }

  async inputReference() {
    const randomNUM = Date.now().toString().slice(-6);
    const reference = `REF-${randomNUM}`;
    const referenceInput = await this.driver.wait(
      until.elementLocated(this.referenceField),
      10000,
    );
    await referenceInput.sendKeys(reference);
    console.log("Entered reference: " + reference);
  }

  async clickAddProductBtn() {
    const addProductBtn = await this.driver.wait(
      until.elementLocated(this.addProductBtn),
      10000,
    );
    await addProductBtn.click();
    console.log("Clicked 'Thêm sản phẩm' button");
  }

  async getLastProductDropdown() {
    const dropdowns = await this.driver.wait(async () => {
      const elements = await this.driver.findElements(this.productDropdowns);
      return elements.length > 0 ? elements : false;
    }, 10000);

    return dropdowns[dropdowns.length - 1];
  }

  async addProductToInbound(productKeyword, qty = 1) {
    const dropdown = await this.getLastProductDropdown();

    await this.driver.executeScript(
      "arguments[0].scrollIntoView({block:'center'});",
      dropdown,
    );

    await this.driver.executeScript("arguments[0].click();", dropdown);

    const input = await dropdown.findElement(By.css("input"));

    await input.sendKeys(productKeyword);

    const option = await this.driver.wait(
      until.elementLocated(this.productOption(productKeyword)),
      10000,
    );

    await this.driver.wait(until.elementIsVisible(option), 10000);

    await input.sendKeys(Key.ENTER);

    // Sau khi chọn product, nhập số lượng cho dòng cuối cùng
    const qtyInputs = await this.driver.findElements(
      By.xpath("//input[contains(@name,'productQty')]"),
    );

    const qtyInput = qtyInputs[qtyInputs.length - 1];

    await qtyInput.clear();
    await qtyInput.sendKeys(String(qty));

    console.log(`Selected product ${productKeyword} with qty ${qty}`);
  }

  async clickAddNewProductBtn() {
    const beforeRows = await this.driver.findElements(this.productDropdowns);
    const beforeCount = beforeRows.length;

    const btn = await this.driver.wait(
      until.elementLocated(this.addNewProductBtn),
      10000,
    );

    await btn.click();

    await this.driver.wait(async () => {
      const rows = await this.driver.findElements(this.productDropdowns);
      return rows.length > beforeCount;
    }, 10000);

    console.log("Added new product row");
  }

  async clickItemsConfirmInbound() {
    const btn = await this.driver.wait(
      until.elementLocated(this.confirmItemInboundBtn),
      10000,
    );
    await btn.click();
    console.log("Clicked 'Xác nhận' button to create inbound");
  }

  async inputProductDimensions(length, width, height) {
    const lengthInput = await this.driver.wait(
      until.elementLocated(this.lengthField),
      10000,
    );
    await lengthInput.clear();
    await lengthInput.sendKeys(String(length));
    const widthInput = await this.driver.wait(
      until.elementLocated(this.widthField),
      10000,
    );
    await widthInput.clear();
    await widthInput.sendKeys(String(width));

    const heightInput = await this.driver.wait(
      until.elementLocated(this.heightField),
      10000,
    );
    await heightInput.clear();
    await heightInput.sendKeys(String(height));
    console.log(`Entered product dimensions: ${length}x${width}x${height}`);
  }

  async confirmCreateInbound() {
    const createBtn = await this.driver.wait(
      until.elementLocated(this.createInboundBtn),
      10000,
    );
    await createBtn.click();
    console.log("Clicked 'Tạo mới' button to create inbound");

    const createAndApprovedbtn = await this.driver.wait(
      until.elementLocated(this.confirmInboundBtn),
      10000,
    );
    await createAndApprovedbtn.click();
    console.log(
      "Clicked 'Tạo và duyệt phiếu nhập' button to confirm inbound creation",
    );
  }

  async getInboundCode() {
    const inboundElement = await this.driver.wait(
      until.elementLocated(this.inboundCodeText),
      10000,
    );

    const text = await inboundElement.getText();

    console.log("Inbound text:", text);

    // regex lấy mã NHIV...
    const match = text.match(/NHIV\d+/);

    if (!match) {
      throw new Error("Không tìm thấy mã nhập kho");
    }

    const inboundCode = match[0];

    console.log("Inbound Code:", inboundCode);

    return inboundCode;
  }
}
module.exports = CreateInboundProductPage;

const { By, until, Key } = require("selenium-webdriver");

class InboundProductWmsPage {
  constructor(driver) {
    this.driver = driver;
  }
  scanPoField = By.xpath("//input[@placeholder='Quét mã PO']");

  scanBoxField = By.xpath("//input[@placeholder='Quét mã kiện']");

  selectProductBtn = By.xpath("//button[i[contains(@class,'ri-more-fill')]]");

  inspectProductBtn = By.xpath(
    "(//button[@role='menuitem' and contains(.,'Kiểm hàng')])[2]",
  );

  goodQtyField = By.xpath("//input[@name='quantity_goods_normal']");

  barcodeField = By.xpath("//input[@name='manufacturer_barcode']");

  batchLotField = By.xpath("//input[@name='batch_lot_code']");

  lengthField = By.xpath("//input[@name='goods_d']");
  widthField = By.xpath("//input[@name='goods_w']");
  heightField = By.xpath("//input[@name='goods_h']");
  weightField = By.xpath("//input[@name='goods_weight']");

  confirmInspectBtn = By.xpath("//button[contains(.,'Kiểm hàng')]");

  async scanPo(poCode) {
    const inputElement = await this.driver.wait(
      until.elementLocated(this.scanPoField),
      10000,
    );

    await this.driver.wait(until.elementIsVisible(inputElement), 10000);
    await this.driver.wait(until.elementIsEnabled(inputElement), 10000);

    await inputElement.click();

    await inputElement.sendKeys(Key.chord(Key.CONTROL, "a"));
    await inputElement.sendKeys(poCode, Key.ENTER);

    console.log(`Scanned PO code: ${poCode}`);
  }

  async scanBox(boxCode) {
    for (let attempt = 1; attempt <= 3; attempt++) {
      try {
        const inputElement = await this.driver.wait(
          until.elementLocated(this.scanBoxField),
          10000,
        );

        await this.driver.wait(until.elementIsVisible(inputElement), 10000);
        await this.driver.wait(until.elementIsEnabled(inputElement), 10000);

        await inputElement.click();
        await inputElement.sendKeys(Key.chord(Key.CONTROL, "a"));
        await inputElement.sendKeys(boxCode, Key.ENTER);

        console.log(`Scanned box code: ${boxCode}`);
        return;
      } catch (error) {
        if (
          error.name === "StaleElementReferenceError" ||
          error.name === "InvalidElementStateError"
        ) {
          console.log(`Retry scan box ${boxCode}: attempt ${attempt}`);
          await this.driver.sleep(500);
          continue;
        }

        throw error;
      }
    }

    throw new Error(`Scan box failed: ${boxCode}`);
  }

  async waitProductActionReady() {
    const btn = await this.driver.wait(
      until.elementLocated(this.selectProductBtn),
      10000,
    );

    await this.driver.wait(until.elementIsVisible(btn), 10000);
    await this.driver.wait(until.elementIsEnabled(btn), 10000);

    console.log("Product action button is ready");
  }

  async inspectProduct() {
    const btn = await this.driver.wait(
      until.elementLocated(this.selectProductBtn),
      10000,
    );

    await this.driver.wait(until.elementIsVisible(btn), 10000);
    await this.driver.wait(until.elementIsEnabled(btn), 10000);

    await btn.click();
    console.log("Clicked select product button");

    await this.driver.sleep(300);

    const inspectButtons = await this.driver.findElements(
      By.xpath("//button[@role='menuitem' and contains(.,'Kiểm hàng')]"),
    );

    for (const inspectBtn of inspectButtons) {
      try {
        if (await inspectBtn.isDisplayed()) {
          await inspectBtn.click();
          console.log("Clicked visible inspect product button");
          return;
        }
      } catch (error) {
        continue;
      }
    }

    throw new Error("Không tìm thấy button Kiểm hàng đang hiển thị");
  }

  async inputGoodQuantity(quantity) {
    const inputElement = await this.driver.wait(
      until.elementLocated(this.goodQtyField),
      10000,
    );
    await inputElement.clear();
    await inputElement.sendKeys(quantity.toString());
  }

  async inputBarcode() {
    const barcode = `AUTO${Date.now()}`;
    const inputElement = await this.driver.wait(
      until.elementLocated(this.barcodeField),
      10000,
    );
    await inputElement.clear();
    await inputElement.sendKeys(barcode);
    console.log(`Input barcode: ${barcode}`);
  }

  async inputBatchLot() {
    const batchLot = `${Date.now()}`;
    const batchField = await this.driver.findElements(this.batchLotField);
    if (batchField.length > 0) {
      await batchField[0].clear();
      await batchField[0].sendKeys(batchLot);
    } else {
      console.log("Đây không phải sản phẩm batch lot");
    }
  }

  async fillIfEnabled(locator, value, fieldName) {
    try {
      const input = await this.driver.wait(until.elementLocated(locator), 5000);

      const isEnabled = await input.isEnabled();

      if (!isEnabled) {
        console.log(`${fieldName} is disabled -> skip`);
        return;
      }

      const readonly = await input.getAttribute("readonly");

      if (readonly !== null) {
        console.log(`${fieldName} is readonly -> skip`);
        return;
      }

      await input.clear();
      await input.sendKeys(value.toString());

      console.log(`Entered ${fieldName}: ${value}`);
    } catch (error) {
      console.log(`Skip ${fieldName}: ${error.message}`);
    }
  }
  async inputProductDimensions(productInfo) {
    await this.fillIfEnabled(this.lengthField, productInfo.goodsD, "Length");

    await this.fillIfEnabled(this.widthField, productInfo.goodsW, "Width");

    await this.fillIfEnabled(this.heightField, productInfo.goodsH, "Height");

    await this.fillIfEnabled(
      this.weightField,
      productInfo.goodsWeight,
      "Weight",
    );

    console.log(
      `Input dimensions: ${productInfo.goodsD}x${productInfo.goodsW}x${productInfo.goodsH} - ${productInfo.goodsWeight}g`,
    );
  }

  async waitScanBoxReady() {
    const input = await this.driver.wait(
      until.elementLocated(this.scanBoxField),
      10000,
    );

    await this.driver.wait(until.elementIsVisible(input), 10000);
    await this.driver.wait(until.elementIsEnabled(input), 10000);

    console.log("Scan box field is ready");
  }

  async confirmInspect() {
    const btnconfirmInspectBtn = await this.driver.wait(
      until.elementLocated(this.confirmInspectBtn),
      10000,
    );
    await btnconfirmInspectBtn.click();
    console.log("Confirmed product inspection");

    await this.waitScanBoxReady();
  }
}
module.exports = InboundProductWmsPage;

const { By, Key, until } = require("selenium-webdriver");
const scanTable = require("../utils/scan_table");

const DEFAULT_TIMEOUT = 10000;
const SHORT_DELAY = 100;
const MATERIAL_DELAY = 500;

const URLS = {
  RECEIVE_PACKING_TROLLEY: "https://stg-wms.onflow.vn/receive-packing-trolley",
  PACKING: "https://stg-wms.onflow.vn/packing",
};

class PickAndPackOrderPage {
  constructor(driver) {
    this.driver = driver;
  }

  addEquipmentBtn = By.xpath("//button[text()='Thêm thiết bị chứa hàng']");

  equipmentCodeField = By.xpath(
    "//div[contains(.,'Thêm thiết bị mới')]//input[@placeholder='Nhập mã thiết bị']",
  );

  equipmentGroupDropdown = By.xpath("//div[text()='Chọn nhóm thiết bị']");

  equipmentTypeDropdown = By.xpath("//div[text()='Chọn loại thiết bị']");

  submitEquipmentBtn = By.xpath("//button[@type='submit']");

  dropDownOption = (typeName) =>
    By.xpath(
      `//*[contains(@class,'-menu')]//*[normalize-space(.)='${typeName}']`,
    );

  toastMessage = (message) =>
    By.xpath(
      `//div[contains(@class,'Toastify__toast-body')]//div[contains(normalize-space(.),'${message}')]`,
    );

  scanPackingTrolleyField = By.xpath(
    "//input[@placeholder='Quét mã XE/ bảng kê cần đóng gói']",
  );

  receivePackingTrolleyBtn = By.xpath("//button[text()='Nhận bảng kê']");

  scanPickUpField = By.xpath("//input[@placeholder='Quét mã Xe/ Bảng kê/ Rổ']");

  scanSKUField = By.xpath("//input[contains(@placeholder,'Sản phẩm')]");

  packagingMaterialsField = By.xpath(
    "//input[@placeholder='Quét hoặc nhập mã vật liệu đóng gói']",
  );

  productRows = By.xpath("//tr[.//div[contains(@id,'barcode_')]]");

  currentTrackingCode = By.xpath(
    "//h6[contains(.,'Bạn đang đóng gói cho đơn hàng')]//span[contains(@class,'fw-medium')]",
  );

  async waitVisible(locator, timeout = DEFAULT_TIMEOUT) {
    const element = await this.driver.wait(
      until.elementLocated(locator),
      timeout,
    );

    await this.driver.wait(until.elementIsVisible(element), timeout);
    await this.driver.wait(until.elementIsEnabled(element), timeout);

    return element;
  }

  async clickElement(locator) {
    const element = await this.waitVisible(locator);
    await element.click();
    return element;
  }

  async jsClickElement(locator) {
    const element = await this.waitVisible(locator);
    await this.driver.executeScript("arguments[0].click();", element);
    return element;
  }

  async clearAndEnter(input, value) {
    await input.click();
    await input.sendKeys(Key.chord(Key.CONTROL, "a"), Key.DELETE);
    await input.sendKeys(value, Key.ENTER);
  }

  getItemBarcode(item) {
    return item.barcodes?.[0] || item.partnerCode;
  }

  getQuantityNeedScan(item) {
    return item.quantitySold - item.quantityPick;
  }

  async addEquipment(equipmentGroupName, equipmentTypeName) {
    const random = Math.floor(Math.random() * 1000);
    const equipmentCode = `THIET-BI-${random}`;

    await this.clickElement(this.addEquipmentBtn);

    const equipmentCodeField = await this.waitVisible(this.equipmentCodeField);
    await equipmentCodeField.sendKeys(equipmentCode);

    await this.clickElement(this.equipmentGroupDropdown);
    await this.jsClickElement(this.dropDownOption(equipmentGroupName));

    console.log(`Selected equipment group: ${equipmentGroupName}`);

    await this.clickElement(this.equipmentTypeDropdown);
    await this.jsClickElement(this.dropDownOption(equipmentTypeName));

    console.log(`Selected equipment type: ${equipmentTypeName}`);

    await this.clickElement(this.submitEquipmentBtn);

    console.log("Thêm thiết bị chứa hàng thành công:", equipmentCode);

    return equipmentCode;
  }

  async verifyToastMessage(message) {
    const toast = await this.waitVisible(this.toastMessage(message));
    const text = await toast.getText();

    console.log("Toast:", text);

    return text;
  }

  async receivePackingTrolley(pickupId) {
    if (!pickupId) {
      throw new Error("pickupId is required");
    }

    await this.driver.get(URLS.RECEIVE_PACKING_TROLLEY);

    const scanPackingTrolleyField = await this.waitVisible(
      this.scanPackingTrolleyField,
    );

    await scanPackingTrolleyField.click();
    await scanPackingTrolleyField.sendKeys(pickupId, Key.ENTER);

    console.log(`Scanned pickup/trolley code: ${pickupId}`);

    await this.clickElement(this.receivePackingTrolleyBtn);
  }

  async scanTablePacking() {
    await this.driver.get(URLS.PACKING);
    await scanTable(this.driver, "PACK02");
  }

  async scanPickUpOrder(pickupId) {
    if (!pickupId) {
      throw new Error("pickupId is required");
    }

    const scanPickUpField = await this.waitVisible(this.scanPickUpField);
    await scanPickUpField.sendKeys(pickupId, Key.ENTER);

    console.log(`Scanned pickup order: ${pickupId}`);
  }

  async getVisibleScanSKUInput() {
    return await this.driver.wait(async () => {
      const inputs = await this.driver.findElements(this.scanSKUField);

      for (const input of inputs) {
        try {
          const displayed = await input.isDisplayed();
          const enabled = await input.isEnabled();

          if (displayed && enabled) {
            return input;
          }
        } catch (e) {
          // ignore stale / detached element
        }
      }

      return false;
    }, 15000);
  }

  async scanProductBarcode(barcode) {
    if (!barcode) {
      throw new Error("barcode is required");
    }

    const input = await this.getVisibleScanSKUInput();

    await this.driver.executeScript(
      "arguments[0].scrollIntoView({block:'center'});",
      input,
    );

    await this.driver.executeScript("arguments[0].click();", input);

    await input.sendKeys(Key.chord(Key.CONTROL, "a"), Key.DELETE);
    await input.sendKeys(barcode, Key.ENTER);

    console.log(`Scanned barcode: ${barcode}`);

    await this.driver.sleep(SHORT_DELAY);
  }

  async getCurrentPackingTrackingCode() {
    const element = await this.waitVisible(this.currentTrackingCode);
    const trackingCode = await element.getText();

    console.log("Current tracking code:", trackingCode);

    return trackingCode.trim();
  }

  async scanPackingProducts(pickupItems) {
    for (const item of pickupItems) {
      const barcode = this.getItemBarcode(item);
      const quantityNeedScan = this.getQuantityNeedScan(item);

      if (!barcode) {
        throw new Error(`Không có barcode cho SKU: ${item.partnerCode}`);
      }

      if (quantityNeedScan <= 0) {
        console.log(`Skip ${barcode} vì quantityNeedScan <= 0`);
        continue;
      }

      for (let i = 1; i <= quantityNeedScan; i++) {
        await this.scanProductBarcode(barcode);
        console.log(`Scanned ${barcode}: ${i}/${quantityNeedScan}`);
      }
    }

    console.log("Scanned all packing products");
  }

  async scanPackagingMaterial(materialCode = "40x20x20") {
    const input = await this.waitVisible(this.packagingMaterialsField);

    await this.clearAndEnter(input, materialCode);

    console.log(`Scanned packaging material: ${materialCode}`);

    await this.driver.sleep(MATERIAL_DELAY);
  }

  async getCurrentPackingItemsFromUI() {
    const rows = await this.driver.wait(async () => {
      const elements = await this.driver.findElements(this.productRows);
      return elements.length > 0 ? elements : false;
    }, DEFAULT_TIMEOUT);

    const items = [];

    for (const row of rows) {
      const rowText = await row.getText();

      const barcode = await row
        .findElement(By.xpath(".//div[contains(@id,'barcode_')]"))
        .getText();

      const qtyText = await row
        .findElement(By.xpath(".//td[contains(@class,'text-right')]//h5"))
        .getText();

      const [packed, total] = qtyText
        .split("/")
        .map((value) => Number(value.trim()));

      items.push({
        rowText,
        barcode,
        packed,
        total,
        needScan: total - packed,
      });
    }

    console.log("Current UI items:", JSON.stringify(items, null, 2));

    return items;
  }

  async packCurrentSuggestedOrder(materialCode = "40x20x20") {
    while (true) {
      const items = await this.getCurrentPackingItemsFromUI();

      const pendingItem = items.find((item) => item.needScan > 0);

      if (!pendingItem) {
        console.log("All products packed for current order");
        break;
      }

      await this.scanProductBarcode(pendingItem.barcode);

      console.log(
        `Scanned pending item: ${pendingItem.barcode} ${pendingItem.packed + 1}/${pendingItem.total}`,
      );

      await this.driver.sleep(SHORT_DELAY);
    }

    await this.scanPackagingMaterial(materialCode);

    console.log("Scanned packaging material for current order");

    return true;
  }

  async packBySystemSuggestion(packingOrders, materialCode = "40x20x20") {
    const processedTrackingCodes = new Set();

    for (const order of packingOrders) {
      if (processedTrackingCodes.has(order.trackingCode)) {
        continue;
      }

      const firstPendingItem = order.items.find((item) => {
        const barcode = this.getItemBarcode(item);
        const quantityNeedScan = this.getQuantityNeedScan(item);

        return barcode && quantityNeedScan > 0;
      });

      if (!firstPendingItem) {
        console.log(`Skip ${order.trackingCode}: không còn sản phẩm cần đóng`);
        processedTrackingCodes.add(order.trackingCode);
        continue;
      }

      const barcode = this.getItemBarcode(firstPendingItem);

      await this.scanProductBarcode(barcode);

      const currentTrackingCode = await this.getCurrentPackingTrackingCode();

      if (processedTrackingCodes.has(currentTrackingCode)) {
        console.log(`Skip processed tracking: ${currentTrackingCode}`);
        continue;
      }

      await this.packCurrentSuggestedOrder(materialCode);

      processedTrackingCodes.add(currentTrackingCode);

      console.log(`Completed tracking: ${currentTrackingCode}`);

      await this.driver.sleep(SHORT_DELAY);
    }

    console.log("Packed by system suggestion completed");
  }

  async packOrdersByTrackingCode(packingOrders, materialCode = "40x20x20") {
    for (const order of packingOrders) {
      console.log(`Packing tracking code: ${order.trackingCode}`);

      let hasScannedProduct = false;

      for (const item of order.items) {
        const barcode = this.getItemBarcode(item);
        const quantityNeedScan = this.getQuantityNeedScan(item);

        if (!barcode) {
          throw new Error(`Không có barcode cho SKU: ${item.partnerCode}`);
        }

        if (quantityNeedScan <= 0) {
          console.log(`Skip ${barcode} vì quantityNeedScan <= 0`);
          continue;
        }

        for (let i = 1; i <= quantityNeedScan; i++) {
          await this.scanProductBarcode(barcode);
          hasScannedProduct = true;

          console.log(`Scanned ${barcode}: ${i}/${quantityNeedScan}`);
        }
      }

      if (!hasScannedProduct) {
        console.log(`Tracking ${order.trackingCode} đã đủ hàng -> skip NVL`);
        continue;
      }

      await this.scanPackagingMaterial(materialCode);

      await this.driver.sleep(MATERIAL_DELAY);
    }

    console.log("Packed all tracking codes");
  }
}

module.exports = PickAndPackOrderPage;

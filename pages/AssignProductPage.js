const { By, Key, until } = require("selenium-webdriver");

class AssignProductPage {
  constructor(driver) {
    this.driver = driver;
  }

  modalChangePwd = By.xpath("//button[.//span[text()='Để sau']]");
  modalNotification = By.xpath(
    "//div[contains(@class,'modal-content')]//button[text()='Đóng']",
  );
  productOps = By.xpath("//span[contains(text(),'Sản phẩm')]");
  searchSKUInput = By.xpath("//input[@placeholder='Tìm kiếm...']");
  searchResultRows = By.css("tbody tr");
  noDataMessage = By.xpath("//*[contains(normalize-space(.),'No data found')]");
  checkboxSelectAll = By.xpath("//input[@id='product-checked-all']");
  approveAssignBtn = By.xpath("//button[.//span[text()='Phê duyệt']]");
  confirmApproveBtn = By.xpath(
    "//div[contains(@class,'modal-content')]//button[text()='Phê duyệt']",
  );
  confirmBtn = By.xpath(
    "//div[contains(@class,'modal-content')]//button[text()='Xác nhận']",
  );

  async waitAndClick(locator, timeout = 10000) {
    const element = await this.driver.wait(
      until.elementLocated(locator),
      timeout,
    );
    await this.driver.wait(until.elementIsVisible(element), timeout);
    await this.driver.wait(until.elementIsEnabled(element), timeout);
    await this.driver.executeScript(
      "arguments[0].scrollIntoView(true);",
      element,
    );
    await this.driver.executeScript("arguments[0].click();", element);
    return element;
  }

  async closeChangePwdModal() {
    try {
      const modalBtn = await this.driver.wait(
        until.elementLocated(this.modalChangePwd),
        5000,
      );
      await this.driver.wait(until.elementIsVisible(modalBtn), 5000);
      await modalBtn.click();
      console.log("Closed change password modal");
    } catch (error) {
      console.log("No change password modal");
    }
  }

  async closeNotificationModal() {
    try {
      const closeModalBtn = await this.driver.wait(
        until.elementLocated(this.modalNotification),
        5000,
      );
      await this.driver.wait(until.elementIsVisible(closeModalBtn), 5000);
      await closeModalBtn.click();
      await this.driver.wait(until.stalenessOf(closeModalBtn), 5000);
      console.log("Closed notification modal");
    } catch (error) {
      console.log("No notification modal");
    }
  }

  async accessProductOps() {
    const productMenu = await this.waitAndClick(this.productOps, 10000);
    console.log("Accessed Product OPS");
    return productMenu;
  }

  async getProductWithID() {
    await this.driver.get("https://stg-ops.onflow.vn/products?user_id=294");
    await this.driver.wait(async () => {
      const currentUrl = await this.driver.getCurrentUrl();
      return currentUrl.includes("/products");
    }, 15000);
    await this.driver.wait(async () => {
      const inputs = await this.driver.findElements(By.css("input"));
      return inputs.length > 0;
    }, 15000);
  }

  async findSearchInput() {
    const candidateLocators = [
      By.xpath(
        "//input[contains(@placeholder,'Tìm kiếm') or contains(@placeholder,'tìm kiếm') or contains(@aria-label,'Tìm kiếm') or contains(@aria-label,'tìm kiếm')]",
      ),
      By.css("input[placeholder='Tìm kiếm...']"),
      By.css("input[type='search']"),
      By.xpath(
        "//input[contains(@placeholder,'SKU') or contains(@placeholder,'sku') or contains(@aria-label,'SKU') or contains(@aria-label,'sku')]",
      ),
    ];

    for (const locator of candidateLocators) {
      try {
        const element = await this.driver.findElement(locator);
        if (await element.isDisplayed()) {
          return element;
        }
      } catch (err) {
        continue;
      }
    }

    const inputs = await this.driver.findElements(By.css("input"));
    for (const input of inputs) {
      if (!(await input.isDisplayed())) {
        continue;
      }
      const placeholder = await input.getAttribute("placeholder");
      if (placeholder && placeholder.toLowerCase().includes("tìm")) {
        return input;
      }
    }

    throw new Error("Search input not found on OPS product page");
  }

  async searchProduct(sku) {
    const searchInput = await this.findSearchInput();
    await this.driver.wait(until.elementIsVisible(searchInput), 10000);
    await searchInput.clear();
    await searchInput.sendKeys(sku);
    await this.driver.executeScript(
      "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
      searchInput,
    );
    await searchInput.sendKeys(Key.ENTER);
    console.log(`Searched for product with SKU: ${sku}`);
  }

  async waitForSearchResults(timeout = 15000) {
    await this.driver.wait(
      async () => {
        const rows = await this.driver.findElements(this.searchResultRows);
        for (const row of rows) {
          try {
            if (await row.isDisplayed()) {
              return true;
            }
          } catch (err) {
            continue;
          }
        }

        const noDataElements = await this.driver.findElements(
          this.noDataMessage,
        );
        for (const element of noDataElements) {
          try {
            if (await element.isDisplayed()) {
              return true;
            }
          } catch (err) {
            continue;
          }
        }

        return false;
      },
      timeout,
      "Timed out waiting for OPS product search results or empty state",
    );
    console.log("OPS search results are ready");
  }

  async assignProductToWarehouse() {
    await this.waitAndClick(this.checkboxSelectAll, 10000);
    console.log("Selected all products");

    await this.waitAndClick(this.approveAssignBtn, 10000);
    console.log("Clicked approve assign button");

    const confirmBtn = await this.driver.wait(
      until.elementLocated(this.confirmApproveBtn),
      15000,
    );
    await this.driver.wait(until.elementIsVisible(confirmBtn), 15000);
    await this.driver.executeScript("arguments[0].click();", confirmBtn);
    console.log("Confirmed approve assign");

    const finalConfirmBtn = await this.driver.wait(
      until.elementLocated(this.confirmBtn),
      15000,
    );
    await this.driver.wait(until.elementIsVisible(finalConfirmBtn), 15000);
    await this.driver.executeScript("arguments[0].click();", finalConfirmBtn);
    console.log("Clicked final confirm button");
    await this.driver.sleep(2000);
  }
}

module.exports = AssignProductPage;

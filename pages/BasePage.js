const { until } = require("selenium-webdriver");

class BasePage {
  constructor(driver) {
    this.driver = driver;
  }

  async find(locator) {
    return await this.driver.findElement(locator);
  }

  async findAll(locator) {
    return await this.driver.findElements(locator);
  }

  async waitForVisible(locator, timeout = 15000) {
    const element = await this.driver.wait(
      until.elementLocated(locator),
      timeout,
    );
    await this.driver.wait(until.elementIsVisible(element), timeout);
    return element;
  }

  async waitForClickable(locator, timeout = 15000) {
    const element = await this.driver.wait(
      until.elementLocated(locator),
      timeout,
    );
    await this.driver.wait(until.elementIsVisible(element), timeout);
    return element;
  }

  async click(locator) {
    const element = await this.waitForClickable(locator);
    await element.click();
    return element;
  }

  async clickViaJs(element) {
    await this.driver.executeScript(
      "arguments[0].scrollIntoView({block:'center'}); arguments[0].click();",
      element,
    );
  }

  async type(locator, text) {
    const element = await this.waitForVisible(locator);
    await element.clear();
    await element.sendKeys(text);
    return element;
  }
}

module.exports = BasePage;

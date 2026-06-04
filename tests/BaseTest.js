const { Builder, until } = require("selenium-webdriver");

class BaseTest {
  constructor(browser = "chrome", timeout = 10000, options = null) {
    this.browser = browser;
    this.timeout = timeout;
    this.options = options;
    this.driver = null;
  }

  async setup() {
    const builder = new Builder().forBrowser(this.browser);
    if (this.options) {
      builder.setChromeOptions(this.options);
    }
    this.driver = await builder.build();
    await this.driver.manage().window().maximize();
    return this.driver;
  }

  async teardown() {
    if (this.driver) {
      await this.driver.quit();
      this.driver = null;
    }
  }

  async goTo(url) {
    await this.driver.get(url);
  }

  async waitForElement(locator, timeout = this.timeout) {
    return await this.driver.wait(until.elementLocated(locator), timeout);
  }

  async waitForVisible(locator, timeout = this.timeout) {
    const element = await this.waitForElement(locator, timeout);
    await this.driver.wait(until.elementIsVisible(element), timeout);
    return element;
  }
}

module.exports = BaseTest;

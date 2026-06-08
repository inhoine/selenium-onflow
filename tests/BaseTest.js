const fs = require("fs");
const path = require("path");
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
      try {
        await this.driver.quit();
      } catch (error) {
        console.warn("Failed to quit browser cleanly:", error);
      }
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

  async saveScreenshot(fileName) {
    if (!this.driver) {
      return null;
    }

    const screenshotsDir = path.resolve(process.cwd(), "screenshots");
    if (!fs.existsSync(screenshotsDir)) {
      fs.mkdirSync(screenshotsDir, { recursive: true });
    }

    const screenshotPath = path.join(screenshotsDir, fileName);
    const screenshot = await this.driver.takeScreenshot();
    fs.writeFileSync(screenshotPath, screenshot, "base64");
    return screenshotPath;
  }
}

module.exports = BaseTest;

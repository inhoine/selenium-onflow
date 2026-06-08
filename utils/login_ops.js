const LoginPage = require("../pages/LoginPage");

const account = require("../data/account");

const { until } = require("selenium-webdriver");

async function login_ops(driver) {
  const loginPage = new LoginPage(driver);

  await driver.wait(async () => {
    const currentUrl = await driver.getCurrentUrl();
    if (!currentUrl.includes("/login")) {
      return true; // already authenticated or redirected away from login
    }

    const inputs = await driver.findElements(loginPage.usenameInput);
    return inputs.length > 0;
  }, 20000);

  const currentUrl = await driver.getCurrentUrl();
  if (currentUrl.includes("/login")) {
    await driver.wait(until.elementLocated(loginPage.usenameInput), 20000);
    await loginPage.login(account.ops.email, account.ops.password);

    // wait for successful redirect away from login
    await driver.wait(async () => {
      const redirectedUrl = await driver.getCurrentUrl();
      return !redirectedUrl.includes("/login");
    }, 20000);
  }
}

module.exports = login_ops;

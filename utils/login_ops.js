const LoginPage = require("../pages/LoginPage");

const account = require("../data/account");

const { until } = require("selenium-webdriver");

async function login_ops(driver) {
  const loginPage = new LoginPage(driver);

  // wait login page ready
  await driver.wait(until.elementLocated(loginPage.usenameInput), 10000);
  await loginPage.login(account.ops.email, account.ops.password);

  // wait for successful redirect away from login
  await driver.wait(async () => {
    const currentUrl = await driver.getCurrentUrl();
    return !currentUrl.includes("/login");
  }, 15000);
}

module.exports = login_ops;

const LoginPage = require("../pages/LoginPage");

const account = require("../data/account");

async function login_oms(driver) {
  const loginPage = new LoginPage(driver);

  await loginPage.login(account.oms.email, account.oms.password);
}

module.exports = login_oms;

const LoginPage = require("../pages/LoginPage");
const account = require("../data/account");
const config = require("../config");

const { By, until } = require("selenium-webdriver");

async function login_wms(driver) {
  const loginPage = new LoginPage(driver);

  await loginPage.waitForLoginForm();
  await loginPage.login(account.wms.email, account.wms.password);

  // ===== HANDLE CONTINUE LOGIN MODAL =====
  try {
    const continueLoginBtn = await driver.wait(
      until.elementLocated(
        By.xpath("//button[contains(.,'Tiếp tục đăng nhập')]"),
      ),
      3000,
    );
    await continueLoginBtn.click();
    console.log("Clicked continue login button");
  } catch (error) {
    console.log("Continue login modal not found -> skip");
  }

  // ===== CHỌN KHO =====
  const warehouse = await driver.wait(
    until.elementLocated(By.xpath(`//span[text()='${config.defaultFcName}']`)),
    10000,
  );
  await warehouse.click();
  console.log(`Selected warehouse ${config.defaultFcName}`);

  const confirmSelectWarehouseBtn = await driver.wait(
    until.elementLocated(
      By.xpath(
        "//button[contains(.,'Bạn đã chọn FC') or contains(.,'Xác nhận')]",
      ),
    ),
    10000,
  );
  await driver.executeScript(
    "arguments[0].scrollIntoView({block:'center'});",
    confirmSelectWarehouseBtn,
  );
  await driver.executeScript(
    "arguments[0].click();",
    confirmSelectWarehouseBtn,
  );

  const token = await driver.wait(async () => {
    const value = await driver.executeScript(
      "return localStorage.getItem('token');",
    );
    return value || false;
  }, 30000);

  return token;
}

module.exports = login_wms;

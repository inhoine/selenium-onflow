const BaseTest = require("./BaseTest");
const LoginPage = require("../pages/LoginPage");

async function loginTest() {
  const baseTest = new BaseTest("chrome", 15000);
  await baseTest.setup();

  try {
    const loginPage = new LoginPage(baseTest.driver);
    await loginPage.open();
    await loginPage.login("thanh.nn@nandh.vn", "Nhl@123456");
    await loginPage.continueLoginIfNeeded();
    await loginPage.selectFc("FC HN");
    await loginPage.waitForLoginSuccess();
    console.log("Login test completed successfully.");
  } catch (error) {
    console.error("Login test failed:", error);
    throw error;
  } finally {
    await baseTest.teardown();
  }
}

loginTest();

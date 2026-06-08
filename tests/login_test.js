const BaseTest = require("./BaseTest");
const LoginPage = require("../pages/LoginPage");
const account = require("../data/account");

async function runLoginScenario({
  name,
  email,
  password,
  shouldSucceed,
  expectedErrorContains,
}) {
  const baseTest = new BaseTest("chrome", 15000);
  await baseTest.setup();

  try {
    const loginPage = new LoginPage(baseTest.driver);
    await loginPage.open();
    await loginPage.login(email, password);
    await loginPage.continueLoginIfNeeded();

    const currentUrlAfterLogin = await baseTest.driver.getCurrentUrl();
    console.log(
      `-> ${name}: current URL after login attempt = ${currentUrlAfterLogin}`,
    );

    if (shouldSucceed) {
      await loginPage.selectFc("FC HN");
      const currentUrlAfterSelectFc = await baseTest.driver.getCurrentUrl();
      console.log(
        `-> ${name}: current URL after FC select = ${currentUrlAfterSelectFc}`,
      );
      await loginPage.waitForLoginSuccess();
      console.log(`✅ ${name} passed: login success`);
      return;
    }

    await loginPage.waitForLoginFailure(10000);
    const currentUrl = await baseTest.driver.getCurrentUrl();
    const loginFormVisible = await loginPage.isLoginFormVisible();
    const errorText = await loginPage.getLoginErrorText();

    if (!loginFormVisible) {
      throw new Error(
        `Expected login to fail and stay on the login screen, but login form is no longer visible and current URL is ${currentUrl}`,
      );
    }

    let hasExpectedMessage = true;
    if (expectedErrorContains) {
      if (errorText) {
        hasExpectedMessage = errorText
          .toLowerCase()
          .includes(expectedErrorContains.toLowerCase());
      } else {
        hasExpectedMessage = loginFormVisible;
      }
    }

    if (expectedErrorContains && !errorText) {
      console.warn(
        `⚠️ ${name}: no explicit error text was found, but login remained on the login page`,
      );
    }

    if (!hasExpectedMessage) {
      throw new Error(
        `Expected login failure with message containing '${expectedErrorContains}', but got '${errorText || "<no error message>"}'`,
      );
    }

    console.log(
      `✅ ${name} passed: login rejected as expected${errorText ? ` (${errorText})` : " (no visible message)"}`,
    );
  } catch (error) {
    console.error(`❌ ${name} failed:`, error);
    throw error;
  } finally {
    await baseTest.teardown();
  }
}

async function loginTestSuite() {
  const scenarios = [
    {
      name: "Positive case - valid WMS login",
      email: account.wms.email,
      password: account.wms.password,
      shouldSucceed: true,
    },
    {
      name: "Negative case - invalid password",
      email: account.wms.email,
      password: "WrongPassword123",
      shouldSucceed: false,
      expectedErrorContains: "Sai",
    },
    {
      name: "Negative case - invalid email",
      email: "invalid.email@nandh.vn",
      password: account.wms.password,
      shouldSucceed: false,
      expectedErrorContains: "không",
    },
    {
      name: "Edge case - empty email",
      email: "",
      password: account.wms.password,
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Edge case - empty password",
      email: account.wms.email,
      password: "",
      shouldSucceed: false,
      expectedErrorContains: "Vui lòng",
    },
    {
      name: "Edge case - invalid email format",
      email: "thanh.nn@nandh",
      password: account.wms.password,
      shouldSucceed: false,
      expectedErrorContains: "Email không đúng định dạng",
    },
  ];

  const results = [];
  for (const scenario of scenarios) {
    try {
      await runLoginScenario(scenario);
      results.push({ name: scenario.name, status: "passed" });
    } catch (error) {
      results.push({
        name: scenario.name,
        status: "failed",
        error: error.message,
      });
    }
  }

  console.log("\n=== Login test suite summary ===");
  results.forEach((result) => {
    console.log(
      `- ${result.name}: ${result.status}${result.error ? ` - ${result.error}` : ""}`,
    );
  });

  const failedCount = results.filter(
    (result) => result.status === "failed",
  ).length;
  if (failedCount > 0) {
    console.error(`\n${failedCount} scenario(s) failed.`);
    process.exit(1);
  }
}

loginTestSuite();

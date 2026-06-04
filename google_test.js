const { Builder, By, until } = require("selenium-webdriver");

async function testGoogle() {
  let driver = await new Builder().forBrowser("chrome").build();

  try {
    // mở google
    await driver.get("https://google.com");

    // tìm ô search
    let searchBox = await driver.findElement(By.name("q"));

    // nhập text
    await searchBox.sendKeys("selenium webdriver");

    // submit
    await searchBox.submit();

    // wait title chứa chữ selenium
    await driver.wait(until.titleContains("selenium"), 5000);

    console.log("Test Passed");
  } catch (error) {
    console.log(error);
  } finally {
    // đóng browser
    await driver.quit();
  }
}

testGoogle();

const { By, until, Key } = require("selenium-webdriver");

async function scanTable(driver, tableCode) {
  const tableInput = By.xpath("//input[@placeholder='Quét hoặc nhập mã bàn']");

  await driver.wait(until.elementLocated(tableInput), 5000);

  const inputElement = await driver.findElement(tableInput);
  await inputElement.clear();
  await inputElement.sendKeys(tableCode);
  await inputElement.sendKeys(Key.ENTER);
  console.log(`Scanned table code: ${tableCode}`);
}

module.exports = scanTable;

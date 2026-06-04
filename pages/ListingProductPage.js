const { By, until } = require("selenium-webdriver");

class ListingProductPage {
  constructor(driver) {
    this.driver = driver;
  }

  listingProductBtn = By.xpath("//a[contains(@id,'listing_product')]");

  // Tìm thẻ chứa text, sau đó lùi lên trên (ancestor) để tìm thẻ div bao ngoài có class chứa '-control'
  channelDropdown = By.xpath(
    "//div[text()='Chọn kênh bán hàng']/ancestor::div[contains(@class, '-control')]",
  );

  // Thay đổi: dùng contains(., ...) thay vì contains(text(), ...)
  // Đồng thời dùng class chứa '-option' để chỉ định chính xác các hàng trong menu
  channelItem = (channelName) =>
    By.xpath(
      `//div[contains(@class, '-menu')]//div[contains(@class, '-option') and contains(., '${channelName}')]`,
    );

  continueBtn = By.xpath("//button[contains(.,'Tiếp theo')]");

  sellProductBtn = By.xpath("//button[contains(.,'Đăng bán')]");

  async accessListingProduct() {
    await this.driver.sleep(2000);
    const originalTab = await this.driver.getWindowHandle();
    const listingBtn = await this.driver.wait(
      until.elementLocated(this.listingProductBtn),
      10000,
    );
    await this.driver.wait(until.elementIsVisible(listingBtn), 10000);
    await listingBtn.click();

    await this.driver.wait(async () => {
      const handles = await this.driver.getAllWindowHandles();
      return handles.length > 1;
    }, 10000);

    const allTabs = await this.driver.getAllWindowHandles();
    const newTab = allTabs.find((tab) => tab !== originalTab);
    await this.driver.switchTo().window(newTab);
    console.log("Switched to listing tab");
  }

  async selectSalesChannel() {
    // 1. Chờ element xuất hiện trong DOM (tối đa 10 giây)
    const dropdown = await this.driver.wait(
      until.elementLocated(this.channelDropdown),
      10000,
    );

    // 2. Chờ element hiển thị trên màn hình và có thể tương tác
    await this.driver.wait(until.elementIsVisible(dropdown), 10000);

    // 3. Cuộn chuột đến element (đề phòng dropdown nằm ngoài tầm mắt)
    await this.driver.executeScript(
      "arguments[0].scrollIntoView(true);",
      dropdown,
    );

    // 4. Thực hiện click
    await dropdown.click();
    console.log("Clicked sales channel dropdown");
  }

  async selectChannelOption(channelName) {
    // 1. Định vị item cần chọn (bây giờ XPath đã quét được toàn bộ text bao gồm cả sau icon)
    const itemLocator = this.channelItem(channelName);

    // 2. Chờ item đó xuất hiện trong DOM
    const option = await this.driver.wait(
      until.elementLocated(itemLocator),
      10000,
    );

    // 3. Cuộn đến option đó (đề phòng danh sách dài bị khuất)
    await this.driver.executeScript(
      "arguments[0].scrollIntoView(true);",
      option,
    );

    // 4. Click thẳng bằng JavaScript để né việc click lệch vào cái Icon ngôi nhà
    await this.driver.executeScript("arguments[0].click();", option);
    console.log(`Selected channel: ${channelName} via JS successfully`);
  }

  async clickContinue() {
    const continueButton = await this.driver.wait(
      until.elementLocated(this.continueBtn),
      10000,
    );
    await this.driver.wait(until.elementIsVisible(continueButton), 10000);
    await continueButton.click();
  }

  async clickSellProduct() {
    const sellButton = await this.driver.wait(
      until.elementLocated(this.sellProductBtn),
      10000,
    );
    await this.driver.wait(until.elementIsVisible(sellButton), 10000);
    await this.driver.sleep(2000); // thêm sleep để đảm bảo mọi thứ đã sẵn sàng trước khi click
    await sellButton.click();
    await this.driver.sleep(2000); // thêm sleep để chờ phản hồi sau khi click
  }
}

module.exports = ListingProductPage;

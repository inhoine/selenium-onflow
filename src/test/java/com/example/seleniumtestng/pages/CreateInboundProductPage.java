package com.example.seleniumtestng.pages;

import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CreateInboundProductPage extends BasePage {
    private final By dropdownInboundBtn = By.xpath("//button[contains(.,'Tạo phiếu') or contains(.,'Tao phieu')]");
    private final By createInboundMenuItem = By.xpath("//button[contains(.,'Tạo phiếu nhập') or contains(.,'Tao phieu nhap')]");
    private final By warehouseField = By.xpath("//div[contains(.,'Chọn địa chỉ lấy hàng') or contains(.,'Chon dia chi lay hang')]/ancestor::div[contains(@class,'-control')]");
    private final By warehouseInput = By.xpath("//div[contains(.,'Chọn địa chỉ lấy hàng') or contains(.,'Chon dia chi lay hang')]/following::input[1]");
    private final By supplierField = By.xpath("//div[contains(.,'Chọn nhà cung cấp') or contains(.,'Chon nha cung cap')]/ancestor::div[contains(@class,'-control')]");
    private final By supplierInput = By.xpath("//div[contains(.,'Chọn nhà cung cấp') or contains(.,'Chon nha cung cap')]/following::input[1]");
    private final By reactSelectControls = By.cssSelector("div[class*='-control']");
    private final By referenceField = By.cssSelector("input[name='shipmentReferenceCode']");
    private final By addProductBtn = By.xpath("//*[normalize-space()='Thêm sản phẩm' or normalize-space()='Them san pham']");
    private final By addNewProductBtn = By.xpath("//button[normalize-space()='Thêm sản phẩm mới' or normalize-space()='Them san pham moi']");
    private final By productDropdowns = By.xpath("//div[contains(.,'Chọn sản phẩm') or contains(.,'Chon san pham')]/ancestor::div[contains(@class,'-control')]");
    private final By confirmItemInboundBtn = By.xpath("//button[normalize-space()='Xác nhận' or normalize-space()='Xac nhan']");
    private final By lengthField = By.xpath("//input[contains(@placeholder,'Dài') or contains(@placeholder,'Dai')]");
    private final By widthField = By.xpath("//input[contains(@placeholder,'Rộng') or contains(@placeholder,'Rong')]");
    private final By heightField = By.xpath("//input[contains(@placeholder,'Cao')]");
    private final By createInboundBtn = By.xpath("//button[normalize-space()='Tạo mới' or normalize-space()='Tao moi']");
    private final By confirmInboundBtn = By.xpath("//button[normalize-space()='Tạo và duyệt phiếu nhập' or normalize-space()='Tao va duyet phieu nhap']");
    private final By inboundCodeText = By.xpath("//h5[contains(.,'Mã nhập kho') or contains(.,'Ma nhap kho')]");

    public CreateInboundProductPage(WebDriver driver) {
        super(driver);
    }

    public void openCreateInboundForm() {
        click(dropdownInboundBtn);
        click(createInboundMenuItem);
    }

    public void selectWarehouse(String keyword) {
        selectReactOption(warehouseField, warehouseInput, keyword, 0);
    }

    public void selectSupplier(String keyword) {
        selectReactOption(supplierField, supplierInput, keyword, 1);
    }

    public String inputReference() {
        String reference = "REF-" + String.valueOf(System.currentTimeMillis()).substring(7);
        type(referenceField, reference);
        return reference;
    }

    public boolean continueIfPresent() {
        return clickTextActionIfPresent("tiep tuc", 0, false, 5000);
    }

    public void clickAddProduct() {
        click(addProductBtn);
    }

    public void clickAddProductForPackage(int packageIndex) {
        clickTextAction("them san pham", packageIndex, true);
    }

    public void addProductToInbound(String productKeyword, int quantity) {
        WebElement dropdown = last(productDropdowns);
        jsClick(dropdown);
        WebElement input = dropdown.findElement(By.cssSelector("input"));
        input.sendKeys(productKeyword);
        visible(By.xpath("//*[contains(@class,'-menu')]//*[contains(normalize-space(.)," + xpathText(productKeyword) + ")]"));
        input.sendKeys(Keys.ENTER);

        List<WebElement> qtyInputs = all(By.xpath("//input[contains(@name,'productQty')]"));
        WebElement qtyInput = qtyInputs.get(qtyInputs.size() - 1);
        qtyInput.clear();
        qtyInput.sendKeys(String.valueOf(quantity));
    }

    public void addNewProductRow() {
        int before = all(productDropdowns).size();
        click(addNewProductBtn);
        wait.until(driver -> all(productDropdowns).size() > before);
    }

    public void addPackageRow() {
        clickTextAction("them kien", 0, false);
    }

    public void confirmItems() {
        click(confirmItemInboundBtn);
    }

    public void inputProductDimensions(int length, int width, int height) {
        type(lengthField, String.valueOf(length));
        type(widthField, String.valueOf(width));
        type(heightField, String.valueOf(height));
    }

    public void inputPackageDimensions(int packageIndex, int length, int width, int height) {
        type(indexedVisible(lengthField, packageIndex), String.valueOf(length));
        type(indexedVisible(widthField, packageIndex), String.valueOf(width));
        type(indexedVisible(heightField, packageIndex), String.valueOf(height));
    }

    public void confirmCreateInbound() {
        clickCreateInboundDropdown();
        if (!clickTextActionIfPresent("tao va duyet phieu nhap", 0, false, 5000)) {
            clickCreateInboundDropdown();
            if (!clickTextActionIfPresent("tao va duyet phieu nhap", 0, false, 5000)) {
                throw new TimeoutException("Create and approve inbound option not found. Buttons=" + buttonSummary());
            }
        }
        clickTextActionIfPresent("xac nhan", 0, false, 3000);
    }

    public String getInboundCode() {
        String inboundCode = shortWait(30000).until(driver -> {
            String fromUrl = firstInboundCode(driver.getCurrentUrl());
            if (fromUrl != null) {
                return fromUrl;
            }
            for (WebElement element : all(inboundCodeText)) {
                if (!displayed(element)) {
                    continue;
                }
                String fromLabel = firstInboundCode(element.getText());
                if (fromLabel != null) {
                    return fromLabel;
                }
            }
            String fromBody = firstInboundCode(driver.findElement(By.tagName("body")).getText());
            return fromBody == null ? null : fromBody;
        });
        System.out.println("Created inbound PO: " + inboundCode);
        return inboundCode;
    }

    private String firstInboundCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("NHIV\\d+").matcher(value);
        return matcher.find() ? matcher.group() : null;
    }

    private void clickCreateInboundDropdown() {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (clickButtonByNormalizedTextIfPresent("tao moi", 3000)
                    || clickButtonByNormalizedTextIfPresent("tao phieu nhap", 3000)) {
                return;
            }
            if (!clickTextActionIfPresent("tiep tuc", 0, false, 5000)) {
                break;
            }
        }
        throw new TimeoutException("Create inbound action button not found. Buttons=" + buttonSummary());
    }

    private boolean clickButtonByNormalizedTextIfPresent(String normalizedText, long timeoutMillis) {
        try {
            WebElement button = shortWait(timeoutMillis).until(driver -> {
                for (WebElement candidate : all(By.cssSelector("button"))) {
                    if (!displayed(candidate) || !candidate.isEnabled()) {
                        continue;
                    }
                    if (normalizeForMatch(candidate.getText()).contains(normalizedText)) {
                        return candidate;
                    }
                }
                return null;
            });
            try {
                button.click();
            } catch (RuntimeException e) {
                jsClick(button);
            }
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void selectReactOption(By field, By inputLocator, String keyword, int fallbackControlIndex) {
        WebElement dropdown = reactSelectControl(field, fallbackControlIndex);
        try {
            dropdown.click();
        } catch (RuntimeException e) {
            jsClick(dropdown);
        }
        WebElement input = inputInside(dropdown, inputLocator);
        input.sendKeys(keyword);
        visible(By.xpath("//*[contains(@class,'-menu')]//*[contains(normalize-space(.)," + xpathText(keyword) + ")]"));
        input.sendKeys(Keys.ENTER);
    }

    private WebElement reactSelectControl(By preferredLocator, int fallbackControlIndex) {
        try {
            return shortWait(3000).until(driver -> {
                List<WebElement> preferred = visibleElements(preferredLocator);
                return preferred.isEmpty() ? null : preferred.get(0);
            });
        } catch (TimeoutException ignored) {
            return indexedVisible(reactSelectControls, fallbackControlIndex);
        }
    }

    private WebElement inputInside(WebElement dropdown, By fallbackInputLocator) {
        try {
            return dropdown.findElement(By.cssSelector("input"));
        } catch (RuntimeException ignored) {
            return visible(fallbackInputLocator);
        }
    }

    private WebElement last(By locator) {
        return wait.until(driver -> {
            List<WebElement> elements = all(locator);
            return elements.isEmpty() ? null : elements.get(elements.size() - 1);
        });
    }

    private WebElement indexedVisible(By locator, int index) {
        return wait.until(driver -> {
            List<WebElement> elements = visibleElements(locator);
            return elements.size() > index ? elements.get(index) : null;
        });
    }

    private void clickTextAction(String normalizedText, int index, boolean excludeAddNewProduct) {
        if (clickTextActionIfPresent(normalizedText, index, excludeAddNewProduct, 15000)) {
            return;
        }
        throw new TimeoutException("Button not found: text="
                + normalizedText
                + ", index="
                + index
                + ", buttons="
                + buttonSummary());
    }

    private boolean clickTextActionIfPresent(
            String normalizedText,
            int index,
            boolean excludeAddNewProduct,
            long timeoutMillis) {
        try {
            shortWait(timeoutMillis).until(driver -> {
                Object clicked = ((JavascriptExecutor) driver).executeScript(
                        "const target = arguments[0];"
                                + "const index = arguments[1];"
                                + "const excludeAddNew = arguments[2];"
                                + "const norm = value => (value || '')"
                                + "  .normalize('NFD').replace(/[\\u0300-\\u036f]/g, '')"
                                + "  .replace(/đ/g, 'd').replace(/Đ/g, 'D')"
                                + "  .toLowerCase().replace(/\\s+/g, ' ').trim();"
                                + "const visible = el => {"
                                + "  const style = window.getComputedStyle(el);"
                                + "  const rect = el.getBoundingClientRect();"
                                + "  return style.visibility !== 'hidden' && style.display !== 'none'"
                                + "    && rect.width > 0 && rect.height > 0;"
                                + "};"
                                + "const matches = el => {"
                                + "  const text = norm(el.innerText || el.textContent);"
                                + "  return text.includes(target) && (!excludeAddNew || !text.includes('them san pham moi'));"
                                + "};"
                                + "const selector = '*';"
                                + "const elements = Array.from(document.querySelectorAll(selector))"
                                + "  .filter(el => visible(el) && matches(el))"
                                + "  .filter(el => !Array.from(el.children).some(child => visible(child) && matches(child)));"
                                + "if (!elements.length) return false;"
                                + "const element = elements[Math.min(index, elements.length - 1)];"
                                + "const clickable = element.closest('button,a,[role=\"button\"],[onclick],.btn,[class*=\"button\"],[class*=\"cursor\"]') || element;"
                                + "clickable.scrollIntoView({block:'center'});"
                                + "clickable.click();"
                                + "return true;",
                        normalizedText,
                        index,
                        excludeAddNewProduct);
                return Boolean.TRUE.equals(clicked) ? true : null;
            });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private WebElement indexedVisibleButton(String normalizedText, int index) {
        try {
            return wait.until(driver -> {
                List<WebElement> elements = visibleTextElements(normalizedText, false);
                if (elements.isEmpty()) {
                    return null;
                }
                return elements.get(Math.min(index, elements.size() - 1));
            });
        } catch (TimeoutException e) {
            throw new TimeoutException("Button not found: text="
                    + normalizedText
                    + ", index="
                    + index
                    + ", buttons="
                    + buttonSummary(), e);
        }
    }

    private List<WebElement> visibleTextElements(String normalizedText, boolean excludeAddNewProduct) {
        return all(By.xpath("//*[normalize-space()]")).stream()
                .filter(this::displayed)
                .filter(element -> textMatches(element, normalizedText, excludeAddNewProduct))
                .filter(element -> !hasMatchingChild(element, normalizedText, excludeAddNewProduct))
                .collect(Collectors.toList());
    }

    private boolean textMatches(WebElement element, String normalizedText, boolean excludeAddNewProduct) {
        String text = normalizeForMatch(element.getText());
        return text.contains(normalizedText)
                && (!excludeAddNewProduct || !text.contains("them san pham moi"));
    }

    private boolean hasMatchingChild(WebElement element, String normalizedText, boolean excludeAddNewProduct) {
        for (WebElement child : element.findElements(By.xpath(".//*[normalize-space()]"))) {
            if (displayed(child) && textMatches(child, normalizedText, excludeAddNewProduct)) {
                return true;
            }
        }
        return false;
    }

    private String buttonSummary() {
        return all(By.cssSelector("button")).stream()
                .filter(this::displayed)
                .map(button -> {
                    String text = button.getText().replaceAll("\\s+", " ").trim();
                    return text + " => " + normalizeForMatch(text);
                })
                .collect(Collectors.joining(" | "));
    }

    private List<WebElement> visibleElements(By locator) {
        return all(locator).stream()
                .filter(this::displayed)
                .collect(Collectors.toList());
    }

    private void type(WebElement element, String value) {
        jsClick(element);
        element.clear();
        element.sendKeys(value);
    }
}

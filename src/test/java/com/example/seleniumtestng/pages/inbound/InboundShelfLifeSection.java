package com.example.seleniumtestng.pages.inbound;

import com.example.seleniumtestng.config.ConfigReader;
import com.example.seleniumtestng.pages.BasePage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Handles shelf-life (HSD / NSX) date inputs and validations during WMS Inbound Inspection:
 * - Parsing system warning messages to detect mandatory shelf-life or minimum expiry days
 * - Calculating valid matching NSX & HSD dates based on configured or system-enforced rules
 * - Injecting mismatched or invalid dates for negative testing (FEFO validation)
 */
public class InboundShelfLifeSection extends BasePage {

    private final By manufactureDateFields = By.cssSelector(
            "input[name*='manufact'], input[name*='mfg'], input[name*='production'], "
                    + "input[name*='produce'], input[name*='nsx']");
    private final By manufactureDateByLabel = By.cssSelector("input[name='manufacturing_date']");
    private final By expiryDateFields = By.cssSelector(
            "input[name*='expiry'], input[name*='expire'], input[name*='expired'], "
                    + "input[name*='expiration'], input[name*='exp_date']");
    private final By expiryDateByLabel = By.cssSelector("input[name='expiry_date']");

    public InboundShelfLifeSection(WebDriver driver) {
        super(driver);
    }

    public void inputShelfLifeDatesIfPresent() {
        int shelfLifeDays = Math.max(1, shelfLifeDays());
        Integer minimumExpiryDays = minimumExpiryDays();
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = LocalDate.parse(ConfigReader.getOrDefault("INBOUND_EXPIRY_DATE", "2027-12-31"));
        LocalDate manufactureDate = expiryDate.minusDays(shelfLifeDays - 1L);

        if (minimumExpiryDays != null
                && (expiryDate.isBefore(today.plusDays(minimumExpiryDays)) || manufactureDate.isAfter(today))) {
            expiryDate = today.plusDays(minimumExpiryDays);
            manufactureDate = expiryDate.minusDays(shelfLifeDays - 1L);
        } else if (manufactureDate.isAfter(today)) {
            manufactureDate = today;
            expiryDate = manufactureDate.plusDays(shelfLifeDays - 1L);
        }

        boolean manufactureFilled = fillDateIfPresent(manufactureDateByLabel, manufactureDateFields, manufactureDate);
        boolean expiryFilled = fillDateIfPresent(expiryDateByLabel, expiryDateFields, expiryDate);
        if (manufactureFilled && expiryFilled) {
            return;
        }

        LocalDate fallbackManufactureDate = today;
        LocalDate fallbackExpiryDate = fallbackManufactureDate.plusDays(shelfLifeDays - 1L);
        if (minimumExpiryDays != null && fallbackExpiryDate.isBefore(today.plusDays(minimumExpiryDays))) {
            fallbackExpiryDate = today.plusDays(minimumExpiryDays);
        }
        System.out.println("Fallback to nearest valid shelf-life dates: manufacture_date="
                + fallbackManufactureDate
                + ", expiry_date="
                + fallbackExpiryDate);
        fillDateIfPresent(manufactureDateByLabel, manufactureDateFields, fallbackManufactureDate);
        fillDateIfPresent(expiryDateByLabel, expiryDateFields, fallbackExpiryDate);
    }

    public void clearShelfLifeDatesIfPresent() {
        clearDateIfPresent(manufactureDateByLabel, manufactureDateFields);
        clearDateIfPresent(expiryDateByLabel, expiryDateFields);
    }

    public void inputMismatchedShelfLifeDatesIfPresent() {
        int shelfLifeDays = Math.max(1, shelfLifeDays());
        Integer minimumExpiryDays = minimumExpiryDays();
        LocalDate manufactureDate = LocalDate.now();
        LocalDate expiryDate = manufactureDate.plusDays(shelfLifeDays + 5L);
        if (minimumExpiryDays != null && expiryDate.isBefore(LocalDate.now().plusDays(minimumExpiryDays))) {
            expiryDate = LocalDate.now().plusDays(minimumExpiryDays + 5L);
        }
        System.out.println("Inputting mismatched shelf-life dates: manufacture_date="
                + manufactureDate
                + ", expiry_date="
                + expiryDate);
        fillDateIfPresent(manufactureDateByLabel, manufactureDateFields, manufactureDate);
        fillDateIfPresent(expiryDateByLabel, expiryDateFields, expiryDate);
    }

    public int shelfLifeDays() {
        Integer fromWarning = shelfLifeDaysFromWarning();
        if (fromWarning != null) {
            return fromWarning;
        }

        String configured = ConfigReader.get("INBOUND_SHELF_LIFE_DAYS");
        if (configured != null && !configured.isBlank()) {
            return Integer.parseInt(configured.trim());
        }
        return 182;
    }

    public Integer shelfLifeDaysFromWarning() {
        try {
            String bodyText = driver.findElement(By.tagName("body")).getText();
            Matcher matcher = Pattern.compile("hệ thống \\((\\d+) ngày\\)").matcher(bodyText);
            if (!matcher.find()) {
                return null;
            }
            return Integer.parseInt(matcher.group(1));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public Integer minimumExpiryDays() {
        try {
            String bodyText = driver.findElement(By.tagName("body")).getText();
            Matcher matcher = Pattern.compile("Minimum\\s+(\\d+)\\s+ng\\S*y", Pattern.CASE_INSENSITIVE).matcher(bodyText);
            if (!matcher.find()) {
                return null;
            }
            return Integer.parseInt(matcher.group(1));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public boolean fillDateIfPresent(By primaryLocator, By fallbackLocator, LocalDate date) {
        WebElement field = firstEditable(primaryLocator);
        if (field == null) {
            field = firstEditable(fallbackLocator);
        }
        if (field == null) {
            return false;
        }
        setFlatpickrDate(field, toDisplayDate(date.toString()));
        return true;
    }

    public boolean clearDateIfPresent(By primaryLocator, By fallbackLocator) {
        WebElement field = firstEditable(primaryLocator);
        if (field == null) {
            field = firstEditable(fallbackLocator);
        }
        if (field == null) {
            return false;
        }
        setInputValue(field, "");
        return true;
    }

    private String toDisplayDate(String isoDate) {
        return LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private WebElement firstEditable(By locator) {
        for (WebElement element : all(locator)) {
            if (isEditable(element)) {
                return element;
            }
        }
        return null;
    }

    private boolean isEditable(WebElement input) {
        try {
            return input.isDisplayed() && input.isEnabled() && input.getDomAttribute("readonly") == null;
        } catch (RuntimeException e) {
            return false;
        }
    }
}


package com.example.seleniumtestng.pages.packing;

import com.example.seleniumtestng.pages.BasePage;
import com.example.seleniumtestng.utils.Log;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PackingProgressTracker extends BasePage {
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("da dong goi\\s+(\\d+)\\s*/\\s*(\\d+)\\s+don");
    private static final Pattern TRACKING_PATTERN = Pattern.compile("(?i)(?:đơn hàng|don hang)\\s+([A-Z0-9][A-Z0-9-]+)");

    private final By currentTrackingCode = By.xpath("//*[contains(normalize-space(.),'Bạn đang đóng gói cho đơn hàng')"
            + " or contains(normalize-space(.),'Ban dang dong goi cho don hang')]");
    private final By productRows = By.xpath("//tr[.//div[contains(@id,'barcode_')]]");
    private final By scanPickUpField;

    public PackingProgressTracker(WebDriver driver, By scanPickUpField) {
        super(driver);
        this.scanPickUpField = scanPickUpField;
    }

    public PackingProgress readPackingProgressNow() {
        // Fast path: find elements specifically containing progress text
        List<WebElement> candidates = driver.findElements(By.xpath(
                "//*[contains(text(),'đóng gói') or contains(text(),'Đóng gói') or contains(text(),'dong goi')]"));
        for (WebElement candidate : candidates) {
            try {
                if (candidate.isDisplayed()) {
                    String text = normalizeScanText(candidate.getText());
                    Matcher matcher = PROGRESS_PATTERN.matcher(text);
                    if (matcher.find()) {
                        return new PackingProgress(
                                Integer.parseInt(matcher.group(1)),
                                Integer.parseInt(matcher.group(2)));
                    }
                    WebElement parent = candidate.findElement(By.xpath(".."));
                    String parentText = normalizeScanText(parent.getText());
                    Matcher parentMatcher = PROGRESS_PATTERN.matcher(parentText);
                    if (parentMatcher.find()) {
                        return new PackingProgress(
                                Integer.parseInt(parentMatcher.group(1)),
                                Integer.parseInt(parentMatcher.group(2)));
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }

        // Fallback: only read body text if not found via candidates
        try {
            String normalizedText = normalizeScanText(driver.findElement(By.tagName("body")).getText());
            Matcher matcher = PROGRESS_PATTERN.matcher(normalizedText);
            if (matcher.find()) {
                return new PackingProgress(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)));
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    public PackingProgress getPackingProgress(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> readPackingProgressNow());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public boolean isPackingCompleteNow() {
        PackingProgress progress = readPackingProgressNow();
        return progress != null && progress.packed() == progress.total();
    }

    public void waitForPackingProgressToAdvance(String trackingCode, PackingProgress progressBeforeMaterial) {
        try {
            shortWait(8000).until(driver -> {
                if (isOrderPackingFinishedAndClosed(trackingCode)) {
                    Log.info("Order {} finished packing and returned to basket scan screen", trackingCode);
                    return true;
                }
                PackingProgress progress = readPackingProgressNow();
                if (progress != null) {
                    if (progressBeforeMaterial == null
                            || progress.packed() > progressBeforeMaterial.packed()
                            || progress.packed() == progress.total()) {
                        return true;
                    }
                }
                return null;
            });
        } catch (RuntimeException error) {
            if (isOrderPackingFinishedAndClosed(trackingCode)) {
                Log.info("Order {} finished packing and returned to basket scan screen (caught timeout)", trackingCode);
                return;
            }
            PackingProgress progress = readPackingProgressNow();
            String currentProgress = progress == null ? "unknown" : progress.packed() + "/" + progress.total();
            throw new IllegalStateException("Packing progress did not advance after material scan. tracking="
                    + trackingCode
                    + ", before="
                    + (progressBeforeMaterial == null ? "none" : progressBeforeMaterial.packed() + "/" + progressBeforeMaterial.total())
                    + ", current="
                    + currentProgress
                    + ", screen="
                    + summarizeScreenText(), error);
        }
    }

    public boolean isOrderPackingFinishedAndClosed(String trackingCode) {
        try {
            List<WebElement> rows = driver.findElements(productRows);
            boolean noProductRows = rows.isEmpty() || rows.stream().noneMatch(WebElement::isDisplayed);
            WebElement scanInput = findScanPickupInputNow();
            if (noProductRows && scanInput != null) {
                return true;
            }
            String screen = summarizeScreenText();
            String normalized = normalizeScanText(screen);
            if (trackingCode != null && screen.contains(trackingCode)) {
                if (normalized.contains("vua cap nhat") || normalized.contains("don hang gan nhat") || normalized.contains("tiep tuc dong goi")) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    public String getCurrentPackingTrackingCodeIfPresent(long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(currentTrackingCode)) {
                    try {
                        if (element.isDisplayed() && !element.getText().trim().isBlank()) {
                            String text = element.getText().trim();
                            String trackingCode = extractCurrentTrackingCode(text);
                            if (trackingCode != null) {
                                return trackingCode;
                            }
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public String getNewPackingTrackingCodeIfPresent(Set<String> visitedTrackingCodes, long timeoutMillis) {
        try {
            return shortWait(timeoutMillis).until(driver -> {
                for (WebElement element : driver.findElements(currentTrackingCode)) {
                    try {
                        if (element.isDisplayed() && !element.getText().trim().isBlank()) {
                            String text = element.getText().trim();
                            String trackingCode = extractCurrentTrackingCode(text);
                            if (trackingCode != null && !visitedTrackingCodes.contains(trackingCode)) {
                                return trackingCode;
                            }
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public String extractCurrentTrackingCode(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = TRACKING_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    public String summarizeScreenText() {
        try {
            String text = driver.findElement(By.tagName("body")).getText().replaceAll("\\s+", " ").trim();
            return text.length() <= 600 ? text : text.substring(0, 600);
        } catch (RuntimeException e) {
            return "<unable to read screen text>";
        }
    }

    public String normalizeScanText(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private WebElement findScanPickupInputNow() {
        for (WebElement element : driver.findElements(scanPickUpField)) {
            try {
                if (element.isDisplayed() && element.isEnabled()) {
                    return element;
                }
            } catch (RuntimeException ignored) {
            }
        }
        for (WebElement input : driver.findElements(By.cssSelector("input"))) {
            try {
                if (!input.isDisplayed() || !input.isEnabled()) {
                    continue;
                }
                String placeholder = normalizeScanText(input.getAttribute("placeholder"));
                if (placeholder.contains("bang ke") || placeholder.contains("xe") || placeholder.contains("ro")) {
                    return input;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    public static final class PackingProgress {
        private final int packed;
        private final int total;

        public PackingProgress(int packed, int total) {
            this.packed = packed;
            this.total = total;
        }

        public int packed() {
            return packed;
        }

        public int total() {
            return total;
        }
    }
}


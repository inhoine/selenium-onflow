package com.example.seleniumtestng.tests;

import com.example.seleniumtestng.base.DriverFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

public class WmsCurrentUiInspectorTest {
    private static final Duration LONG_WAIT = Duration.ofSeconds(18);
    private static final Duration SHORT_WAIT = Duration.ofSeconds(4);
    private static final Duration LOGIN_STEP_WAIT = Duration.ofSeconds(8);
    private static final long MENU_SETTLE_MS = longEnvOrDefault("WMS_MENU_SETTLE_MS", 550);
    private static final long GROUP_TOGGLE_MS = longEnvOrDefault("WMS_GROUP_TOGGLE_MS", 350);
    private static final long MODAL_OPEN_MS = longEnvOrDefault("WMS_MODAL_OPEN_MS", 450);
    private static final long TINY_SETTLE_MS = longEnvOrDefault("WMS_TINY_SETTLE_MS", 200);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final List<GroupSpec> STG_MENU = List.of(
            new GroupSpec("Nhập kho", List.of("Danh sách", "Kiểm hàng nhập", "Chờ cất hàng")),
            new GroupSpec("Xử lý đơn hàng", List.of("Danh sách", "Đơn chưa xác định")),
            new GroupSpec("Lấy hàng & xuất kho", List.of("Tạo yêu cầu xuất", "Danh sách yêu cầu xuất")),
            new GroupSpec("Đóng gói", List.of("Rổ chờ đóng gói", "Nhận xe hàng", "Đóng gói B2C", "Đóng gói B2B", "Campaign")),
            new GroupSpec("Bàn giao", List.of()),
            new GroupSpec("Hàng hoàn", List.of("Đơn hoàn chờ nhận", "Danh sách phiếu hoàn", "Kiểm hàng hoàn", "Hàng hoàn đã kiểm")),
            new GroupSpec("Sản phẩm", List.of("Danh sách", "Nhà phân phối", "Điều chỉnh tồn", "Chuyển vị trí", "Chuyển đổi")),
            new GroupSpec("Kiểm kê kho", List.of("Danh sách", "Sai lệch tồn kho")),
            new GroupSpec("Báo cáo", List.of("Nhập kho theo PO", "Tồn kho theo khách hàng", "Báo cáo đơn hàng",
                    "Hiệu suất bàn đóng gói", "Sản lượng theo giờ", "Tỷ lệ lấp đầy kho", "Trung tâm báo cáo")));

    private static final List<ProdGroupSpec> PROD_MENU = List.of(
            new ProdGroupSpec(0, "Báo cáo nhập kho", "Nhập kho", List.of("Theo PO", "Theo thời gian")),
            new ProdGroupSpec(1, "Báo cáo sản phẩm", "Sản phẩm", List.of(
                    "Theo hạn sử dụng",
                    "Theo ngày lưu kho",
                    "Theo lô lot",
                    "Theo khách hàng",
                    "Báo cáo hàng hết hạn")),
            new ProdGroupSpec(2, "Báo cáo tổng hợp", "Báo cáo", List.of(
                    "Đơn hàng",
                    "Lấy hàng",
                    "Đóng gói",
                    "Hàng hoàn trả",
                    "Kiểm kê kho",
                    "Sản lượng",
                    "Vị trí lưu kho",
                    "Tải file")),
            new ProdGroupSpec(3, "Nhập kho", "Nhập kho", List.of("Danh sách", "Kiểm hàng", "Chờ lưu kho")),
            new ProdGroupSpec(4, "Xuất kho", "Xuất kho", List.of("Tạo yêu cầu", "Danh sách")),
            new ProdGroupSpec(5, "Đóng gói", "Đóng gói", List.of(
                    "Rổ chờ đóng gói",
                    "Nhận xe hàng",
                    "Đóng gói B2C",
                    "Đóng gói B2B",
                    "Campaign")),
            new ProdGroupSpec(6, "Đơn hàng", "Đơn hàng", List.of("Danh sách", "Không xác định")),
            new ProdGroupSpec(7, "Xử lý hàng hoàn", "Xử lý hàng hoàn", List.of(
                    "Đơn chờ nhận",
                    "Phiếu nhận hoàn",
                    "Kiểm hàng hoàn",
                    "Hàng đã kiểm")),
            new ProdGroupSpec(8, "Bàn giao", "Bàn giao", List.of()),
            new ProdGroupSpec(9, "Kiểm kê kho", "Kiểm kê kho", List.of("Danh sách", "Có vấn đề", "Báo cáo")),
            new ProdGroupSpec(10, "Sản phẩm", "Sản phẩm", List.of(
                    "Danh sách",
                    "Điều chỉnh tồn kho",
                    "Điều chỉnh vị trí",
                    "Nhà phân phối",
                    "Chuyển đổi")),
            new ProdGroupSpec(11, "Nhân viên", "Nhân viên", List.of("Danh sách", "Phân quyền", "Cấu hình KPI")),
            new ProdGroupSpec(12, "Cấu hình kho", "Cấu hình kho", List.of(
                    "Bàn đóng gói",
                    "Vị trí lưu kho",
                    "Đơn vị lấy hàng",
                    "Cấu hình SLA",
                    "Cấu hình lấy hàng",
                    "Thiết bị chứa hàng")));

    private static final List<String> CONFIG_CARDS = List.of(
            "Danh sách",
            "Phân quyền",
            "Vị trí lưu kho",
            "Bàn đóng gói",
            "Thiết bị kho",
            "Cấu hình lấy hàng",
            "Cấu hình SLA",
            "Cấu hình KPI",
            "Đối tác vận chuyển");

    @Test
    public void inspectStgMenuByUiClickOnly() throws Exception {
        Environment stg = new Environment(
                "stg",
                envOrDefault("STG_WMS_BASE_URL", "https://stg-wms.onflow.vn"),
                requiredEnv("STG_WMS_EMAIL"),
                requiredEnv("STG_WMS_PASSWORD"));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path runDir = Path.of("target", "wms-stg-menu-click-review", timestamp);
        Files.createDirectories(runDir);

        WebDriver driver = DriverFactory.create(envOrDefault("BROWSER", "chrome"));
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(25));
            login(driver, stg);
            settle(driver);
            saveScreenshot(driver, runDir.resolve("00-after-login.png"));

            StgMenuReport report = new StgMenuReport();
            report.generatedAt = LocalDateTime.now().toString();
            report.environment = stg.baseUrl;
            report.navigationMethod = "STG only. Login, select FC if required, then access pages by clicking sidebar menu/submenu and config modal cards. No direct page URLs were used after login.";
            report.visits = walkStgMenu(driver, stg, runDir.resolve("pages"));

            MAPPER.writeValue(runDir.resolve("stg-menu-click-report.json").toFile(), report);
            Files.writeString(runDir.resolve("summary.txt"), buildSummary(report), StandardCharsets.UTF_8);

            System.out.println("WMS_STG_MENU_CLICK_REVIEW_DIR=" + runDir.toAbsolutePath());
            System.out.println("STG_VISITED=" + report.visits.size());
            System.out.println("STG_ERRORS=" + report.visits.stream().filter(visit -> visit.error != null).count());
        } finally {
            driver.quit();
        }
    }

    @Test
    public void inspectStgConfigModalOnly() throws Exception {
        Environment stg = new Environment(
                "stg",
                envOrDefault("STG_WMS_BASE_URL", "https://stg-wms.onflow.vn"),
                requiredEnv("STG_WMS_EMAIL"),
                requiredEnv("STG_WMS_PASSWORD"));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path runDir = Path.of("target", "wms-stg-config-click-review", timestamp);
        Files.createDirectories(runDir);

        WebDriver driver = DriverFactory.create(envOrDefault("BROWSER", "chrome"));
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(25));
            login(driver, stg);
            settle(driver);
            saveScreenshot(driver, runDir.resolve("00-after-login.png"));

            StgMenuReport report = new StgMenuReport();
            report.generatedAt = LocalDateTime.now().toString();
            report.environment = stg.baseUrl;
            report.navigationMethod = "STG config modal only. Login, select FC if required, open Quản trị & cấu hình modal, then click each icon/card inside the modal.";
            report.visits = walkConfigModalOnly(driver, stg, runDir.resolve("pages"));

            MAPPER.writeValue(runDir.resolve("stg-config-click-report.json").toFile(), report);
            Files.writeString(runDir.resolve("summary.txt"), buildSummary(report), StandardCharsets.UTF_8);

            System.out.println("WMS_STG_CONFIG_CLICK_REVIEW_DIR=" + runDir.toAbsolutePath());
            System.out.println("STG_CONFIG_VISITED=" + report.visits.size());
            System.out.println("STG_CONFIG_ERRORS=" + report.visits.stream().filter(visit -> visit.error != null).count());
        } finally {
            driver.quit();
        }
    }

    @Test
    public void inspectProdMenuByUiClickOnly() throws Exception {
        Environment prod = new Environment(
                "prod",
                envOrDefault("PRD_WMS_BASE_URL", envOrDefault("PROD_WMS_BASE_URL", "https://wms.onflow.vn")),
                requiredAnyEnv("PRD_WMS_EMAIL", "PROD_WMS_EMAIL"),
                requiredAnyEnv("PRD_WMS_PASSWORD", "PROD_WMS_PASSWORD"));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path runDir = Path.of("target", "wms-prod-menu-click-review", timestamp);
        Files.createDirectories(runDir);

        WebDriver driver = DriverFactory.create(envOrDefault("BROWSER", "chrome"));
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(25));
            login(driver, prod, true);
            settle(driver);
            saveScreenshot(driver, runDir.resolve("00-after-login.png"));

            StgMenuReport report = new StgMenuReport();
            report.generatedAt = LocalDateTime.now().toString();
            report.environment = prod.baseUrl;
            report.navigationMethod = "PRD only. Login with the provided account, select FC Sandbox if required, then access pages by clicking sidebar group/submenu items in order. No direct page URLs were used after login.";
            report.visits = walkProdMenu(driver, prod, runDir.resolve("pages"));

            MAPPER.writeValue(runDir.resolve("prod-menu-click-report.json").toFile(), report);
            Files.writeString(runDir.resolve("summary.txt"), buildSummary(report), StandardCharsets.UTF_8);

            System.out.println("WMS_PROD_MENU_CLICK_REVIEW_DIR=" + runDir.toAbsolutePath());
            System.out.println("PROD_VISITED=" + report.visits.size());
            System.out.println("PROD_ERRORS=" + report.visits.stream().filter(visit -> visit.error != null).count());
        } finally {
            driver.quit();
        }
    }

    @Test
    public void debugStgConfigModalCards() throws Exception {
        Environment stg = new Environment(
                "stg",
                envOrDefault("STG_WMS_BASE_URL", "https://stg-wms.onflow.vn"),
                requiredEnv("STG_WMS_EMAIL"),
                requiredEnv("STG_WMS_PASSWORD"));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path runDir = Path.of("target", "wms-stg-config-debug", timestamp);
        Files.createDirectories(runDir);

        WebDriver driver = DriverFactory.create(envOrDefault("BROWSER", "chrome"));
        try {
            login(driver, stg);
            settle(driver);
            if (!openConfigModal(driver)) {
                throw new IllegalStateException("Không mở được modal Quản trị & cấu hình");
            }
            saveScreenshot(driver, runDir.resolve("config-modal.png"));
            Files.writeString(runDir.resolve("modal-candidates.json"), modalCandidatesJson(driver), StandardCharsets.UTF_8);
            System.out.println("WMS_STG_CONFIG_DEBUG_DIR=" + runDir.toAbsolutePath());
        } finally {
            driver.quit();
        }
    }

    @Test
    public void debugProdSidebarAfterLogin() throws Exception {
        Environment prod = new Environment(
                "prod",
                envOrDefault("PRD_WMS_BASE_URL", envOrDefault("PROD_WMS_BASE_URL", "https://wms.onflow.vn")),
                requiredAnyEnv("PRD_WMS_EMAIL", "PROD_WMS_EMAIL"),
                requiredAnyEnv("PRD_WMS_PASSWORD", "PROD_WMS_PASSWORD"));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path runDir = Path.of("target", "wms-prod-sidebar-debug", timestamp);
        Files.createDirectories(runDir);

        WebDriver driver = DriverFactory.create(envOrDefault("BROWSER", "chrome"));
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(25));
            login(driver, prod, true);
            settle(driver);
            saveScreenshot(driver, runDir.resolve("00-after-login.png"));
            Files.writeString(runDir.resolve("sidebar-candidates.json"), sidebarCandidatesJson(driver), StandardCharsets.UTF_8);
            Files.writeString(runDir.resolve("body-preview.txt"), bodyPreview(driver), StandardCharsets.UTF_8);
            System.out.println("WMS_PROD_SIDEBAR_DEBUG_DIR=" + runDir.toAbsolutePath());
        } finally {
            driver.quit();
        }
    }

    @Test
    public void debugProdSidebarGroups() throws Exception {
        Environment prod = new Environment(
                "prod",
                envOrDefault("PRD_WMS_BASE_URL", envOrDefault("PROD_WMS_BASE_URL", "https://wms.onflow.vn")),
                requiredAnyEnv("PRD_WMS_EMAIL", "PROD_WMS_EMAIL"),
                requiredAnyEnv("PRD_WMS_PASSWORD", "PROD_WMS_PASSWORD"));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path runDir = Path.of("target", "wms-prod-sidebar-groups-debug", timestamp);
        Files.createDirectories(runDir);

        WebDriver driver = DriverFactory.create(envOrDefault("BROWSER", "chrome"));
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(25));
            login(driver, prod, true);
            settle(driver);
            int topMenuCount = prodTopMenuCount(driver);
            for (int index = 0; index < topMenuCount; index++) {
                String label = clickProdTopMenuByIndex(driver, index);
                if (label == null || label.isBlank()) {
                    continue;
                }
                sleep(GROUP_TOGGLE_MS);
                String prefix = String.format("%02d-%s", index + 1, slug(label));
                saveScreenshot(driver, runDir.resolve(prefix + ".png"));
                Files.writeString(runDir.resolve(prefix + ".json"), sidebarCandidatesJson(driver), StandardCharsets.UTF_8);
                System.out.println("PROD_OPEN_GROUP=" + (index + 1) + " | " + label);
            }
            System.out.println("WMS_PROD_SIDEBAR_GROUPS_DEBUG_DIR=" + runDir.toAbsolutePath());
        } finally {
            driver.quit();
        }
    }

    private static List<MenuVisit> walkStgMenu(WebDriver driver, Environment env, Path pagesDir) throws IOException {
        Files.createDirectories(pagesDir);
        List<MenuVisit> visits = new ArrayList<>();
        int index = 1;

        for (GroupSpec group : STG_MENU) {
            if (group.children.isEmpty()) {
                visits.add(clickSidebarTarget(driver, env, group.name, group.name, null, pagesDir, index++));
                continue;
            }
            for (String child : group.children) {
                ensureGroupOpen(driver, group.name, child);
                visits.add(clickSidebarTarget(driver, env, group.name, child, child, pagesDir, index++));
            }
        }

        for (String card : CONFIG_CARDS) {
            int currentIndex = index++;
            if (!openConfigModal(driver)) {
                MenuVisit visit = newVisit("Quản trị & cấu hình", card, pagesDir, currentIndex);
                visit.error = "Không mở được modal Quản trị & cấu hình trước khi click card";
                trySaveScreenshot(driver, visit.screenshot);
                visits.add(visit);
                continue;
            }
            visits.add(clickConfigCard(driver, env, "Quản trị & cấu hình", card, pagesDir, currentIndex));
        }
        return visits;
    }

    private static List<MenuVisit> walkConfigModalOnly(WebDriver driver, Environment env, Path pagesDir) throws IOException {
        Files.createDirectories(pagesDir);
        List<MenuVisit> visits = new ArrayList<>();
        int index = 1;
        for (String card : CONFIG_CARDS) {
            if (!openConfigModal(driver)) {
                MenuVisit visit = newVisit("Quản trị & cấu hình", card, pagesDir, index++);
                visit.error = "Không mở được modal Quản trị & cấu hình trước khi click card";
                trySaveScreenshot(driver, visit.screenshot);
                visits.add(visit);
                continue;
            }
            visits.add(clickConfigCard(driver, env, "Quản trị & cấu hình", card, pagesDir, index++));
        }
        return visits;
    }

    private static List<MenuVisit> walkProdMenu(WebDriver driver, Environment env, Path pagesDir) throws IOException {
        Files.createDirectories(pagesDir);
        List<MenuVisit> visits = new ArrayList<>();
        int index = 1;

        for (ProdGroupSpec group : PROD_MENU) {
            if (group.children.isEmpty()) {
                visits.add(clickProdTopTarget(driver, env, group, pagesDir, index++));
                continue;
            }
            for (String child : group.children) {
                ensureProdGroupOpen(driver, group, child);
                visits.add(clickProdChildTarget(driver, env, group, child, pagesDir, index++));
            }
        }
        return visits;
    }

    private static void ensureProdGroupOpen(WebDriver driver, ProdGroupSpec group, String child) {
        closeBlockingModalIfAny(driver);
        if (isProdChildVisible(driver, group, child)) {
            return;
        }
        System.out.println("OPEN_PROD_GROUP=" + group.displayName);
        clickProdTopMenu(driver, group);
        sleep(GROUP_TOGGLE_MS);
        if (!isProdChildVisible(driver, group, child)) {
            clickProdTopMenuByIndex(driver, group.topIndex);
            sleep(GROUP_TOGGLE_MS);
        }
        if (!isProdChildVisible(driver, group, child)) {
            throw new IllegalStateException("Group PRD đã click nhưng submenu chưa hiển thị: "
                    + group.displayName + " / " + child);
        }
    }

    private static MenuVisit clickProdTopTarget(
            WebDriver driver,
            Environment env,
            ProdGroupSpec group,
            Path pagesDir,
            int index) {
        MenuVisit visit = newVisit(group.displayName, group.menuLabel, pagesDir, index);
        try {
            closeBlockingModalIfAny(driver);
            System.out.println("CLICK_PROD_MENU=" + index + " | " + group.displayName + " / " + group.menuLabel);
            String clicked = clickProdTopMenu(driver, group);
            if (clicked == null || clicked.isBlank()) {
                visit.error = "Không tìm thấy hoặc không click được group/menu PRD trên sidebar";
            } else {
                captureAfterClick(driver, env, visit);
            }
            saveScreenshot(driver, Path.of(visit.screenshot));
        } catch (RuntimeException | IOException e) {
            visit.error = e.getClass().getSimpleName() + ": " + e.getMessage();
            trySaveScreenshot(driver, visit.screenshot);
        }
        return visit;
    }

    private static MenuVisit clickProdChildTarget(
            WebDriver driver,
            Environment env,
            ProdGroupSpec group,
            String child,
            Path pagesDir,
            int index) {
        MenuVisit visit = newVisit(group.displayName, child, pagesDir, index);
        try {
            closeBlockingModalIfAny(driver);
            System.out.println("CLICK_PROD_MENU=" + index + " | " + group.displayName + " / " + child);
            if (!clickProdChild(driver, group, child)) {
                visit.error = "Không tìm thấy hoặc không click được submenu PRD trên sidebar";
            } else {
                captureAfterClick(driver, env, visit);
            }
            saveScreenshot(driver, Path.of(visit.screenshot));
        } catch (RuntimeException | IOException e) {
            visit.error = e.getClass().getSimpleName() + ": " + e.getMessage();
            trySaveScreenshot(driver, visit.screenshot);
        }
        return visit;
    }

    private static void ensureGroupOpen(WebDriver driver, String groupName, String firstChild) {
        closeBlockingModalIfAny(driver);
        if (isSidebarChildVisible(driver, groupName, firstChild)) {
            return;
        }
        System.out.println("OPEN_GROUP=" + groupName);
        if (!clickSidebarGroup(driver, groupName)) {
            throw new IllegalStateException("Không tìm thấy group menu trên sidebar: " + groupName);
        }
        sleep(GROUP_TOGGLE_MS);
        if (!isSidebarChildVisible(driver, groupName, firstChild)) {
            clickSidebarGroup(driver, groupName);
            sleep(GROUP_TOGGLE_MS);
        }
        if (!isSidebarChildVisible(driver, groupName, firstChild)) {
            throw new IllegalStateException("Group đã click nhưng submenu chưa hiển thị: " + groupName + " / " + firstChild);
        }
    }

    private static MenuVisit clickSidebarTarget(
            WebDriver driver,
            Environment env,
            String group,
            String label,
            String childLabel,
            Path pagesDir,
            int index) {
        MenuVisit visit = newVisit(group, label, pagesDir, index);
        try {
            closeBlockingModalIfAny(driver);
            System.out.println("CLICK_MENU=" + index + " | " + group + " / " + label);
            boolean clicked = childLabel == null
                    ? clickSidebarGroup(driver, label)
                    : clickSidebarChild(driver, group, childLabel);
            if (!clicked) {
                visit.error = "Không tìm thấy hoặc không click được menu item trên sidebar";
            } else {
                captureAfterClick(driver, env, visit);
            }
            saveScreenshot(driver, Path.of(visit.screenshot));
        } catch (RuntimeException | IOException e) {
            visit.error = e.getClass().getSimpleName() + ": " + e.getMessage();
            trySaveScreenshot(driver, visit.screenshot);
        }
        return visit;
    }

    private static boolean openConfigModal(WebDriver driver) {
        closeBlockingModalIfAny(driver);
        System.out.println("OPEN_CONFIG_MODAL=Quản trị & cấu hình");
        scrollSidebarToBottom(driver);
        sleep(TINY_SETTLE_MS);
        if (!clickSidebarGroup(driver, "Quản trị & cấu hình") && !clickBottomConfigButton(driver)) {
            return false;
        }
        sleep(MODAL_OPEN_MS);
        boolean opened = Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "return !!Array.from(document.querySelectorAll('[role=\"dialog\"], .ant-modal, [class*=\"modal\"]'))"
                        + ".find(el => (el.innerText || '').includes('Quản trị & cấu hình'));"));
        if (!opened) {
            return false;
        }
        return true;
    }

    private static void scrollSidebarToBottom(WebDriver driver) {
        ((JavascriptExecutor) driver).executeScript(
                "for (const el of Array.from(document.querySelectorAll('*'))) {"
                        + "  const r = el.getBoundingClientRect();"
                        + "  if (r.left >= 0 && r.left < 320 && r.height > 300 && el.scrollHeight > el.clientHeight) { el.scrollTop = el.scrollHeight; }"
                        + "}");
    }

    private static boolean clickBottomConfigButton(WebDriver driver) {
        Boolean clickedByText = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "const norm = s => (s || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').replace(/\\s+/g,' ').trim().toLowerCase();"
                        + "const text = el => (el.innerText || el.textContent || '').replace(/\\s+/g,' ').trim();"
                        + "const candidates = Array.from(document.querySelectorAll('a,button,[role=\"button\"],div,span')).filter(el => {"
                        + "  const r = el.getBoundingClientRect();"
                        + "  return r.width > 0 && r.height > 0 && r.left >= 0 && r.left < 305 && r.top > 250 && norm(text(el)).includes('quan tri') && norm(text(el)).includes('cau hinh');"
                        + "}).sort((a,b) => text(a).length - text(b).length || b.getBoundingClientRect().top - a.getBoundingClientRect().top);"
                        + "const el = candidates[0];"
                        + "if (!el) return false;"
                        + "const r = el.getBoundingClientRect();"
                        + "const x = Math.min(275, Math.max(40, r.right - 20));"
                        + "const y = r.top + r.height / 2;"
                        + "const target = document.elementFromPoint(x, y) || el;"
                        + "for (const type of ['pointerdown','mousedown','pointerup','mouseup','click']) target.dispatchEvent(new MouseEvent(type, {bubbles:true, cancelable:true, clientX:x, clientY:y, view:window}));"
                        + "return true;");
        if (Boolean.TRUE.equals(clickedByText)) {
            return true;
        }
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "const x = 230;"
                        + "const y = window.innerHeight - 42;"
                        + "const target = document.elementFromPoint(x, y);"
                        + "if (!target) return false;"
                        + "for (const type of ['pointerdown','mousedown','pointerup','mouseup','click']) target.dispatchEvent(new MouseEvent(type, {bubbles:true, cancelable:true, clientX:x, clientY:y, view:window}));"
                        + "return true;"));
    }

    private static MenuVisit clickConfigCard(WebDriver driver, Environment env, String group, String card, Path pagesDir, int index) {
        MenuVisit visit = newVisit(group, card, pagesDir, index);
        try {
            System.out.println("CLICK_CONFIG_CARD=" + index + " | " + card);
            if (!clickModalCard(driver, card)) {
                visit.error = "Không tìm thấy hoặc không click được card trong modal Quản trị & cấu hình";
            } else {
                captureAfterClick(driver, env, visit);
            }
            saveScreenshot(driver, Path.of(visit.screenshot));
        } catch (RuntimeException | IOException e) {
            visit.error = e.getClass().getSimpleName() + ": " + e.getMessage();
            trySaveScreenshot(driver, visit.screenshot);
        }
        return visit;
    }

    private static MenuVisit newVisit(String group, String label, Path pagesDir, int index) {
        MenuVisit visit = new MenuVisit();
        visit.expectedOrder = index;
        visit.group = group;
        visit.label = label;
        visit.screenshot = pagesDir.resolve(String.format("%02d-%s-%s.png", index, slug(group), slug(label))).toString();
        return visit;
    }

    private static void captureAfterClick(WebDriver driver, Environment env, MenuVisit visit) {
        waitForReadyStateSoft(driver);
        sleep(MENU_SETTLE_MS);
        settle(driver);
        visit.finalUrl = driver.getCurrentUrl();
        visit.path = toPath(env.baseUrl, visit.finalUrl);
        visit.heading = firstVisibleText(driver, "h1, h2, h3, .ant-page-header-heading-title, [class*='title'], [class*='Title']");
        visit.bodyPreview = bodyPreview(driver);
        visit.loginRedirected = String.valueOf(visit.finalUrl).contains("/login");
        visit.notFoundLike = looksLikeNotFound(visit.bodyPreview);
        visit.modalStillOpen = isAnyModalOpen(driver);
        closeBlockingModalIfAny(driver);
    }

    private static boolean isAnyModalOpen(WebDriver driver) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "const visible = el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; };"
                        + "return Array.from(document.querySelectorAll('[role=\"dialog\"], .ant-modal, [class*=\"modal\"], [class*=\"drawer\"]')).some(visible);"));
    }

    private static void closeBlockingModalIfAny(WebDriver driver) {
        for (int attempt = 0; attempt < 3; attempt++) {
            Boolean closed = (Boolean) ((JavascriptExecutor) driver).executeScript(
                    "const visible = el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; };"
                            + "const modals = Array.from(document.querySelectorAll('[role=\"dialog\"], .ant-modal, [class*=\"modal\"], [class*=\"drawer\"]')).filter(visible);"
                            + "if (!modals.length) return false;"
                            + "const top = modals[modals.length - 1];"
                            + "const tr = top.getBoundingClientRect();"
                            + "const closeSelectors = '.ant-modal-close, [aria-label=\"Close\"], [aria-label=\"close\"], .btn-close, button[class*=\"close\"], [class*=\"close\"], [data-icon=\"close\"]';"
                            + "let close = top.querySelector(closeSelectors);"
                            + "if (!close) {"
                            + "  close = Array.from(top.querySelectorAll('button,[role=\"button\"],svg,i,span,div')).filter(visible).find(el => {"
                            + "    const r = el.getBoundingClientRect();"
                            + "    const t = (el.innerText || el.textContent || '').trim();"
                            + "    return r.left > tr.right - 90 && r.top < tr.top + 80 && (t === '×' || t === 'x' || t === 'X' || /close|times|x/i.test(el.className || '') || /close/i.test(el.getAttribute('aria-label') || ''));"
                            + "  });"
                            + "}"
                            + "const fire = (el, x, y) => {"
                            + "  for (const type of ['pointerdown','mousedown','pointerup','mouseup','click']) el.dispatchEvent(new MouseEvent(type, {bubbles:true, cancelable:true, clientX:x, clientY:y, view:window}));"
                            + "};"
                            + "if (close) { const r = close.getBoundingClientRect(); fire(close, r.left + r.width / 2, r.top + r.height / 2); return true; }"
                            + "const x = tr.right - 22;"
                            + "const y = tr.top + 28;"
                            + "const target = document.elementFromPoint(x, y);"
                            + "if (target) { fire(target, x, y); return true; }"
                            + "return false;");
            if (Boolean.TRUE.equals(closed)) {
                sleep(TINY_SETTLE_MS);
                continue;
            }
            try {
                driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
                sleep(TINY_SETTLE_MS);
            } catch (RuntimeException ignored) {
            }
            boolean stillOpen = Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                    "const visible = el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; };"
                            + "return Array.from(document.querySelectorAll('[role=\"dialog\"], .ant-modal, [class*=\"modal\"], [class*=\"drawer\"]')).some(visible);"));
            if (!stillOpen) {
                return;
            }
        }
    }

    private static boolean clickSidebarGroup(WebDriver driver, String label) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(sidebarScript("clickGroup"), label, groupNames()));
    }

    private static boolean clickSidebarChild(WebDriver driver, String group, String child) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(sidebarScript("clickChild"), group, child, groupNames()));
    }

    private static boolean isSidebarChildVisible(WebDriver driver, String group, String child) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(sidebarScript("hasChild"), group, child, groupNames()));
    }

    private static boolean clickModalCard(WebDriver driver, String label) {
        Boolean clicked = (Boolean) ((JavascriptExecutor) driver).executeScript(""
                + "const label = arguments[0];"
                + "const norm = s => (s || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').replace(/\\s+/g,' ').trim().toLowerCase();"
                + "const text = el => (el.innerText || el.textContent || '').replace(/\\s+/g,' ').trim();"
                + "const visible = el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; };"
                + "const adminModalRoot = el => {"
                + "  let node = el;"
                + "  for (let depth = 0; depth < 10 && node; depth++, node = node.parentElement) {"
                + "    if (norm(text(node)).includes('quan tri') && norm(text(node)).includes('cau hinh') && node.querySelector('.administration-app-launcher__app')) return node;"
                + "  }"
                + "  return null;"
                + "};"
                + "const apps = Array.from(document.querySelectorAll('.administration-app-launcher__app'))"
                + "  .filter(el => visible(el) && adminModalRoot(el) && norm(text(el)) === norm(label));"
                + "const app = apps.sort((a,b) => a.getBoundingClientRect().top - b.getBoundingClientRect().top || a.getBoundingClientRect().left - b.getBoundingClientRect().left)[0];"
                + "if (!app) return false;"
                + "app.scrollIntoView({block:'center', inline:'center'});"
                + "const r = app.getBoundingClientRect();"
                + "const x = Math.round(r.left + r.width / 2);"
                + "const y = Math.round(r.top + r.height / 2);"
                + "const target = document.elementFromPoint(x, y);"
                + "const clickTarget = target && app.contains(target) ? target : app;"
                + "for (const type of ['pointerdown','mousedown','pointerup','mouseup','click']) {"
                + "  clickTarget.dispatchEvent(new MouseEvent(type, {bubbles:true, cancelable:true, clientX:x, clientY:y, view:window}));"
                + "}"
                + "return true;",
                label);
        sleep(TINY_SETTLE_MS);
        return Boolean.TRUE.equals(clicked);
    }

    private static String modalCandidatesJson(WebDriver driver) {
        return Objects.toString(((JavascriptExecutor) driver).executeScript(
                "const text = el => (el.innerText || el.textContent || '').replace(/\\s+/g,' ').trim();"
                        + "const visible = el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; };"
                        + "const modal = Array.from(document.querySelectorAll('[role=\"dialog\"], .ant-modal, [class*=\"modal\"]')).filter(visible).find(el => text(el).includes('Quản trị & cấu hình'));"
                        + "if (!modal) return '[]';"
                        + "const rows = Array.from(modal.querySelectorAll('a,button,[role=\"button\"],div,span')).filter(visible).map((el, i) => {"
                        + "  const r = el.getBoundingClientRect();"
                        + "  return {i, tag: el.tagName, cls: el.className || '', text: text(el), left: Math.round(r.left), top: Math.round(r.top), width: Math.round(r.width), height: Math.round(r.height)};"
                        + "}).filter(x => x.text && x.text.length <= 120);"
                        + "return JSON.stringify(rows, null, 2);"),
                "[]");
    }

    private static String sidebarCandidatesJson(WebDriver driver) {
        return Objects.toString(((JavascriptExecutor) driver).executeScript(
                "const text = el => (el.innerText || el.textContent || '').replace(/\\s+/g,' ').trim();"
                        + "const visible = el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; };"
                        + "const rows = Array.from(document.querySelectorAll('a,button,[role=\"button\"],li,div,span')).filter(visible).map((el, i) => {"
                        + "  const r = el.getBoundingClientRect();"
                        + "  const href = el.href || el.getAttribute('href') || '';"
                        + "  return {i, tag: el.tagName, cls: el.className || '', role: el.getAttribute('role') || '', href, text: text(el), left: Math.round(r.left), top: Math.round(r.top), width: Math.round(r.width), height: Math.round(r.height)};"
                        + "}).filter(x => x.left >= 0 && x.left < 360 && x.text && x.text.length <= 180);"
                        + "return JSON.stringify(rows, null, 2);"),
                "[]");
    }

    private static int prodTopMenuCount(WebDriver driver) {
        Number count = (Number) ((JavascriptExecutor) driver).executeScript(prodTopMenuScript()
                + "return topMenus().length;");
        return count == null ? 0 : count.intValue();
    }

    private static String clickProdTopMenuByIndex(WebDriver driver, int index) {
        Object value = ((JavascriptExecutor) driver).executeScript(prodTopMenuScript()
                + "const el = topMenus()[arguments[0]];"
                + "if (!el) return null;"
                + "const label = text(el);"
                + "fire(el, 'group');"
                + "return label;",
                index);
        return Objects.toString(value, "");
    }

    private static String clickProdTopMenu(WebDriver driver, ProdGroupSpec group) {
        Object value = ((JavascriptExecutor) driver).executeScript(prodTopMenuScript()
                + "const norm = s => (s || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').replace(/\\s+/g,' ').trim().toLowerCase();"
                + "const label = arguments[0];"
                + "const occurrence = arguments[1];"
                + "const el = topMenus().filter(item => norm(text(item)) === norm(label))[occurrence];"
                + "if (!el) return null;"
                + "const foundLabel = text(el);"
                + "fire(el, 'group');"
                + "return foundLabel;",
                group.menuLabel,
                prodTopMenuOccurrence(group));
        return Objects.toString(value, "");
    }

    private static boolean isProdChildVisible(WebDriver driver, ProdGroupSpec group, String child) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                prodChildMenuScript("hasChild"),
                group.menuLabel,
                prodTopMenuOccurrence(group),
                child));
    }

    private static boolean clickProdChild(WebDriver driver, ProdGroupSpec group, String child) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                prodChildMenuScript("clickChild"),
                group.menuLabel,
                prodTopMenuOccurrence(group),
                child));
    }

    private static int prodTopMenuOccurrence(ProdGroupSpec group) {
        int occurrence = 0;
        for (ProdGroupSpec spec : PROD_MENU) {
            if (Objects.equals(normalizeText(spec.menuLabel), normalizeText(group.menuLabel))) {
                if (spec.topIndex == group.topIndex) {
                    return occurrence;
                }
                occurrence++;
            }
        }
        return occurrence;
    }

    private static String prodChildMenuScript(String action) {
        return prodTopMenuScript()
                + "const action = '" + action + "';"
                + "const norm = s => (s || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').replace(/\\s+/g,' ').trim().toLowerCase();"
                + "const topByLabel = (label, occurrence) => topMenus().filter(el => norm(text(el)) === norm(label))[occurrence] || null;"
                + "const childLinks = (label, occurrence) => {"
                + "  const tops = topMenus();"
                + "  const group = topByLabel(label, occurrence);"
                + "  if (!group) return [];"
                + "  const topIndex = tops.indexOf(group);"
                + "  const gTop = group.getBoundingClientRect().top;"
                + "  const next = tops[topIndex + 1];"
                + "  const nextTop = next ? next.getBoundingClientRect().top : Infinity;"
                + "  return Array.from(document.querySelectorAll('.sidebar-scrollable a')).filter(el => {"
                + "    if (!visible(el) || tops.includes(el) || !text(el)) return false;"
                + "    const r = el.getBoundingClientRect();"
                + "    return r.left >= 20 && r.left < 260 && r.top > gTop && r.top < nextTop && !text(el).includes('@') && text(el) !== 'Đăng xuất';"
                + "  }).sort((a,b) => a.getBoundingClientRect().top - b.getBoundingClientRect().top);"
                + "};"
                + "const findChild = (menuLabel, occurrence, childLabel) => childLinks(menuLabel, occurrence).find(el => norm(text(el)) === norm(childLabel)) || null;"
                + "if (action === 'hasChild') return !!findChild(arguments[0], arguments[1], arguments[2]);"
                + "if (action === 'clickChild') { const el = findChild(arguments[0], arguments[1], arguments[2]); if (!el) return false; fire(el, 'child'); return true; }"
                + "return false;";
    }

    private static String prodTopMenuScript() {
        return ""
                + "const text = el => (el.innerText || el.textContent || '').replace(/\\s+/g,' ').trim();"
                + "const visible = el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; };"
                + "const topMenus = () => Array.from(document.querySelectorAll('.sidebar-scrollable li.nav-item > a.nav-link.menu-link, .sidebar-scrollable a.nav-link.menu-link'))"
                + "  .filter(el => visible(el) && text(el) && el.getBoundingClientRect().left >= 0 && el.getBoundingClientRect().left < 260)"
                + "  .filter(el => !text(el).includes('@') && text(el) !== 'Đăng xuất')"
                + "  .sort((a,b) => a.getBoundingClientRect().top - b.getBoundingClientRect().top);"
                + "const fire = (el, mode) => {"
                + "  el.scrollIntoView({block:'center', inline:'nearest'});"
                + "  const r = el.getBoundingClientRect();"
                + "  const x = mode === 'group' ? Math.min(225, Math.max(40, r.right - 20)) : Math.min(Math.max(r.left + r.width / 2, 45), 225);"
                + "  const y = r.top + r.height / 2;"
                + "  for (const type of ['pointerdown','mousedown','pointerup','mouseup','click']) el.dispatchEvent(new MouseEvent(type, {bubbles:true, cancelable:true, clientX:x, clientY:y, view:window}));"
                + "  if (typeof el.click === 'function') el.click();"
                + "};";
    }

    private static List<String> groupNames() {
        List<String> names = new ArrayList<>();
        for (GroupSpec spec : STG_MENU) {
            names.add(spec.name);
        }
        names.add("Quản trị & cấu hình");
        return names;
    }

    private static String sidebarScript(String action) {
        return ""
                + "const action = '" + action + "';"
                + "const norm = s => (s || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').replace(/\\s+/g,' ').trim().toLowerCase();"
                + "const text = el => (el.innerText || el.textContent || '').replace(/\\s+/g,' ').trim();"
                + "const labelText = el => norm(text(el).replace(/^[•·◦\\-]+\\s*/,''));"
                + "const matches = (el, label) => labelText(el) === norm(label) || (labelText(el).endsWith(norm(label)) && labelText(el).length <= norm(label).length + 4);"
                + "const visible = el => { const r = el.getBoundingClientRect(); return !!text(el) && r.width > 0 && r.height > 0 && r.left >= 0 && r.left < 305; };"
                + "const all = () => Array.from(document.querySelectorAll('a,button,[role=\"button\"],li,div,span')).filter(visible);"
                + "const rawExact = label => all().filter(el => matches(el,label)).sort((a,b) => text(a).length - text(b).length || a.getBoundingClientRect().top - b.getBoundingClientRect().top)[0] || null;"
                + "const fire = (el, mode) => {"
                + "  if (!el) return false;"
                + "  el.scrollIntoView({block:'center', inline:'nearest'});"
                + "  const r = el.getBoundingClientRect();"
                + "  const x = mode === 'group' ? Math.min(275, Math.max(40, r.right - 20)) : Math.min(Math.max(r.left + r.width / 2, 45), 275);"
                + "  const y = r.top + r.height / 2;"
                + "  const target = document.elementFromPoint(x, y) || el;"
                + "  for (const type of ['pointerdown','mousedown','pointerup','mouseup','click']) target.dispatchEvent(new MouseEvent(type, {bubbles:true, cancelable:true, clientX:x, clientY:y, view:window}));"
                + "  return true;"
                + "};"
                + "const findChild = (group, child, groups) => {"
                + "  const g = rawExact(group); if (!g) return null;"
                + "  const gTop = g.getBoundingClientRect().top;"
                + "  let nextTop = Infinity;"
                + "  for (const name of groups) { const other = rawExact(name); if (!other || norm(name) === norm(group)) continue; const t = other.getBoundingClientRect().top; if (t > gTop && t < nextTop) nextTop = t; }"
                + "  const found = all().filter(el => matches(el, child)).filter(el => { const t = el.getBoundingClientRect().top; return t > gTop && t < nextTop; });"
                + "  return found.sort((a,b) => text(a).length - text(b).length || a.getBoundingClientRect().top - b.getBoundingClientRect().top)[0] || null;"
                + "};"
                + "if (action === 'clickGroup') { return fire(rawExact(arguments[0]), 'group'); }"
                + "if (action === 'clickChild') { return fire(findChild(arguments[0], arguments[1], arguments[2]), 'child'); }"
                + "if (action === 'hasChild') { return !!findChild(arguments[0], arguments[1], arguments[2]); }"
                + "return false;";
    }

    private static String sidebarScript(String action, Object ignored) {
        return sidebarScript(action);
    }

    private static String sidebarScriptAction(String action) {
        return sidebarScript(action);
    }

    @SuppressWarnings("unchecked")
    private static CardPoint findModalCardPoint(WebDriver driver, String label) {
        List<Number> value = (List<Number>) ((JavascriptExecutor) driver).executeScript(""
                + "const label = arguments[0];"
                + "const norm = s => (s || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').replace(/\\s+/g,' ').trim().toLowerCase();"
                + "const text = el => (el.innerText || el.textContent || '').replace(/\\s+/g,' ').trim();"
                + "const visible = el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; };"
                + "const modal = Array.from(document.querySelectorAll('[role=\"dialog\"], .ant-modal, [class*=\"modal\"]')).filter(visible).find(el => text(el).includes('Quản trị & cấu hình'));"
                + "if (!modal) return null;"
                + "const mr = modal.getBoundingClientRect();"
                + "const insideModal = el => { const r = el.getBoundingClientRect(); return r.left >= mr.left && r.right <= mr.right && r.top >= mr.top && r.bottom <= mr.bottom; };"
                + "const candidates = Array.from(modal.querySelectorAll('a,button,[role=\"button\"],div,span'))"
                + "  .filter(el => visible(el) && insideModal(el) && norm(text(el)) === norm(label));"
                + "const labels = candidates.sort((a,b) => text(a).length - text(b).length);"
                + "for (const labelEl of labels) {"
                + "  let node = labelEl;"
                + "  for (let depth = 0; depth < 10 && node && node !== modal; depth++, node = node.parentElement) {"
                + "    const r = node.getBoundingClientRect();"
                + "    if (!insideModal(node)) continue;"
                + "    const looksLikeCard = r.width >= 160 && r.width <= 460 && r.height >= 45 && r.height <= 150;"
                + "    const containsLabel = norm(text(node)).includes(norm(label));"
                + "    if (!looksLikeCard || !containsLabel) continue;"
                + "    const x = r.left + r.width / 2;"
                + "    const y = r.top + r.height / 2;"
                + "    return [Math.round(x), Math.round(y)];"
                + "  }"
                + "}"
                + "return null;",
                label);
        if (value == null || value.size() < 2) {
            return null;
        }
        return new CardPoint(value.get(0).intValue(), value.get(1).intValue());
    }

    private static void login(WebDriver driver, Environment env) {
        login(driver, env, true);
    }

    private static void login(WebDriver driver, Environment env, boolean selectFcAfterLogin) {
        driver.manage().window().maximize();
        driver.get(joinUrl(env.baseUrl, "/login"));
        waitForReadyState(driver);
        sleep(350);

        for (int attempt = 1; attempt <= 3; attempt++) {
            clickButtonContaining(driver, "Tiếp tục đăng nhập");
            if (selectFcAfterLogin) {
                selectFcOnlyIfRequired(driver, env);
            }
            if (loggedIn(driver)) {
                logSelectedFc(driver);
                return;
            }

            WebElement email = firstEmailInput(driver);
            WebElement password = firstPasswordInput(driver);
            if (email != null && password != null) {
                System.out.println("LOGIN_SUBMIT=" + env.email);
                setInputValue(driver, email, env.email);
                setInputValue(driver, password, env.password);
                clickSubmit(driver);
                waitForLoginTransition(driver);
                clickButtonContaining(driver, "Tiếp tục đăng nhập");
                if (selectFcAfterLogin) {
                    selectFcOnlyIfRequired(driver, env);
                }
                if (waitUntilLoggedIn(driver)) {
                    logSelectedFc(driver);
                    return;
                }
            }
            sleep(1000);
        }
        throw new IllegalStateException("Unable to login. Current URL: " + driver.getCurrentUrl());
    }

    private static boolean waitUntilLoggedIn(WebDriver driver) {
        try {
            new WebDriverWait(driver, LOGIN_STEP_WAIT).until((ExpectedCondition<Boolean>) WmsCurrentUiInspectorTest::loggedIn);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean loggedIn(WebDriver driver) {
        String url = String.valueOf(driver.getCurrentUrl());
        String body = normalizeText(bodyText(driver));
        return !url.contains("/login") && !body.contains("vui long chon fulfillment center");
    }

    private static void waitForLoginTransition(WebDriver driver) {
        try {
            new WebDriverWait(driver, LOGIN_STEP_WAIT).until((ExpectedCondition<Boolean>) d -> {
                String body = normalizeText(bodyText(d));
                return loggedIn(d)
                        || body.contains("vui long chon fulfillment center")
                        || body.contains("tiep tuc dang nhap");
            });
        } catch (RuntimeException ignored) {
            sleep(500);
        }
    }

    private static void selectFcOnlyIfRequired(WebDriver driver, Environment env) {
        if (!normalizeText(bodyText(driver)).contains("vui long chon fulfillment center")) {
            return;
        }
        String fcName = fcNameFor(env);
        System.out.println("SELECT_FC=" + fcName);
        WebElement fc = exactTextElement(driver, fcName, SHORT_WAIT);
        if (fc != null) {
            clickUi(driver, fc);
            sleep(TINY_SETTLE_MS);
            clickButtonContaining(driver, "Bạn đã chọn");
            clickButtonContaining(driver, "Vui lòng chọn fulfillment center");
            clickButtonContaining(driver, "Xác nhận");
            sleep(GROUP_TOGGLE_MS);
        } else {
            throw new IllegalStateException("Không tìm thấy FC để chọn: " + fcName);
        }
    }

    private static String fcNameFor(Environment env) {
        String defaultFc = "prod".equalsIgnoreCase(env.name) ? "FC Sandbox" : "FC HN";
        String prefix = env.name.toUpperCase(Locale.ROOT);
        String preferredKey = prefix + "_WMS_FC_NAME";
        String fallbackKey = "prod".equalsIgnoreCase(env.name) ? "PROD_WMS_FC_NAME" : "DEFAULT_FC_NAME";
        return envOrDefault(preferredKey, envOrDefault(fallbackKey, envOrDefault("DEFAULT_FC_NAME", defaultFc)));
    }

    private static void logSelectedFc(WebDriver driver) {
        String body = normalizeText(bodyText(driver));
        if (body.contains("fc hn")) {
            System.out.println("FC_ALREADY_SELECTED=FC HN");
        }
    }

    private static WebElement firstEmailInput(WebDriver driver) {
        for (WebElement input : visibleInputs(driver)) {
            String type = attr(input, "type").toLowerCase(Locale.ROOT);
            String text = (attr(input, "name") + " " + attr(input, "placeholder")).toLowerCase(Locale.ROOT);
            if ("email".equals(type) || text.contains("email") || text.contains("mail")) {
                return input;
            }
        }
        return null;
    }

    private static WebElement firstPasswordInput(WebDriver driver) {
        for (WebElement input : visibleInputs(driver)) {
            if ("password".equalsIgnoreCase(attr(input, "type"))) {
                return input;
            }
        }
        return null;
    }

    private static List<WebElement> visibleInputs(WebDriver driver) {
        List<WebElement> inputs = new ArrayList<>();
        for (WebElement input : driver.findElements(By.cssSelector("input"))) {
            if (displayed(input) && input.isEnabled()) {
                inputs.add(input);
            }
        }
        return inputs;
    }

    private static void setInputValue(WebDriver driver, WebElement input, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});"
                        + "arguments[0].focus();"
                        + "const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;"
                        + "setter.call(arguments[0], arguments[1]);"
                        + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",
                input,
                value);
    }

    private static void clickSubmit(WebDriver driver) {
        WebElement button = firstVisible(driver, By.cssSelector("button[type='submit']"), SHORT_WAIT);
        if (button == null) {
            button = firstVisible(driver, By.xpath("//button[contains(normalize-space(.),'Đăng nhập')]"), SHORT_WAIT);
        }
        if (button == null) {
            throw new IllegalStateException("Submit button was not found");
        }
        clickUi(driver, button);
    }

    private static boolean clickButtonContaining(WebDriver driver, String text) {
        WebElement button = firstVisible(driver, By.xpath(
                "//button[contains(normalize-space(.)," + xpathLiteral(text) + ")]"
                        + " | //*[@role='button' and contains(normalize-space(.)," + xpathLiteral(text) + ")]"),
                Duration.ofMillis(1500));
        if (button == null || !button.isEnabled()) {
            return false;
        }
        clickUi(driver, button);
        return true;
    }

    private static WebElement firstVisible(WebDriver driver, By locator, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout).until(d -> {
                for (WebElement element : d.findElements(locator)) {
                    if (displayed(element)) {
                        return element;
                    }
                }
                return null;
            });
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static WebElement exactTextElement(WebDriver driver, String text, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout).until(d -> {
                WebElement best = null;
                for (WebElement element : d.findElements(By.xpath(
                        "//*[normalize-space(.)=" + xpathLiteral(text) + " and not(self::script) and not(self::style)]"))) {
                    if (!displayed(element)) {
                        continue;
                    }
                    if (best == null || element.getText().length() < best.getText().length()) {
                        best = element;
                    }
                }
                return best;
            });
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void waitForReadyState(WebDriver driver) {
        new WebDriverWait(driver, LONG_WAIT).until((ExpectedCondition<Boolean>) d ->
                "complete".equals(String.valueOf(((JavascriptExecutor) d).executeScript("return document.readyState"))));
    }

    private static void waitForReadyStateSoft(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).until((ExpectedCondition<Boolean>) d ->
                    "complete".equals(String.valueOf(((JavascriptExecutor) d).executeScript("return document.readyState"))));
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean displayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String attr(WebElement element, String name) {
        try {
            String value = element.getAttribute(name);
            return value == null ? "" : value;
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static void clickUi(WebDriver driver, WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "const target = arguments[0].closest('a,button,[role=\"button\"],[class*=\"menu\"],[class*=\"item\"]') || arguments[0];"
                            + "target.scrollIntoView({block:'center', inline:'center'}); target.click();",
                    element);
        } catch (RuntimeException e) {
            element.click();
        }
    }

    private static void saveScreenshot(WebDriver driver, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.write(target, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }

    private static void trySaveScreenshot(WebDriver driver, String screenshot) {
        try {
            saveScreenshot(driver, Path.of(screenshot));
        } catch (RuntimeException | IOException ignored) {
        }
    }

    private static String firstVisibleText(WebDriver driver, String selector) {
        Object value = ((JavascriptExecutor) driver).executeScript(
                "for (const el of Array.from(document.querySelectorAll(arguments[0]))) {"
                        + "  const r = el.getBoundingClientRect();"
                        + "  const text = (el.innerText || el.textContent || '').replace(/\\s+/g, ' ').trim();"
                        + "  if (text && (r.width || r.height)) return text;"
                        + "}"
                        + "return '';",
                selector);
        return Objects.toString(value, "");
    }

    private static String bodyPreview(WebDriver driver) {
        String text = bodyText(driver).replaceAll("\\s+", " ").trim();
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }

    private static String bodyText(WebDriver driver) {
        try {
            return Objects.toString(((JavascriptExecutor) driver).executeScript("return document.body ? document.body.innerText : ''"), "");
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static boolean looksLikeNotFound(String body) {
        String normalized = normalizeText(body);
        return normalized.contains("404") || normalized.contains("not found") || normalized.contains("khong tim thay");
    }

    private static String toPath(String baseUrl, String url) {
        try {
            URI base = URI.create(baseUrl);
            URI uri = URI.create(url);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(base.getHost())) {
                return url;
            }
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                path += "?" + uri.getQuery();
            }
            return path;
        } catch (RuntimeException e) {
            return url;
        }
    }

    private static String buildSummary(StgMenuReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("Generated at: ").append(report.generatedAt).append(System.lineSeparator());
        builder.append("Environment: ").append(report.environment).append(System.lineSeparator());
        builder.append("Navigation: ").append(report.navigationMethod).append(System.lineSeparator());
        builder.append("Visited: ").append(report.visits.size()).append(System.lineSeparator()).append(System.lineSeparator());
        for (MenuVisit visit : report.visits) {
            builder.append(visit.expectedOrder).append(". ")
                    .append(visit.group).append(" / ").append(visit.label)
                    .append(" -> ").append(visit.path == null ? "" : visit.path);
            if (visit.error != null) {
                builder.append(" | ERROR: ").append(visit.error);
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static String joinUrl(String baseUrl, String path) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }

    private static String xpathLiteral(String value) {
        if (value == null) {
            return "''";
        }
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        StringBuilder builder = new StringBuilder("concat(");
        for (int index = 0; index < value.length(); index++) {
            if (index > 0) {
                builder.append(",");
            }
            char character = value.charAt(index);
            builder.append(character == '\'' ? "\"'\"" : "'" + character + "'");
        }
        return builder.append(")").toString();
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String slug(String value) {
        String ascii = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        ascii = ascii.replaceAll("[^a-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
        return ascii.isBlank() ? "page" : ascii;
    }

    private static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }

    private static String requiredAnyEnv(String primaryKey, String fallbackKey) {
        String value = System.getenv(primaryKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        value = System.getenv(fallbackKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        throw new IllegalStateException("Missing required environment variable: " + primaryKey + " or " + fallbackKey);
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static long longEnvOrDefault(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static void settle(WebDriver driver) {
        sleep(TINY_SETTLE_MS);
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
        } catch (RuntimeException ignored) {
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class Environment {
        public final String name;
        public final String baseUrl;
        public final String email;
        public final String password;

        public Environment(String name, String baseUrl, String email, String password) {
            this.name = name;
            this.baseUrl = baseUrl;
            this.email = email;
            this.password = password;
        }
    }

    public static class GroupSpec {
        public String name;
        public List<String> children;

        public GroupSpec() {
        }

        public GroupSpec(String name, List<String> children) {
            this.name = name;
            this.children = children;
        }
    }

    public static class ProdGroupSpec {
        public final int topIndex;
        public final String displayName;
        public final String menuLabel;
        public final List<String> children;

        public ProdGroupSpec(int topIndex, String displayName, String menuLabel, List<String> children) {
            this.topIndex = topIndex;
            this.displayName = displayName;
            this.menuLabel = menuLabel;
            this.children = children;
        }
    }

    public static class StgMenuReport {
        public String generatedAt;
        public String environment;
        public String navigationMethod;
        public List<MenuVisit> visits = List.of();
    }

    public static class MenuVisit {
        public int expectedOrder;
        public String group;
        public String label;
        public String finalUrl;
        public String path;
        public String heading;
        public String bodyPreview;
        public boolean loginRedirected;
        public boolean notFoundLike;
        public boolean modalStillOpen;
        public String screenshot;
        public String error;
    }

    private static class CardPoint {
        private final int x;
        private final int y;

        private CardPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}

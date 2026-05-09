package com.aiwriter.rpa;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spike test to validate XHS (小红书) creator backend RPA publish flow.
 * Run manually — NOT part of CI.
 *
 * Prerequisites:
 * - Run `mvn generate-test-resources` first to download Chromium
 * - Have a phone with XHS app for QR code scanning
 */
@Disabled("Manual spike — run individually")
class XhsPublishSpike {

    private static final String PUBLISH_URL = "https://creator.xiaohongshu.com/publish/publish";
    private static final String LOGIN_URL_PREFIX = "https://creator.xiaohongshu.com/login";
    private static final Path SESSION_DIR = Paths.get(System.getProperty("user.home"), ".aiwriter", "rpa-session");
    private static final Path SESSION_FILE = SESSION_DIR.resolve("xhs-state.json");
    private static final Path SCREENSHOT_DIR = Paths.get("target", "rpa-spike-screenshots");

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private final List<StepResult> results = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        playwright = Playwright.create();
        Files.createDirectories(SCREENSHOT_DIR);
        Files.createDirectories(SESSION_DIR);
    }

    @AfterEach
    void tearDown() {
        if (context != null) context.close();
        if (browser != null) browser.close();
        playwright.close();
        printAssessment();
    }

    @Test
    void fullPublishFlow() {
        long start;

        // ── Step 1: Session & Login ──
        start = System.currentTimeMillis();
        try {
            createOrRestoreSession();
            page = context.newPage();

            boolean loggedIn = isLoggedIn();
            if (!loggedIn) {
                System.out.println("[Login] Session expired or first run. Waiting for re-login...");
                if (Files.exists(SESSION_FILE)) {
                    Files.delete(SESSION_FILE);
                    System.out.println("[Login] Deleted expired session file.");
                }
                waitForLoginAndSave();
                page.navigate(PUBLISH_URL);
                page.waitForLoadState(LoadState.NETWORKIDLE);
            } else {
                System.out.println("[Login] Session valid, already on publish page.");
                context.storageState(new BrowserContext.StorageStateOptions().setPath(SESSION_FILE));
            }
            record("Login & Session", true, "URL check", System.currentTimeMillis() - start, null);
        } catch (Exception e) {
            record("Login & Session", false, "URL check", System.currentTimeMillis() - start, e.getMessage());
            takeScreenshot("login-failed");
            return;
        }

        // ── Step 2: Upload Image ──
        uploadImage();

        // ── Step 3: Fill Title ──
        fillTitle("[TEST] RPA Spike 验证标题");

        // ── Step 4: Fill Body ──
        fillBody("这是一条 RPA Spike 自动化测试内容，验证发布流程是否可行。可手动删除。");

        // ── Step 5: Click Publish ──
        clickPublish();
    }

    // ── Session Management ──────────────────────────────────────

    private BrowserContext createOrRestoreSession() {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(1280, 800);

        if (Files.exists(SESSION_FILE)) {
            System.out.println("[Session] Found saved session, attempting to restore...");
            options.setStorageStatePath(SESSION_FILE);
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            context = browser.newContext(options);
            return context;
        }

        System.out.println("[Session] No saved session found. Launching headed browser for QR login...");
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        context = browser.newContext(options);
        return context;
    }

    private boolean isLoggedIn() {
        page.navigate(PUBLISH_URL);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        String currentUrl = page.url();
        return !currentUrl.contains("/login");
    }

    private void waitForLoginAndSave() {
        System.out.println("[Session] Waiting for QR scan login (headed browser window)...");
        System.out.println("[Session] Please scan QR code with XHS app on your phone.");

        // Wait up to 120s for URL to change away from login page
        page.waitForURL(url -> !url.contains("/login"),
                new Page.WaitForURLOptions().setTimeout(120_000));

        System.out.println("[Session] Login detected! Saving session...");
        context.storageState(new BrowserContext.StorageStateOptions().setPath(SESSION_FILE));
        System.out.println("[Session] Session saved to " + SESSION_FILE);
    }

    // ── Assessment Reporting ────────────────────────────────────

    record StepResult(String name, boolean passed, String selector, long durationMs, String error) {}

    void record(String name, boolean passed, String selector, long durationMs, String error) {
        results.add(new StepResult(name, passed, selector, durationMs, error));
        String icon = passed ? "✅" : "❌";
        System.out.printf("  %s %s (%dms) selector=%s%n", icon, name, durationMs, selector);
        if (error != null) {
            System.out.printf("     Error: %s%n", error);
        }
    }

    private void printAssessment() {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("  XHS RPA SPIKE — FEASIBILITY ASSESSMENT");
        System.out.println("═".repeat(60));

        long passed = results.stream().filter(r -> r.passed).count();
        long failed = results.stream().filter(r -> !r.passed).count();
        String verdict = failed == 0 ? "PASS" : "FAIL";

        System.out.printf("  Verdict: %s (%d/%d steps passed)%n%n", verdict, passed, results.size());

        for (StepResult r : results) {
            String icon = r.passed ? "✅" : "❌";
            System.out.printf("  %s %-30s %5dms  selector=%s%n",
                    icon, r.name, r.durationMs, r.selector);
            if (r.error != null) {
                System.out.printf("     ↳ %s%n", r.error);
            }
        }

        System.out.println("═".repeat(60));
    }

    private void takeScreenshot(String name) {
        try {
            Path path = SCREENSHOT_DIR.resolve(System.currentTimeMillis() + "-" + name + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(path));
            System.out.println("  📸 Screenshot saved: " + path);
        } catch (Exception e) {
            System.out.println("  ⚠️ Screenshot failed: " + e.getMessage());
        }
    }
}

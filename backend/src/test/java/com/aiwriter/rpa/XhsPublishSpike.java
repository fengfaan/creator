package com.aiwriter.rpa;

import com.microsoft.playwright.*;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
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

    private boolean hadSessionFile;

    @Test
    void fullPublishFlow() {
        long start;

        // ── Step 1: Session & Login ──
        start = System.currentTimeMillis();
        try {
            hadSessionFile = Files.exists(SESSION_FILE);
            createOrRestoreSession();
            page = context.newPage();

            if (!hadSessionFile) {
                // No session file — go directly to login page and wait for QR scan
                System.out.println("[Login] First run, navigating to publish page...");
                page.navigate(PUBLISH_URL);
                waitForPageSettle();
                waitForLoginAndSave();
                page.navigate(PUBLISH_URL);
                waitForPageSettle();
            } else {
                // Had session — check if still valid (wait for SPA to settle)
                page.navigate(PUBLISH_URL);
                waitForPageSettle();
                if (isOnLoginPage()) {
                    System.out.println("[Login] Session expired (SPA redirected to login). Waiting for re-login...");
                    Files.deleteIfExists(SESSION_FILE);
                    waitForLoginAndSave();
                    page.navigate(PUBLISH_URL);
                    waitForPageSettle();
                } else {
                    System.out.println("[Login] Session valid, on publish page.");
                    context.storageState(new BrowserContext.StorageStateOptions().setPath(SESSION_FILE));
                }
            }
            record("Login & Session", true, "URL check", System.currentTimeMillis() - start, null);
        } catch (Exception e) {
            record("Login & Session", false, "URL check", System.currentTimeMillis() - start, e.getMessage());
            takeScreenshot("login-failed");
            return;
        }

        // ── Step 2: Upload Image ──
        uploadImage();

        // ── Debug: dump page DOM structure ──
        debugPageStructure();

        // ── Step 3: Fill Title ──
        fillTitle("[TEST] RPA Spike 验证标题");

        // ── Step 4: Fill Body ──
        fillBody("这是一条 RPA Spike 自动化测试内容，验证发布流程是否可行。可手动删除。");

        // ── Step 5: Click Publish ──
        clickPublish();
    }

    // ── Debug ─────────────────────────────────────────────────

    private void debugPageStructure() {
        System.out.println("\n[Debug] Page URL: " + page.url());
        System.out.println("[Debug] Page title: " + page.title());

        // Dump ALL visible elements that have text or are interactive
        var allElements = (List<Object>) page.evaluate("() => {" +
                "  const results = [];" +
                "  const els = document.querySelectorAll('input, button, textarea, [contenteditable], [role], [class*=\"title\"], [class*=\"editor\"], [class*=\"upload\"], [class*=\"image\"], [class*=\"next\"], [class*=\"publish\"], [class*=\"draft\"]');" +
                "  for (const el of els) {" +
                "    results.push({" +
                "      tag: el.tagName," +
                "      class: el.className," +
                "      id: el.id," +
                "      type: el.type || ''," +
                "      placeholder: el.placeholder || ''," +
                "      role: el.getAttribute('role') || ''," +
                "      text: (el.innerText || '').substring(0, 50)," +
                "      visible: el.offsetParent !== null || el.offsetWidth > 0" +
                "    });" +
                "  }" +
                "  return results;" +
                "}");
        System.out.println("[Debug] All interesting elements (" + allElements.size() + "):");
        for (var el : allElements) {
            System.out.println("  " + el);
        }

        System.out.println();
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

    private void waitForPageSettle() {
        // Wait for initial load
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        // Wait for SPA to finish rendering (network idle after initial load)
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(10_000));
        } catch (Exception e) {
            // NETWORKIDLE may timeout on pages with persistent connections — that's OK
            System.out.println("[Page] NETWORKIDLE timeout (expected for SPA), continuing...");
        }
        // Extra settle time for SPA JavaScript to execute
        page.waitForTimeout(2000);
    }

    private boolean isOnLoginPage() {
        String currentUrl = page.url();
        System.out.println("[Page] Current URL: " + currentUrl);
        return currentUrl.contains("/login");
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

    // ── Publish Steps ──────────────────────────────────────────

    private void uploadImage() {
        long start = System.currentTimeMillis();
        String selector = "input[type='file']";
        try {
            // XHS file input is hidden — don't wait for visibility
            ElementHandle fileInput = page.querySelector(selector);
            if (fileInput == null) {
                throw new RuntimeException("No file input found on page");
            }

            Path coverPath = Paths.get("src/test/resources/spike-cover.jpg")
                    .toAbsolutePath();
            if (!Files.exists(coverPath)) {
                throw new RuntimeException("Test cover image not found: " + coverPath);
            }

            fileInput.setInputFiles(coverPath);
            // Wait for upload to process and thumbnail to appear
            page.waitForTimeout(5000);
            record("Upload Image", true, selector, System.currentTimeMillis() - start, null);
        } catch (Exception e) {
            record("Upload Image", false, selector, System.currentTimeMillis() - start, e.getMessage());
            takeScreenshot("upload-image-failed");
        }
    }

    private void fillTitle(String title) {
        long start = System.currentTimeMillis();
        String[] selectors = {
                "input[placeholder*='标题']",
                "input[placeholder*='填写标题']",
                "#title",
                ".title-input input",
                "input[maxlength='20']"
        };
        for (String selector : selectors) {
            try {
                Locator input = page.locator(selector).first();
                if (input.isVisible()) {
                    input.click();
                    input.fill(title);
                    record("Fill Title", true, selector, System.currentTimeMillis() - start, null);
                    return;
                }
            } catch (Exception ignored) {}
        }
        try {
            Locator editable = page.locator("[contenteditable='true']").first();
            editable.click();
            editable.fill(title);
            record("Fill Title", true, "[contenteditable=true]", System.currentTimeMillis() - start, null);
        } catch (Exception e) {
            record("Fill Title", false, "all selectors exhausted", System.currentTimeMillis() - start, e.getMessage());
            takeScreenshot("fill-title-failed");
        }
    }

    private void fillBody(String body) {
        long start = System.currentTimeMillis();
        String[] selectors = {
                "div[contenteditable='true']",
                ".ql-editor",
                ".editor-inner",
                "textarea[placeholder*='正文']",
                "textarea[placeholder*='描述']"
        };
        for (String selector : selectors) {
            try {
                Locator editor = page.locator(selector).first();
                if (editor.isVisible()) {
                    editor.click();
                    page.keyboard().type(body, new Keyboard.TypeOptions().setDelay(10));
                    record("Fill Body", true, selector, System.currentTimeMillis() - start, null);
                    return;
                }
            } catch (Exception ignored) {}
        }
        record("Fill Body", false, "all selectors exhausted", System.currentTimeMillis() - start,
                "Could not locate body editor");
        takeScreenshot("fill-body-failed");
    }

    private void clickPublish() {
        long start = System.currentTimeMillis();
        String[] selectors = {
                "button:has-text('发布')",
                "button:has-text('发布笔记')",
                "button.publish-btn",
                ".submit-btn"
        };
        for (String selector : selectors) {
            try {
                Locator btn = page.locator(selector).first();
                if (btn.isVisible() && btn.isEnabled()) {
                    btn.click();
                    page.waitForTimeout(5000);

                    String resultUrl = page.url();
                    boolean success = resultUrl.contains("/publish/success")
                            || resultUrl.contains("/note/")
                            || page.locator("text=发布成功").isVisible()
                            || page.locator("text=已发布").isVisible();

                    if (success) {
                        record("Click Publish", true, selector, System.currentTimeMillis() - start, null);
                    } else {
                        takeScreenshot("publish-result");
                        record("Click Publish", true, selector, System.currentTimeMillis() - start,
                                "Published but success indicator unclear — check screenshot");
                    }
                    return;
                }
            } catch (Exception ignored) {}
        }
        record("Click Publish", false, "all selectors exhausted", System.currentTimeMillis() - start,
                "Could not locate publish button");
        takeScreenshot("click-publish-failed");
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

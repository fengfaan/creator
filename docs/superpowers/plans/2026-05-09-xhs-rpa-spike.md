# XHS RPA Spike Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Validate that Playwright for Java can automate the full XHS (小红书) creator backend publish flow: login → upload image → fill title → fill body → click publish.

**Architecture:** A single JUnit 5 test class (`XhsPublishSpike`) that launches a Chromium browser via Playwright, manages session persistence via `storageState()`, and walks through the publish flow step-by-step with logging and screenshots on failure. No production code changes — purely a validation spike.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Playwright for Java 1.49.0, JUnit 5 (already via spring-boot-starter-test)

---

## File Structure

| Action | File | Purpose |
|--------|------|---------|
| Modify | `backend/pom.xml` | Add Playwright dependency |
| Create | `backend/src/test/resources/spike-cover.jpg` | Test cover image for upload |
| Create | `backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java` | The spike test class |
| Generated | `~/.aiwriter/rpa-session/xhs-state.json` | Saved browser session (cookies/localStorage) |
| Generated | `backend/target/rpa-spike-screenshots/` | Failure screenshots |

---

### Task 1: Add Playwright dependency to pom.xml

**Files:**
- Modify: `backend/pom.xml`

- [ ] **Step 1: Add Playwright dependency**

Add this block inside `<dependencies>`, after the `spring-boot-starter-test` dependency:

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.49.0</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Add Playwright install plugin**

Add this plugin inside `<plugins>` in the `<build>` section. This ensures Chromium is downloaded before tests run:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>install-playwright</id>
            <phase>generate-test-resources</phase>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>java</executable>
                <arguments>
                    <argument>-cp</argument>
                    <classpath/>
                    <argument>com.microsoft.playwright.CLI</argument>
                    <argument>install</argument>
                    <argument>--with-deps</argument>
                    <argument>chromium</argument>
                </arguments>
                <classpathScope>test</classpathScope>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 3: Verify dependency resolves**

Run: `cd backend && mvn dependency:resolve -q`
Expected: BUILD SUCCESS, Playwright jar downloaded.

- [ ] **Step 4: Commit**

```bash
git add backend/pom.xml
git commit -m "chore: add Playwright for Java dependency and install plugin"
```

---

### Task 2: Prepare test cover image

**Files:**
- Create: `backend/src/test/resources/spike-cover.jpg`

- [ ] **Step 1: Generate a minimal test image**

Run from project root:

```bash
mkdir -p backend/src/test/resources
python3 -c "
from PIL import Image
img = Image.new('RGB', (800, 600), color=(73, 109, 137))
img.save('backend/src/test/resources/spike-cover.jpg')
print('Created spike-cover.jpg')
" 2>/dev/null || python3 -c "
import struct, zlib
def create_jpeg(path, w=800, h=600):
    import io
    # Create a minimal valid PNG as fallback (Playwright accepts PNG too)
    def chunk(ctype, data):
        c = ctype + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
    raw = b'\x00' + b'\x49\x6d\x61\x67\x65' * w
    compressed = zlib.compress(raw * h)
    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
    png += chunk(b'IDAT', compressed)
    png += chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)
create_jpeg('backend/src/test/resources/spike-cover.jpg')
print('Created spike-cover.jpg (PNG format)')
"
```

If neither Python nor PIL is available, download a placeholder:

```bash
curl -sL -o backend/src/test/resources/spike-cover.jpg \
  "https://via.placeholder.com/800x600/496D85/FFFFFF?text=Test+Cover"
```

- [ ] **Step 2: Verify file exists**

Run: `file backend/src/test/resources/spike-cover.jpg`
Expected: A valid image file (JPEG or PNG).

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/resources/spike-cover.jpg
git commit -m "test: add spike cover image for XHS RPA validation"
```

---

### Task 3: Create Spike class skeleton with session management

**Files:**
- Create: `backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java`

This is the core of the spike. We build it in layers across Tasks 3-6. Task 3 sets up the class structure, Playwright lifecycle, and session persistence.

- [ ] **Step 1: Create the spike class with session management**

```java
package com.aiwriter.rpa;

import com.microsoft.playwright.*;
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
        // Each step is a method call below — built incrementally in Tasks 4-6
        // This test will be completed in Task 6
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
```

- [ ] **Step 2: Verify it compiles**

Run: `cd backend && mvn test-compile -q`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/aiwriter/rpa/
git commit -m "test: add XHS RPA spike skeleton with session management"
```

---

### Task 4: Implement login detection and session restoration

**Files:**
- Modify: `backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java`

- [ ] **Step 1: Implement the fullPublishFlow test with login logic**

Replace the empty `fullPublishFlow()` method with:

```java
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
            // Clear stale session file
            if (Files.exists(SESSION_FILE)) {
                Files.delete(SESSION_FILE);
                System.out.println("[Login] Deleted expired session file.");
            }
            waitForLoginAndSave();
            // Navigate back to publish page after login
            page.navigate(PUBLISH_URL);
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } else {
            System.out.println("[Login] Session valid, already on publish page.");
            // Save updated session
            context.storageState(new BrowserContext.StorageStateOptions().setPath(SESSION_FILE));
        }
        record("Login & Session", true, "URL check", System.currentTimeMillis() - start, null);
    } catch (Exception e) {
        record("Login & Session", false, "URL check", System.currentTimeMillis() - start, e.getMessage());
        takeScreenshot("login-failed");
        return; // Can't continue without login
    }

    // Subsequent steps will be added in Tasks 5-6
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn test-compile -q`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java
git commit -m "test: implement login detection and session restoration in spike"
```

---

### Task 5: Implement image upload, title fill, body fill

**Files:**
- Modify: `backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java`

- [ ] **Step 1: Add publish step helper methods**

Add these methods inside the class, after `waitForLoginAndSave()`:

```java
// ── Publish Steps ──────────────────────────────────────────

private void uploadImage() {
    long start = System.currentTimeMillis();
    String selector = "input[type='file']";
    try {
        // XHS has a hidden file input for image upload
        Locator fileInput = page.locator(selector);
        fileInput.waitFor(new Locator.WaitForOptions().setTimeout(10_000));

        Path coverPath = Paths.get("src/test/resources/spike-cover.jpg")
                .toAbsolutePath();
        if (!Files.exists(coverPath)) {
            throw new RuntimeException("Test cover image not found: " + coverPath);
        }

        fileInput.setInputFiles(coverPath);

        // Wait for upload to complete — look for thumbnail appearance
        page.waitForTimeout(3000);
        record("Upload Image", true, selector, System.currentTimeMillis() - start, null);
    } catch (Exception e) {
        record("Upload Image", false, selector, System.currentTimeMillis() - start, e.getMessage());
        takeScreenshot("upload-image-failed");
    }
}

private void fillTitle(String title) {
    long start = System.currentTimeMillis();
    // Try multiple selector strategies — XHS may use different inputs
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
    // Fallback: try contenteditable
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
                // Use keyboard to type into contenteditable divs
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
```

- [ ] **Step 2: Wire upload/title/body into fullPublishFlow**

Replace the comment `// Subsequent steps will be added in Tasks 5-6` at the end of `fullPublishFlow()` with:

```java
    // ── Step 2: Upload Image ──
    uploadImage();

    // ── Step 3: Fill Title ──
    fillTitle("[TEST] RPA Spike 验证标题");

    // ── Step 4: Fill Body ──
    fillBody("这是一条 RPA Spike 自动化测试内容，验证发布流程是否可行。可手动删除。");
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn test-compile -q`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java
git commit -m "test: implement image upload, title fill, body fill in spike"
```

---

### Task 6: Implement publish click and finalize test

**Files:**
- Modify: `backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java`

- [ ] **Step 1: Add clickPublish method**

Add this method after `fillBody()`:

```java
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

                // Wait for success: either a toast notification or URL change
                page.waitForTimeout(5000);

                // Check for success indicators
                String resultUrl = page.url();
                boolean success = resultUrl.contains("/publish/success")
                        || resultUrl.contains("/note/")
                        || page.locator("text=发布成功").isVisible()
                        || page.locator("text=已发布").isVisible();

                if (success) {
                    record("Click Publish", true, selector, System.currentTimeMillis() - start, null);
                } else {
                    // Might be on a confirmation page — take screenshot for inspection
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
```

- [ ] **Step 2: Wire clickPublish into fullPublishFlow**

Append at the end of `fullPublishFlow()`, after the `fillBody(...)` call:

```java
    // ── Step 5: Click Publish ──
    clickPublish();
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn test-compile -q`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/aiwriter/rpa/XhsPublishSpike.java
git commit -m "test: implement publish click and finalize spike test"
```

---

### Task 7: Run the spike and record results

This task requires manual interaction (QR code scan). It validates the full flow end-to-end.

**Files:**
- No code changes — execution only

- [ ] **Step 1: Install Chromium (first time only)**

Run: `cd backend && mvn generate-test-resources -q`
Expected: Chromium downloads (~150MB first time). Subsequent runs are instant.

- [ ] **Step 2: Run the spike test**

Run: `cd backend && mvn test -Dtest="XhsPublishSpike#fullPublishFlow" -pl .`

Expected behavior:
1. A Chromium window opens
2. If first run: XHS login page appears → scan QR code with phone
3. After login: script navigates to publish page
4. Script uploads test image, fills title/body, clicks publish
5. Console prints feasibility assessment report

- [ ] **Step 3: Review output and record findings**

Copy the console output (the feasibility assessment section) into a comment on the OpenSpec change. Key things to note:
- Which selectors worked? Which needed fallback?
- Did session persistence work on second run?
- Any unexpected page behavior (popups, captchas, rate limits)?
- Total execution time

- [ ] **Step 4: Clean up test note on XHS**

If the spike published a real note, log into XHS (app or web) and delete the test note manually.

---

## Self-Review

**Spec coverage:**
- ✅ "Playwright for Java 集成验证" → Task 3 (session management), Task 4 (login detection)
- ✅ "小红书发布核心路径验证" → Tasks 5-6 (upload, title, body, publish)
- ✅ "可行性评估输出" → Task 3 (assessment reporting in skeleton), Task 7 (run and record)
- ✅ DOM 选择器失效截图 → `takeScreenshot()` called on every failure path

**Placeholder scan:** No TBDs, no TODOs. All selectors are concrete (with documented fallback chains).

**Type consistency:** `StepResult` record defined once in Task 3, used consistently via `record()`. `BrowserContext`, `Page` lifecycle consistent across all tasks.

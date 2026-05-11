package com.aiwriter.rpa;

import com.aiwriter.model.RpaPublishRequest;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class XhsRpaPublisher implements RpaPublisher {
    private static final String PUBLISH_URL = "https://creator.xiaohongshu.com/publish/publish";
    private final List<BrowserSession> activeSessions = new CopyOnWriteArrayList<>();
    private final Map<String, BrowserSession> sessionsByJob = new ConcurrentHashMap<>();

    @Value("${app.data-dir}")
    private String dataDir;

    @Override
    public String platform() {
        return "xhs";
    }

    @Override
    public void prepareDraft(String jobId, RpaPublishRequest request, RpaJobLogger logger) throws Exception {
        Path sessionDir = Path.of(dataDir, "rpa-session");
        Path sessionFile = sessionDir.resolve("xhs-state.json");
        Files.createDirectories(sessionDir);

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        boolean keepBrowserOpen = false;
        try {
            logger.info("正在启动浏览器");
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            Browser.NewContextOptions options = new Browser.NewContextOptions().setViewportSize(1280, 800);
            if (Files.exists(sessionFile)) {
                options.setStorageStatePath(sessionFile);
                logger.info("已加载本地小红书登录态");
            }
            context = browser.newContext(options);
            Page page = context.newPage();
            logger.info("正在打开小红书发布页");
            page.navigate(PUBLISH_URL);
            waitForPageSettle(page, logger);
            if (isLoginPage(page)) {
                logger.warn("登录态失效，请在浏览器中扫码登录小红书");
                page.waitForURL(url -> !url.contains("/login"),
                        new Page.WaitForURLOptions().setTimeout(120_000));
                waitForPageSettle(page, logger);
                context.storageState(new BrowserContext.StorageStateOptions().setPath(sessionFile));
                logger.success("登录态已保存");
                logger.info("正在重新打开发布页");
                page.navigate(PUBLISH_URL);
                waitForPageSettle(page, logger);
            } else {
                context.storageState(new BrowserContext.StorageStateOptions().setPath(sessionFile));
                logger.success("小红书登录态有效");
            }

            logger.info("当前页面: " + page.url());
            clickImagePostTab(page, logger);
            uploadCoverIfPresent(page, request.getCoverPath(), logger);
            fillTitle(page, request.getTitle(), logger);
            fillBody(page, request.getContent(), logger);
            logger.success("草稿内容已准备，未点击发布按钮");
            logger.warn("浏览器窗口会保持打开，请在小红书后台检查后点击确认发布");
            BrowserSession session = new BrowserSession(playwright, browser, context, page);
            activeSessions.add(session);
            sessionsByJob.put(jobId, session);
            keepBrowserOpen = true;
        } finally {
            if (!keepBrowserOpen) {
                closeQuietly(context);
                closeQuietly(browser);
                closeQuietly(playwright);
            }
        }
    }

    @Override
    public void confirm(String jobId, RpaJobLogger logger) {
        BrowserSession session = sessionsByJob.get(jobId);
        if (session == null) {
            throw new RpaException(404, "未找到待确认的小红书浏览器会话");
        }
        Page page = session.page();
        logger.info("正在定位小红书发布按钮");
        String[] selectors = {
                "button:has-text('发布')",
                "button:has-text('发布笔记')",
                "xpath=//button[contains(normalize-space(.), '发布')]",
                ".publish-btn",
                ".submit-btn"
        };
        if (!clickFirstVisible(page, selectors, 4_000) && !clickByExactText(page, "发布")) {
            throw new RpaException(502, "未定位到小红书发布按钮，当前页面: " + page.url());
        }
        page.waitForTimeout(3000);
        logger.success("已点击小红书发布按钮");
        cleanupJobSession(jobId);
    }

    private void clickImagePostTab(Page page, RpaJobLogger logger) {
        logger.info("正在寻找上传图文入口");
        if (clickByExactText(page, "上传图文")) {
            page.waitForTimeout(1500);
            logger.success("已切换到上传图文");
            return;
        }
        String[] selectors = {
                "text=上传图文",
                "xpath=//*[normalize-space()='上传图文']",
                "button:has-text('上传图文')",
                "[role='tab']:has-text('上传图文')"
        };
        if (clickFirstVisible(page, selectors, 1_500)) {
            page.waitForTimeout(1500);
            logger.success("已切换到上传图文");
        } else {
            logger.warn("未找到上传图文入口，继续尝试当前页面");
        }
    }

    private void uploadCoverIfPresent(Page page, String coverPath, RpaJobLogger logger) {
        if (coverPath == null || coverPath.isBlank()) {
            throw new RpaException(400, "小红书图文发布需要先提供封面图路径");
        }
        Path path = Path.of(coverPath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new RpaException(400, "封面图不存在: " + path);
        }
        ElementHandle input = page.querySelector("input[type='file']");
        if (input == null) {
            throw new RpaException(502, "未找到小红书图片上传控件");
        }
        input.setInputFiles(path);
        page.waitForTimeout(5000);
        logger.success("封面图已上传");
    }

    private void fillTitle(Page page, String title, RpaJobLogger logger) {
        logger.info("正在填写标题");
        String[] selectors = {
                "input[placeholder*='标题']",
                "textarea[placeholder*='标题']",
                "[contenteditable='true'][placeholder*='标题']",
                "[contenteditable='true']"
        };
        if (!fillFirstVisible(page, selectors, title, 4_000)) {
            throw new RpaException(502, "未定位到小红书标题输入框，当前页面: " + page.url());
        }
        logger.success("标题已填写");
    }

    private void fillBody(Page page, String content, RpaJobLogger logger) {
        logger.info("正在填写正文");
        String[] selectors = {
                "textarea[placeholder*='正文']",
                "textarea[placeholder*='描述']",
                ".ql-editor",
                ".editor-inner",
                "div[contenteditable='true']"
        };
        if (!fillFirstVisible(page, selectors, content, 4_000)) {
            throw new RpaException(502, "未定位到小红书正文输入框，当前页面: " + page.url());
        }
        logger.success("正文已填写");
    }

    private boolean clickFirstVisible(Page page, String[] selectors, int timeoutMs) {
        for (String selector : selectors) {
            try {
                Locator locator = page.locator(selector).first();
                locator.waitFor(new Locator.WaitForOptions().setTimeout(timeoutMs));
                if (locator.isVisible() && locator.isEnabled()) {
                    locator.click();
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean clickByExactText(Page page, String text) {
        Object clicked = page.evaluate("""
            (targetText) => {
              const elements = Array.from(document.querySelectorAll('button, [role="tab"], div, span, a'));
              const target = elements.find((el) => {
                const text = (el.innerText || el.textContent || '').trim();
                const rect = el.getBoundingClientRect();
                return text === targetText && rect.width > 0 && rect.height > 0;
              });
              if (!target) return false;
              target.click();
              return true;
            }
            """, text);
        return Boolean.TRUE.equals(clicked);
    }

    private boolean fillFirstVisible(Page page, String[] selectors, String value, int timeoutMs) {
        for (String selector : selectors) {
            try {
                Locator locator = page.locator(selector).first();
                locator.waitFor(new Locator.WaitForOptions().setTimeout(timeoutMs));
                if (locator.isVisible()) {
                    locator.click();
                    locator.fill(value);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private void waitForPageSettle(Page page, RpaJobLogger logger) {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(10_000));
        } catch (Exception ignored) {
            logger.warn("页面仍有长连接，继续执行");
        }
        page.waitForTimeout(1500);
    }

    private boolean isLoginPage(Page page) {
        return page.url().contains("/login");
    }

    @PreDestroy
    void closeActiveSessions() {
        for (BrowserSession session : activeSessions) {
            closeQuietly(session.context());
            closeQuietly(session.browser());
            closeQuietly(session.playwright());
        }
        activeSessions.clear();
        sessionsByJob.clear();
    }

    private void cleanupJobSession(String jobId) {
        BrowserSession session = sessionsByJob.remove(jobId);
        if (session == null) {
            return;
        }
        activeSessions.remove(session);
        closeQuietly(session.context());
        closeQuietly(session.browser());
        closeQuietly(session.playwright());
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private record BrowserSession(Playwright playwright, Browser browser, BrowserContext context, Page page) {
    }
}

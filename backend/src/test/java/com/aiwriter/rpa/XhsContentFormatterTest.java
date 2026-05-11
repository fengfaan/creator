package com.aiwriter.rpa;

import com.aiwriter.model.RpaPublishRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class XhsContentFormatterTest {
    @Test
    void convertsMarkdownDraftToXhsPlainText() {
        RpaPublishRequest request = new RpaPublishRequest(
                "xhs",
                "# AI 写作工具",
                """
                # AI 写作工具

                ## 我的做法

                1. **先写长文草稿**
                2. 自动拆成[小红书版](https://example.com)

                - 保留重点
                - 去掉 Markdown

                ![cover](/assets/cover.png)

                > 适合多平台分发
                """,
                "/tmp/cover.png"
        );

        RpaPublishRequest formatted = XhsContentFormatter.format(request);

        assertEquals("AI 写作工具", formatted.getTitle());
        assertEquals("""
                📌 我的做法

                ① 先写长文草稿
                ② 自动拆成小红书版

                • 保留重点
                • 去掉 Markdown

                ▎适合多平台分发""", formatted.getContent());
        assertFalse(formatted.getContent().contains("!["));
        assertFalse(formatted.getContent().contains("**"));
    }
}

package com.aiwriter.rpa;

import com.aiwriter.model.AiCheckIssue;
import com.aiwriter.model.RpaPublishRequest;
import com.aiwriter.service.AiContentCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpaJobServiceTest {

    private AiContentCheckService contentCheckService;

    @BeforeEach
    void setUp() {
        contentCheckService = new AiContentCheckService(null, null, null);
    }

    @Test
    void createsJobAndStreamsLogsFromPublisher() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        RpaPublisher publisher = new RpaPublisher() {
            @Override
            public String platform() {
                return "xhs";
            }

            @Override
            public void prepareDraft(String jobId, RpaPublishRequest request, RpaJobLogger logger) {
                called.set(true);
                logger.success("草稿已准备");
            }
        };
        RpaJobService service = new RpaJobService(List.of(publisher), System.getProperty("user.home") + "/wiki/AI Writer", contentCheckService);

        var response = service.start(new RpaPublishRequest("xhs", "标题", "正文", ""));
        Thread.sleep(200);

        assertThat(called).isTrue();
        assertThat(service.get(response.getJobId()).getStatus()).isEqualTo("WAITING_CONFIRMATION");
        assertThat(service.logs(response.getJobId(), 0))
                .extracting("message")
                .contains("草稿已准备", "已停在人工确认步骤，请检查页面后点击确认发布");
    }

    @Test
    void confirmsWaitingJobThroughPublisher() throws Exception {
        AtomicBoolean confirmed = new AtomicBoolean(false);
        RpaPublisher publisher = new RpaPublisher() {
            @Override
            public String platform() {
                return "xhs";
            }

            @Override
            public void prepareDraft(String jobId, RpaPublishRequest request, RpaJobLogger logger) {
                logger.success("草稿已准备");
            }

            @Override
            public void confirm(String jobId, RpaJobLogger logger) {
                confirmed.set(true);
                logger.success("确认发布完成");
            }
        };
        RpaJobService service = new RpaJobService(List.of(publisher), System.getProperty("user.home") + "/wiki/AI Writer", contentCheckService);

        var response = service.start(new RpaPublishRequest("xhs", "标题", "正文", ""));
        Thread.sleep(200);
        service.confirm(response.getJobId());
        Thread.sleep(200);

        assertThat(confirmed).isTrue();
        assertThat(service.get(response.getJobId()).getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void rejectsUnsupportedPlatform() {
        RpaJobService service = new RpaJobService(List.of(), System.getProperty("user.home") + "/wiki/AI Writer", contentCheckService);

        assertThatThrownBy(() -> service.start(new RpaPublishRequest("wechat", "标题", "正文", "")))
                .isInstanceOf(RpaException.class)
                .hasMessage("当前只支持小红书 RPA");
    }

    @Test
    void blocksPublishingWhenContactInfoDetected() throws Exception {
        RpaPublisher publisher = new RpaPublisher() {
            @Override public String platform() { return "xhs"; }
            @Override public void prepareDraft(String jobId, RpaPublishRequest request, RpaJobLogger logger) {
                logger.success("should not reach here");
            }
            @Override public void confirm(String jobId, RpaJobLogger logger) {}
        };
        RpaJobService service = new RpaJobService(List.of(publisher), System.getProperty("user.home") + "/wiki/AI Writer", contentCheckService);

        var response = service.start(new RpaPublishRequest("xhs", "加微信abc123", "正文内容", ""));
        Thread.sleep(200);

        assertThat(service.get(response.getJobId()).getStatus()).isEqualTo("FAILED");
        assertThat(service.logs(response.getJobId(), 0))
                .extracting("message")
                .anyMatch(msg -> ((String) msg).contains("联系方式"));
    }
}

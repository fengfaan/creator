package com.aiwriter.rpa;

import com.aiwriter.model.RpaJobResponse;
import com.aiwriter.model.RpaLogEntry;
import com.aiwriter.model.RpaPublishRequest;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RpaJobService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<String, RpaJobState> jobs = new ConcurrentHashMap<>();
    private final Map<String, RpaPublisher> publishers;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "rpa-job-worker");
        thread.setDaemon(true);
        return thread;
    });

    public RpaJobService(List<RpaPublisher> publishers) {
        this.publishers = publishers.stream()
                .collect(java.util.stream.Collectors.toMap(RpaPublisher::platform, publisher -> publisher));
    }

    public RpaJobResponse start(RpaPublishRequest request) {
        String platform = normalizePlatform(request == null ? null : request.getPlatform());
        RpaPublisher publisher = publishers.get(platform);
        if (publisher == null) {
            throw new RpaException(400, "当前只支持小红书 RPA");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RpaException(400, "标题不能为空");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new RpaException(400, "正文不能为空");
        }

        String jobId = UUID.randomUUID().toString();
        RpaJobState state = new RpaJobState(jobId, platform);
        jobs.put(jobId, state);
        state.add("INFO", "任务已创建，等待浏览器执行");

        executor.submit(() -> runJob(state, request, publisher));
        return state.toResponse("任务已开始");
    }

    public RpaJobResponse get(String jobId) {
        return requireJob(jobId).toResponse(null);
    }

    public List<RpaLogEntry> logs(String jobId, long after) {
        return requireJob(jobId).logs.stream()
                .filter(log -> log.getSequence() > after)
                .sorted(Comparator.comparingLong(RpaLogEntry::getSequence))
                .toList();
    }

    public RpaJobResponse confirm(String jobId) {
        RpaJobState state = requireJob(jobId);
        if (!"WAITING_CONFIRMATION".equals(state.status)) {
            throw new RpaException(400, "当前任务不在待确认状态");
        }
        RpaPublisher publisher = publishers.get(state.platform);
        if (publisher == null) {
            throw new RpaException(400, "当前平台不支持自动确认发布");
        }
        state.status = "PUBLISHING";
        state.add("WARN", "收到人工确认，准备自动点击发布");
        executor.submit(() -> confirmJob(state, publisher));
        return state.toResponse("正在确认发布");
    }

    private void runJob(RpaJobState state, RpaPublishRequest request, RpaPublisher publisher) {
        state.status = "RUNNING";
        state.add("INFO", "开始准备小红书草稿");
        try {
            publisher.prepareDraft(state.jobId, request, state::add);
            state.status = "WAITING_CONFIRMATION";
            state.add("WARN", "已停在人工确认步骤，请检查页面后点击确认发布");
        } catch (Exception e) {
            state.status = "FAILED";
            state.add("ERROR", e.getMessage() == null ? "RPA 执行失败" : e.getMessage());
        }
    }

    private void confirmJob(RpaJobState state, RpaPublisher publisher) {
        try {
            publisher.confirm(state.jobId, state::add);
            state.status = "PUBLISHED";
            state.add("SUCCESS", "已提交发布，请以平台页面结果为准");
        } catch (Exception e) {
            state.status = "FAILED";
            state.add("ERROR", e.getMessage() == null ? "自动确认发布失败" : e.getMessage());
        }
    }

    private RpaJobState requireJob(String jobId) {
        RpaJobState state = jobs.get(jobId);
        if (state == null) {
            throw new RpaException(404, "RPA 任务不存在");
        }
        return state;
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return "xhs";
        }
        return platform.trim().toLowerCase(Locale.ROOT);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static class RpaJobState {
        private final String jobId;
        private final String platform;
        private final AtomicLong sequence = new AtomicLong();
        private final List<RpaLogEntry> logs = new CopyOnWriteArrayList<>();
        private volatile String status = "QUEUED";

        private RpaJobState(String jobId, String platform) {
            this.jobId = jobId;
            this.platform = platform;
        }

        private void add(String level, String message) {
            logs.add(new RpaLogEntry(
                    sequence.incrementAndGet(),
                    LocalTime.now().format(TIME_FORMAT),
                    level,
                    message
            ));
        }

        private RpaJobResponse toResponse(String message) {
            String latest = message;
            if (latest == null && !logs.isEmpty()) {
                latest = new ArrayList<>(logs).get(logs.size() - 1).getMessage();
            }
            return new RpaJobResponse(jobId, platform, status, latest);
        }
    }
}

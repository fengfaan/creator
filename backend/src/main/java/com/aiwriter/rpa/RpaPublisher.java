package com.aiwriter.rpa;

import com.aiwriter.model.RpaPublishRequest;

public interface RpaPublisher {
    String platform();

    void prepareDraft(String jobId, RpaPublishRequest request, RpaJobLogger logger) throws Exception;

    default void confirm(String jobId, RpaJobLogger logger) throws Exception {
        throw new RpaException(400, "当前平台不支持自动确认发布");
    }
}

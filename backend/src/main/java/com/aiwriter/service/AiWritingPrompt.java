package com.aiwriter.service;

record AiWritingPrompt(String system, String user, double temperature, int maxTokens) {
}

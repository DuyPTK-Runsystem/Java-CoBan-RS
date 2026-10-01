package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/** Provider-neutral usage. Null is UNKNOWN; an absent cache count must not become a cache miss. */
public record TimetableAgentModelUsage(Long inputTokens, Long outputTokens, Long totalTokens,
        Long cacheReadTokens, Long cacheWriteTokens) {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimetableAgentModelUsage.class);
    private static final String PHASE_TAG = "phase";

    public static TimetableAgentModelUsage unknown() {
        return new TimetableAgentModelUsage(null, null, null, null, null);
    }

    public static TimetableAgentModelUsage from(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return unknown();
        }
        Usage usage = response.getMetadata().getUsage();
        if (usage == null || usage instanceof EmptyUsage) {
            return unknown();
        }
        try {
            Long input = nonNegative(usage.getPromptTokens());
            Long output = nonNegative(usage.getCompletionTokens());
            Long total = input == null || output == null ? null : nonNegative(usage.getTotalTokens());
            return new TimetableAgentModelUsage(input, output, total, nonNegative(usage.getCacheReadInputTokens()),
                    nonNegative(usage.getCacheWriteInputTokens()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            LOGGER.warn("Timetable model usage could not be normalized.");
            return unknown();
        }
    }

    public void record(String phase, String outcome, long elapsedNanos) {
        try {
            recordMeters(phase, outcome, elapsedNanos);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Timetable model metrics could not be recorded.");
        }
    }

    private void recordMeters(String phase, String outcome, long elapsedNanos) {
        Metrics.counter("timetable.agent.model.calls", PHASE_TAG, phase, "outcome", outcome).increment();
        Metrics.timer("timetable.agent.model.latency", PHASE_TAG, phase, "outcome", outcome)
                .record(Math.max(0L, elapsedNanos), TimeUnit.NANOSECONDS);
        recordTokens(phase, "input", inputTokens);
        recordTokens(phase, "output", outputTokens);
        recordTokens(phase, "total", totalTokens);
        recordTokens(phase, "cache_read", cacheReadTokens);
        recordTokens(phase, "cache_write", cacheWriteTokens);
        Metrics.counter("timetable.agent.model.cost", PHASE_TAG, phase, "state", "UNKNOWN").increment();
    }

    private void recordTokens(String phase, String kind, Long value) {
        String state = value == null ? "UNKNOWN" : "KNOWN";
        Metrics.counter("timetable.agent.model.usage", PHASE_TAG, phase, "kind", kind, "state", state).increment();
        if (value != null) {
            Metrics.summary("timetable.agent.model.tokens", PHASE_TAG, phase, "kind", kind).record(value);
        }
    }

    private static Long nonNegative(Number value) {
        return value == null || value.longValue() < 0 ? null : value.longValue();
    }
}

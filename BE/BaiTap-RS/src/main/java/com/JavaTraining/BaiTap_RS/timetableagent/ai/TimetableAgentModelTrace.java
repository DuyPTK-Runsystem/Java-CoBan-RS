package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/** Development-only payload traces at the model boundary. */
final class TimetableAgentModelTrace {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAiTimetableModelGateway.class);

    private TimetableAgentModelTrace() {
    }

    /* default */ static void request(String phase, String system, String user, String schema) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(">>>TimetableAgent [MODEL REQ] phase={} system={} user={} schema={} [{}] [{}]",
                    phase, system, user, schema, Thread.currentThread().getName(), requestId());
        }
    }

    /* default */ static void response(String phase, ChatResponse response) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(">>>TimetableAgent [MODEL RESPONSE} phase={} responsePresent={} generationCount={} [{}] [{}]",
                    phase, response != null, response == null ? 0 : response.getResults().size(),
                    Thread.currentThread().getName(), requestId());
            if (response != null) {
                for (int index = 0; index < response.getResults().size(); index++) {
                    logGeneration(phase, index, response.getResults().get(index));
                }
            }
        }
    }

    private static void logGeneration(String phase, int index, Generation generation) {
        if (LOGGER.isDebugEnabled()) {
            AssistantMessage output = generation == null ? null : generation.getOutput();
            LOGGER.debug(">>>TimetableAgent [MODEL RESPONSE} phase={} generation={} text={} toolCalls={} [{}] [{}]",
                    phase, index, output == null ? null : output.getText(),
                    output == null ? null : output.getToolCalls(), Thread.currentThread().getName(), requestId());
        }
    }

    private static String requestId() {
        String id = MDC.get("requestId");
        return id == null || id.isBlank() ? "N/A" : id;
    }
}

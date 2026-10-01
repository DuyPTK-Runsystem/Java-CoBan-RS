package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentActionPrompt;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentToolCall;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentModelActionService {

    private static final String ACTION_PROMPT = "ai/timetable/action-system.txt";

    private final TimetableAgentProperties properties;
    private final ObjectMapper objectMapper;

    public TimetableAgentToolCall requestSave(TimetableAgentModelGateway gateway, String proposalId,
            Long proposalVersion, Long targetRevisionId, Long expectedVersion) {
        String approvedReference = write(Map.of("proposalId", proposalId,
                "proposalVersion", proposalVersion,
                "targetRevisionId", targetRevisionId,
                "expectedVersion", expectedVersion));
        TimetableAgentActionPrompt prompt = new TimetableAgentActionPrompt(readPrompt(), approvedReference);
        return boundedSaveCall(gateway, prompt);
    }

    private TimetableAgentToolCall boundedSaveCall(TimetableAgentModelGateway gateway,
            TimetableAgentActionPrompt prompt) {
        FutureTask<TimetableAgentToolCall> call = new FutureTask<>(() -> gateway.requestSave(prompt));
        Thread.ofVirtual().name("timetable-agent-save-call").start(call);
        try {
            return call.get(properties.getProviderTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            call.cancel(true);
            throw new TimetableAgentModelException("Save tool call exceeded configured timeout.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            call.cancel(true);
            throw new TimetableAgentModelException("Save tool call was interrupted.", exception);
        } catch (ExecutionException exception) {
            throw new TimetableAgentModelException("Save tool call failed.", exception);
        }
    }

    private String readPrompt() {
        try {
            return new String(new ClassPathResource(ACTION_PROMPT).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Required timetable agent prompt is missing.", exception);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Action request could not be encoded.", exception);
        }
    }
}

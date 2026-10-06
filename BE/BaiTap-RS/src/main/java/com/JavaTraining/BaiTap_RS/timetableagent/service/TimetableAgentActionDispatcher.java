package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.io.IOException;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentToolCall;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TimetableAgentActionDispatcher {

    private static final Set<String> EXPECTED_FIELDS = Set.of(
            "proposalId", "proposalVersion", "targetRevisionId", "expectedVersion");
    private final ObjectMapper strictMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    public void requireAuthorizedSingleCall(TimetableAgentToolCall call, String proposalId,
            long proposalVersion, long targetRevisionId, long expectedVersion) {
        requireSaveCall(call);
        JsonNode arguments = parseArguments(call.argumentsJson());
        requireExactFields(arguments);
        requireExactReference(arguments, proposalId, proposalVersion, targetRevisionId, expectedVersion);
    }

    private void requireSaveCall(TimetableAgentToolCall call) {
        if (call == null || !"saveTimetableDraft".equals(call.name()) || call.callId() == null
                || call.callId().isBlank()) {
            throw denied("The model did not return the single approved save tool call.");
        }
    }

    private JsonNode parseArguments(String input) {
        try {
            return strictMapper.readTree(input);
        } catch (IOException exception) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "The model tool arguments are not valid JSON.", exception);
        }
    }

    private void requireExactFields(JsonNode arguments) {
        if (arguments == null || !arguments.isObject() || arguments.size() != EXPECTED_FIELDS.size()) {
            throw denied("The model tool arguments do not match the approved schema.");
        }
        arguments.fieldNames().forEachRemaining(field -> {
            if (!EXPECTED_FIELDS.contains(field)) {
                throw denied("The model tool arguments contain an unexpected field.");
            }
        });
    }

    private void requireExactReference(JsonNode arguments, String proposalId,
            long proposalVersion, long targetRevisionId, long expectedVersion) {
        if (!arguments.path("proposalId").isTextual()
                || !proposalId.equals(arguments.path("proposalId").textValue())
                || !isExactInteger(arguments.path("proposalVersion"), proposalVersion)
                || !isExactInteger(arguments.path("targetRevisionId"), targetRevisionId)
                || !isExactInteger(arguments.path("expectedVersion"), expectedVersion)) {
            throw denied("The model tool arguments differ from the approved proposal.");
        }
    }

    private boolean isExactInteger(JsonNode value, long expected) {
        return value.isIntegralNumber() && value.canConvertToLong() && value.longValue() == expected;
    }

    private AppException denied(String message) {
        return new AppException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}

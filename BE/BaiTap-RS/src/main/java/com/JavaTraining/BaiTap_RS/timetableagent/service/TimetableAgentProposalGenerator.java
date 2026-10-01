package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.HashSet;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentInvalidOutputException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentProposalPrompt;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public class TimetableAgentProposalGenerator {

    private static final String SYSTEM_PROMPT = "ai/timetable/proposal-system.txt";
    private static final String SCHEMA = "ai/timetable/timetable-proposal.schema.json";
    private final TimetableAgentProperties properties;
    private final TimetableProposalValidator validator;
    private final TimetableAgentPayloadCodec codec;

    public GeneratedProposal generate(TimetableAgentModelGateway gateway, TimetableAgentSnapshot snapshot,
            String initialRequest) {
        int budget = budget();
        long deadline = System.nanoTime() + properties.getProviderTimeout().toNanos();
        String system = TimetableAgentPayloadCodec.resource(SYSTEM_PROMPT);
        String schema = TimetableAgentPayloadCodec.resource(SCHEMA);
        String feedback = "";
        Set<String> seenFailures = new HashSet<>();
        GeneratedProposal lastConflict = null;
        for (int attempt = 0; attempt < budget; attempt++) {
            requireNotCancelled();
            try {
                GeneratedProposal completed = attempt(gateway, snapshot, initialRequest + feedback,
                        system, schema, deadline);
                if (completed.result().status() != TimetableAgentProposalStatus.CONFLICTS) {
                    return completed;
                }
                lastConflict = completed;
                String signature = TimetableAgentPayloadCodec.hash(codec.write(completed.proposal().entries())
                        + "\n" + codec.write(completed.result().issues()));
                if (!seenFailures.add(signature)) {
                    break;
                }
                feedback = feedback(completed);
            } catch (TimetableAgentInvalidOutputException exception) {
                if (!seenFailures.add("INVALID_SCHEMA:" + exception.getMessage())) {
                    break;
                }
                feedback = "\nAPPLICATION_VALIDATION_FEEDBACK (DATA):\n"
                        + "{\"code\":\"INVALID_SCHEMA\",\"path\":\"proposal\","
                        + "\"message\":\"Return one JSON object matching the supplied output schema.\"}";
            }
        }
        if (lastConflict != null) {
            return lastConflict;
        }
        throw new AppException(HttpStatus.BAD_GATEWAY, "The timetable model did not return a valid proposal.");
    }

    private GeneratedProposal attempt(TimetableAgentModelGateway gateway, TimetableAgentSnapshot snapshot,
            String userRequest, String system, String schema, long deadline) {
        TimetableAgentProposalPrompt prompt = new TimetableAgentProposalPrompt(system, snapshot.snapshotJson(),
                userRequest, schema);
        TimetableAgentModelProposalDTO proposal = TimetableAgentProviderCall.call(() -> gateway.propose(prompt),
                deadline);
        return new GeneratedProposal(proposal, validator.validate(snapshot, proposal));
    }

    private int budget() {
        if (properties.getMaxModelCalls() < 1 || properties.getProviderTimeout() == null
                || properties.getProviderTimeout().isZero() || properties.getProviderTimeout().isNegative()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "The timetable model call limits are invalid.");
        }
        return Math.min(properties.getMaxModelCalls(), 3);
    }

    private String feedback(GeneratedProposal completed) {
        return "\nPREVIOUS_CANDIDATE (DATA):\n" + codec.write(completed.proposal())
                + "\nAPPLICATION_VALIDATION_FEEDBACK (DATA):\n" + codec.write(completed.result().issues())
                + "\nReturn a corrected complete proposal for the same captured snapshot.";
    }

    private void requireNotCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new AppException(HttpStatus.REQUEST_TIMEOUT, "The timetable model request was cancelled.");
        }
    }

    public record GeneratedProposal(TimetableAgentModelProposalDTO proposal, TimetableAgentValidationResult result) {
    }
}

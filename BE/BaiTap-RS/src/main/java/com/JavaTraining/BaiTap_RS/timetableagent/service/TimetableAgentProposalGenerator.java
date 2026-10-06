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
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentUnresolvedConstraintDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Slf4j
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
            AttemptResult result = runAttempt(gateway, snapshot, initialRequest + feedback,
                    system, schema, deadline, attempt, budget, seenFailures);
            if (result.completed() != null) {
                GeneratedProposal completed = result.completed();
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
            } else if (result.stop()) {
                break;
            } else {
                feedback = result.feedback();
            }
        }
        if (lastConflict != null) {
            return lastConflict;
        }
        throw new AppException(HttpStatus.BAD_GATEWAY, "The timetable model did not return a valid proposal.");
    }

    private AttemptResult runAttempt(TimetableAgentModelGateway gateway, TimetableAgentSnapshot snapshot,
            String userRequest, String system, String schema, long deadline, int attempt, int budget,
            Set<String> seenFailures) {
        try {
            return new AttemptResult(attempt(gateway, snapshot, userRequest, system, schema, deadline), false, null);
        } catch (TimetableAgentInvalidOutputException exception) {
            return handleInvalidOutput(exception, snapshot, attempt, budget, seenFailures);
        }
    }

    private AttemptResult handleInvalidOutput(TimetableAgentInvalidOutputException exception,
            TimetableAgentSnapshot snapshot, int attempt, int budget, Set<String> seenFailures) {
        int attemptNumber = attempt + 1;
        String errorCategory = exception.getCause() instanceof JsonProcessingException ? "JSON_DECODE"
                : "CONTRACT_VALIDATION";
        if (log.isWarnEnabled()) {
            log.warn("Timetable proposal attempt rejected: snapshotId={} attempt={} maxAttempts={} "
                    + "category={} causeType={}",
                    snapshot.snapshotId(), attemptNumber, budget, errorCategory,
                    exception.getCause() == null ? "NONE" : exception.getCause().getClass().getSimpleName());
        }
        if (!seenFailures.add("INVALID_SCHEMA:" + exception.getMessage())) {
            if (log.isWarnEnabled()) {
                log.warn("Timetable proposal repair stopped: snapshotId={} attempt={} reason=REPEATED_FAILURE",
                        snapshot.snapshotId(), attemptNumber);
            }
            return new AttemptResult(null, true, null);
        }
        if (attemptNumber < budget && log.isInfoEnabled()) {
            log.info("Timetable proposal repair scheduled: snapshotId={} nextAttempt={}",
                    snapshot.snapshotId(), attemptNumber + 1);
        }
        String feedback = "\nAPPLICATION_VALIDATION_FEEDBACK (DATA):\n"
                + "{\"code\":\"INVALID_SCHEMA\",\"path\":\"$\","
                + "\"message\":\"Return exactly one JSON object with these root keys only: "
                + "schemaVersion, status, snapshotId, entries, unresolvedConstraints, explanation. "
                + "Do not add a proposal wrapper. Put validFrom and validTo inside each entry object, "
                + "never at the root. Keep the supplied schema's required keys, arrays, and value types; "
                + "do not add fields. For status NEEDS_INPUT, entries must be an empty array. "
                + "Correct the structure and return the complete proposal for the same snapshot.\"}";
        return new AttemptResult(null, false, feedback);
    }

    private GeneratedProposal attempt(TimetableAgentModelGateway gateway, TimetableAgentSnapshot snapshot,
            String userRequest, String system, String schema, long deadline) {
        TimetableAgentProposalPrompt prompt = new TimetableAgentProposalPrompt(system, snapshot.snapshotJson(),
                userRequest, schema);
        if (log.isInfoEnabled()) {
            log.info("TIMETABLE_AGENT_DEBUG prompt snapshotId={} targetRevisionId={} classCount={} demandCount={} "
                    + "assignmentMetadataCount={} currentEntryCount={} publishedContextCount={} requestLength={}",
                    snapshot.snapshotId(), snapshot.targetRevisionId(), snapshot.classIds().size(),
                    snapshot.demands().size(), snapshot.assignments().size(), snapshot.currentEntries().size(),
                    snapshot.publishedContextEntries().size(), userRequest.length());
        }
        if (log.isDebugEnabled()) {
            String requestId = MDC.get("requestId");
            log.debug(">>>TimetableAgent (model snapshot): snapshotId={} snapshotLength={} snapshotJson={} [{}] [{}]",
                    snapshot.snapshotId(), snapshot.snapshotJson().length(), snapshot.snapshotJson(),
                    Thread.currentThread().getName(), requestId == null || requestId.isBlank() ? "N/A" : requestId);
        }
        TimetableAgentModelProposalDTO proposal = TimetableAgentProviderCall.call(() -> gateway.propose(prompt),
                deadline);
        TimetableAgentValidationResult result = validator.validate(snapshot, proposal);
        if (log.isInfoEnabled()) {
            log.info("TIMETABLE_AGENT_DEBUG response snapshotId={} responseSnapshotId={} modelStatus={} "
                    + "constraintCodes={} validationStatus={} issueCodes={}",
                    snapshot.snapshotId(), proposal.snapshotId(), proposal.status(),
                    proposal.unresolvedConstraints().stream().map(TimetableAgentUnresolvedConstraintDTO::code).toList(),
                    result.status(), result.issues().stream()
                            .map(ResTimetableAgentIssueDTO::code).toList());
        }
        return new GeneratedProposal(proposal, result);
    }

    private int budget() {
        if (properties.getMaxModelCalls() < 1 || properties.getProviderTimeout() == null
                || properties.getProviderTimeout().isZero() || properties.getProviderTimeout().isNegative()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "The timetable model call limits are invalid.");
        }
        return properties.getMaxModelCalls();
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

    private record AttemptResult(GeneratedProposal completed, boolean stop, String feedback) {
    }
}

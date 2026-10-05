package com.JavaTraining.BaiTap_RS.timetableagent.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGatewayResolver;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentProposalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableAgentOrchestrator {

    private final TimetableAgentProperties properties;
    private final TimetableSnapshotService snapshotService;
    private final TimetableProposalValidator validator;
    private final TimetableAgentProposalRepository proposalRepository;
    private final ObjectMapper objectMapper;
    private final TimetableAgentModelGatewayResolver gatewayResolver;
    private final TimetableAgentInputLimits inputLimits;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResTimetableAgentProposalDTO create(Long actorId, ReqCreateTimetableAgentProposalDTO request) {
        requireFeature();
        TimetableAgentModelGateway gateway = gatewayResolver.getIfUnambiguous();
        if (gateway == null || !gateway.supportsNativeStructuredOutput()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No model with native structured output is configured.");
        }
        inputLimits.validateRequest(request);
        TimetableAgentSnapshot snapshot = snapshotService.create(actorId, request);
        if (log.isInfoEnabled()) {
            log.info("TIMETABLE_AGENT_DEBUG snapshot-created snapshotId={} actorId={} targetRevisionId={} classIds={} "
                            + "demandCount={} currentEntryCount={} publishedContextCount={} assignmentMetadataCount={}",
                    snapshot.snapshotId(), actorId, snapshot.targetRevisionId(), snapshot.classIds(),
                    snapshot.demands().size(), snapshot.currentEntries().size(), snapshot.publishedContextEntries().size(),
                    snapshot.assignments().size());
        }
        inputLimits.validateSnapshot(snapshot);
        TimetableAgentPayloadCodec codec = new TimetableAgentPayloadCodec(objectMapper);
        TimetableAgentProposalGenerator.GeneratedProposal completed = new TimetableAgentProposalGenerator(
                properties, validator, codec).generate(gateway, snapshot, userRequest(request));
        inputLimits.validateProposal(completed.proposal());
        TimetableAgentProposal stored = TimetableAgentProposalFactory.create(actorId, snapshot, completed,
                codec, properties.getProposalTtl());
        proposalRepository.saveAndFlush(stored);
        if (log.isInfoEnabled()) {
            log.info("TIMETABLE_AGENT_DEBUG persisted snapshotId={} proposalId={} proposalStatus={} validationStatus={} "
                            + "issueCodes={}",
                    snapshot.snapshotId(), stored.getId(), completed.proposal().status(), completed.result().status(),
                    completed.result().issues().stream().map(ResTimetableAgentIssueDTO::code).toList());
        }
        return response(stored, completed.result());
    }

    @Transactional(readOnly = true)
    public ResTimetableAgentProposalDTO get(Long actorId, Long proposalId) {
        TimetableAgentProposal stored = requireProposal(actorId, proposalId);
        return response(stored, readResult(stored));
    }

    /* package */ TimetableAgentProposal requireProposal(Long actorId, Long proposalId) {
        return proposalRepository.findByIdAndActorId(proposalId, actorId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Proposal was not found."));
    }

    /* package */ TimetableAgentValidationResult readResult(TimetableAgentProposal proposal) {
        return new TimetableAgentPayloadCodec(objectMapper).read(proposal.getValidationJson(),
                TimetableAgentValidationResult.class);
    }

    /* package */ void requireFeature() {
        if (!properties.isEnabled()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "The timetable agent is disabled.");
        }
    }

    private String userRequest(ReqCreateTimetableAgentProposalDTO request) {
        String preference = request.preferences() == null || request.preferences().isBlank()
                ? "" : "\nPreferences: " + request.preferences().trim();
        String requestText = request.userRequest() == null || request.userRequest().isBlank()
                ? "" : "\nRequest: " + request.userRequest().trim();
        return preference + requestText;
    }

    private ResTimetableAgentProposalDTO response(TimetableAgentProposal stored,
            TimetableAgentValidationResult result) {
        return new TimetableAgentProposalMapper(properties, gatewayResolver).response(stored, result);
    }
}

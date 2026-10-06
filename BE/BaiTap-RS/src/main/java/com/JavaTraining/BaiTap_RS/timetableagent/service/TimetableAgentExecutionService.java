package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGatewayResolver;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqExecuteTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentActionStateDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentActionRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentExecutionService {

    private final TimetableAgentOrchestrator orchestrator;
    private final TimetableAgentApprovalRepository approvalRepository;
    private final TimetableAgentActionRepository actionRepository;
    private final TimetableAgentActionReservationService reservationService;
    private final TimetableAgentActionStateService actionStateService;
    private final TimetableAgentOwnedActionService ownedActionService;
    private final TimetableAgentModelGatewayResolver gatewayResolver;

    public ResTimetableAgentActionStateDTO execute(Long actorId, Long proposalId, String key,
            ReqExecuteTimetableAgentProposalDTO request) {
        validateKey(key);
        TimetableAgentProposal proposal = orchestrator.requireProposal(actorId, proposalId);
        requireCurrentVersion(proposal, request.proposalVersion());
        String requestHash = hash(proposalId + ":" + request.proposalVersion() + ":"
                + proposal.getProposalHash() + ":" + proposal.getTargetRevisionId() + ":"
                + proposal.getExpectedVersion());
        ResTimetableAgentActionStateDTO replay = replay(actorId, key, requestHash);
        if (replay != null) {
            return replay;
        }
        orchestrator.requireFeature();
        TimetableAgentModelGateway gateway = requireGateway();
        requireApproval(proposal, actorId, proposalId, request.proposalVersion());
        TimetableAgentActionReservationService.Reservation reservation = reserve(
                actorId, key, requestHash, proposalId, proposal.getTargetRevisionId());
        TimetableAgentAction action = reservation.action();
        if (!reservation.owner()) {
            return actionStateService.state(action);
        }
        return ownedActionService.execute(gateway, proposal, request, action, actorId, proposalId);
    }

    private ResTimetableAgentActionStateDTO replay(Long actorId, String key, String requestHash) {
        TimetableAgentAction prior = actionRepository.findByActorIdAndIdempotencyKey(actorId, key).orElse(null);
        if (prior == null) {
            return null;
        }
        if (!prior.getRequestHash().equals(requestHash)) {
            throw new AppException(HttpStatus.CONFLICT, "Idempotency-Key is already bound to another action.");
        }
        boolean pending = "PENDING".equals(prior.getStatus())
                && prior.getLeaseExpiresAt().isAfter(Instant.now(Clock.systemUTC()));
        boolean terminalFailure = "FAILED".equals(prior.getStatus())
                && !actionStateService.isRetryable(prior.getErrorCode());
        if ("SAVED_DRAFT".equals(prior.getStatus()) || pending || terminalFailure) {
            return actionStateService.state(prior);
        }
        return null;
    }

    private void requireCurrentVersion(TimetableAgentProposal proposal, Long proposalVersion) {
        if (!proposal.getProposalVersion().equals(proposalVersion)) {
            throw new AppException(HttpStatus.CONFLICT, "Proposal version is no longer current.");
        }
    }

    private TimetableAgentModelGateway requireGateway() {
        TimetableAgentModelGateway gateway = gatewayResolver.getIfUnambiguous();
        if (gateway == null || !gateway.supportsSaveToolCalling()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No model with native save-tool calling is configured.");
        }
        return gateway;
    }

    private void requireApproval(TimetableAgentProposal proposal, Long actorId, Long proposalId,
            Long proposalVersion) {
        if (proposal.getStatus() != TimetableAgentProposalStatus.APPROVED
                || !approvalRepository.existsByProposalIdAndProposalVersionAndProposalHashAndActorId(
                        proposalId, proposalVersion, proposal.getProposalHash(), actorId)) {
            throw new AppException(HttpStatus.CONFLICT, "A current matching approval is required.");
        }
        if (!Instant.now(Clock.systemUTC()).isBefore(proposal.getExpiresAt())) {
            throw new AppException(HttpStatus.GONE, "Proposal has expired.");
        }
    }

    private TimetableAgentActionReservationService.Reservation reserve(Long actorId, String key, String requestHash,
            Long proposalId, Long targetRevisionId) {
        try {
            return reservationService.reserve(actorId, key, requestHash, proposalId, targetRevisionId);
        } catch (DataIntegrityViolationException exception) {
            TimetableAgentAction winner = actionRepository.findByActorIdAndIdempotencyKey(actorId, key).orElse(null);
            if (winner != null && requestHash.equals(winner.getRequestHash())) {
                return new TimetableAgentActionReservationService.Reservation(winner, false);
            }
            throw new AppException(HttpStatus.CONFLICT, "Idempotency-Key could not be reserved.", exception);
        } catch (IllegalStateException exception) {
            if ("IDEMPOTENCY_KEY_REUSED".equals(exception.getMessage())) {
                throw new AppException(HttpStatus.CONFLICT,
                        "Idempotency-Key is already bound to another action.", exception);
            }
            throw exception;
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 255) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1 to 255 characters.");
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}

package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqApproveTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentApproval;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableAgentApprovalService {

    private final TimetableAgentOrchestrator orchestrator;
    private final TimetableAgentApprovalRepository approvalRepository;
    private final TimetableAgentDraftRevalidator draftRevalidator;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResTimetableAgentProposalDTO approve(Long actorId, Long proposalId,
            ReqApproveTimetableAgentProposalDTO request) {
        orchestrator.requireFeature();
        TimetableAgentProposal proposal = orchestrator.requireProposal(actorId, proposalId);
        if (Instant.now(Clock.systemUTC()).isAfter(proposal.getExpiresAt())) {
            proposal.setStatus(TimetableAgentProposalStatus.EXPIRED);
            throw new AppException(HttpStatus.GONE, "Proposal has expired.");
        }
        if (proposal.getStatus() != TimetableAgentProposalStatus.READY_FOR_REVIEW
                || !proposal.getProposalVersion().equals(request.proposalVersion())
                || !constantTimeEquals(proposal.getProposalHash(), request.proposalHash())) {
            throw new AppException(HttpStatus.CONFLICT, "Proposal version or hash is no longer approvable.");
        }
        draftRevalidator.validate(proposal);
        approvalRepository.saveAndFlush(new TimetableAgentApproval(proposalId, proposal.getProposalVersion(),
                proposal.getProposalHash(), actorId));
        proposal.setStatus(TimetableAgentProposalStatus.APPROVED);
        return orchestrator.get(actorId, proposalId);
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        if (expected == null || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}

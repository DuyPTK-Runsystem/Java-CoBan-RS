package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Clock;
import java.time.Instant;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentActionStateDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentActionRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentApprovalRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableAgentCommandExecutor {

    private final TimetableAgentActionRepository actionRepository;
    private final TimetableAgentProposalRepository proposalRepository;
    private final TimetableAgentApprovalRepository approvalRepository;
    private final TimetableHeadRepository headRepository;
    private final TimetableRevisionRepository revisionRepository;
    private final TimetableAgentDraftRevalidator draftRevalidator;
    private final TimetableAgentDraftMutationService draftMutationService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResTimetableAgentActionStateDTO save(Long actionId, Long actorId, String leaseToken,
            Long proposalId, Long proposalVersion) {
        TimetableAgentAction action = requireOwnedLease(actionId, actorId, leaseToken, proposalId);
        TimetableAgentProposal proposal = requireApprovedProposal(proposalId, actorId, proposalVersion);
        TimetableRevision revision = lockCurrentDraft(proposal);
        TimetableAgentDraftRevalidator.RevalidatedDraft revalidated = draftRevalidator.validate(proposal);
        return draftMutationService.apply(revision, action, proposal, revalidated.result(),
                revalidated.snapshot(), revalidated.request().validFrom(), revalidated.request().validTo());
    }

    private TimetableAgentAction requireOwnedLease(Long actionId, Long actorId, String leaseToken, Long proposalId) {
        TimetableAgentAction action = actionRepository.findByIdForUpdate(actionId)
                .orElseThrow(() -> notFound("Action was not found."));
        boolean validLease = "PENDING".equals(action.getStatus()) && leaseToken != null
                && leaseToken.equals(action.getLeaseToken())
                && action.getLeaseExpiresAt().isAfter(Instant.now(Clock.systemUTC()));
        boolean ownerMatches = actorId.equals(action.getActorId()) && proposalId.equals(action.getProposalId());
        if (!validLease || !ownerMatches) {
            throw conflict("Action lease is no longer owned by this request.");
        }
        return action;
    }

    private TimetableAgentProposal requireApprovedProposal(Long proposalId, Long actorId, Long proposalVersion) {
        TimetableAgentProposal proposal = proposalRepository.findByIdAndActorId(proposalId, actorId)
                .orElseThrow(() -> notFound("Proposal was not found."));
        boolean approved = proposal.getProposalVersion().equals(proposalVersion)
                && proposal.getStatus() == TimetableAgentProposalStatus.APPROVED
                && proposal.getExpiresAt().isAfter(Instant.now(Clock.systemUTC()))
                && approvalRepository.existsByProposalIdAndProposalVersionAndProposalHashAndActorId(
                        proposalId, proposalVersion, proposal.getProposalHash(), actorId);
        if (!approved) {
            throw conflict("A current matching user approval is required.");
        }
        return proposal;
    }

    private TimetableRevision lockCurrentDraft(TimetableAgentProposal proposal) {
        TimetableHead head = headRepository.findBySemesterId(proposal.getTargetSemesterId())
                .orElseThrow(() -> conflict("Timetable head is unavailable."));
        headRepository.findByIdAndSemesterIdForUpdate(head.getId(), proposal.getTargetSemesterId())
                .orElseThrow(() -> conflict("Timetable head changed during execution."));
        TimetableRevision revision = revisionRepository.findByIdForUpdate(proposal.getTargetRevisionId())
                .orElseThrow(() -> conflict("Target revision is no longer available."));
        if (revision.getStatus() != TimetableRevisionStatus.DRAFT
                || !revision.getVersion().equals(proposal.getExpectedVersion())) {
            throw conflict("The draft changed after proposal approval.");
        }
        return revision;
    }

    private AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}

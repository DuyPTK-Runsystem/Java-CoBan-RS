package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Instant;
import java.util.List;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqApproveTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentCapabilitiesDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentDiffDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentApproval;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalIdentity;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalPayload;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentApprovalRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class TimetableAgentApprovalServiceTest {

    private static final String PROPOSAL_HASH = "hash-1";
    private final TimetableAgentOrchestrator orchestrator = Mockito.mock(TimetableAgentOrchestrator.class);
    private final TimetableAgentApprovalRepository approvalRepository =
            Mockito.mock(TimetableAgentApprovalRepository.class);
    private final TimetableAgentDraftRevalidator draftRevalidator = Mockito.mock(TimetableAgentDraftRevalidator.class);
    private TimetableAgentApprovalService approvalService;
    private TimetableAgentProposal proposal;
    private ResTimetableAgentProposalDTO approvedResponse;

    @BeforeEach
    void setUp() {
        approvalService = new TimetableAgentApprovalService(orchestrator, approvalRepository, draftRevalidator);
        proposal = new TimetableAgentProposal(
                new TimetableAgentProposalIdentity(7L, 77L, 5L, 9L, 2L, "snap-1", "fingerprint", PROPOSAL_HASH),
                new TimetableAgentProposalPayload("{}", "{}", "{}", "{}", "[]", "{}"),
                com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus.READY_FOR_REVIEW,
                Instant.now().plusSeconds(60));
        ReflectionTestUtils.setField(proposal, "id", 31L);
        Mockito.when(orchestrator.requireProposal(7L, 31L)).thenReturn(proposal);
        approvedResponse = new ResTimetableAgentProposalDTO("31", 2L, PROPOSAL_HASH, 77L, 9L,
                TimetableAgentProposalStatus.APPROVED, Instant.now().plusSeconds(60), "snap-1", List.of(),
                List.of(), "approved", new ResTimetableAgentDiffDTO(List.of(), List.of(), List.of()),
                new ResTimetableAgentCapabilitiesDTO(true, true, true));
        Mockito.when(orchestrator.get(7L, 31L)).thenReturn(approvedResponse);
    }

    @Test
    void approveRequiresExactCurrentVersionAndHashThenPersistsActorBoundApproval() {
        ResTimetableAgentProposalDTO result = approvalService.approve(7L, 31L,
                new ReqApproveTimetableAgentProposalDTO(2L, PROPOSAL_HASH));

        checkApprovedResponseAndActorBoundApproval(result);
    }

    @Test
    void approveRejectsWrongVersionOrHashWithoutCreatingApproval() {
        List.of(new ReqApproveTimetableAgentProposalDTO(1L, PROPOSAL_HASH),
                new ReqApproveTimetableAgentProposalDTO(2L, "other-hash"))
                .forEach(this::checkConflictWithoutApproval);
    }

    @Test
    void approveRejectsExpiredProposalAndDoesNotPersistApproval() {
        proposal.setExpiresAt(Instant.now().minusSeconds(1));
        checkExpiredProposalDoesNotCreateApproval();
    }

    @Test
    void approveRejectsDraftThatChangedSinceProposalAndDoesNotPersistApproval() {
        Mockito.doThrow(new AppException(HttpStatus.CONFLICT, "Draft changed after proposal."))
                .when(draftRevalidator).validate(proposal);

        assertStaleDraftApprovalWasRejectedWithoutTransitionOrPersistence();
    }

    private void checkApprovedResponseAndActorBoundApproval(ResTimetableAgentProposalDTO result) {
        Assertions.assertSame(approvedResponse, result, "approval must return the authoritative proposal response");
        Assertions.assertEquals(
                com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus.APPROVED,
                proposal.getStatus(), "the proposal must enter the approved state");
        org.mockito.ArgumentCaptor<TimetableAgentApproval> captor =
                org.mockito.ArgumentCaptor.forClass(TimetableAgentApproval.class);
        Mockito.verify(approvalRepository).saveAndFlush(captor.capture());
        TimetableAgentApproval approval = captor.getValue();
        Assertions.assertTrue(approval.getProposalId() == 31L && approval.getProposalVersion() == 2L
                        && PROPOSAL_HASH.equals(approval.getProposalHash()) && approval.getActorId() == 7L,
                "approval must bind the exact proposal version, hash, and acting user");
    }

    private void checkConflictWithoutApproval(ReqApproveTimetableAgentProposalDTO request) {
        AppException conflict = Assertions.assertThrows(AppException.class,
                () -> approvalService.approve(7L, 31L, request),
                "stale version or hash must block approval");
        Assertions.assertEquals(HttpStatus.CONFLICT, conflict.getStatus(),
                "stale approval bindings must return a conflict");
        Mockito.verify(approvalRepository, Mockito.never()).saveAndFlush(
                ArgumentMatchers.any(TimetableAgentApproval.class));
    }

    private void checkExpiredProposalDoesNotCreateApproval() {
        AppException expired = Assertions.assertThrows(AppException.class,
                () -> approvalService.approve(7L, 31L, new ReqApproveTimetableAgentProposalDTO(2L, PROPOSAL_HASH)),
                "expired proposals cannot be approved");
        Assertions.assertTrue(HttpStatus.GONE.equals(expired.getStatus())
                        && com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus.EXPIRED
                                .equals(proposal.getStatus()),
                "expiration must be visible on the proposal and returned as gone");
        Mockito.verify(approvalRepository, Mockito.never()).saveAndFlush(
                ArgumentMatchers.any(TimetableAgentApproval.class));
    }

    private void assertStaleDraftApprovalWasRejectedWithoutTransitionOrPersistence() {
        AppException staleDraft = Assertions.assertThrows(AppException.class,
                () -> approvalService.approve(7L, 31L,
                        new ReqApproveTimetableAgentProposalDTO(2L, PROPOSAL_HASH)),
                "approval must revalidate the current draft after matching proposal bindings");
        Assertions.assertEquals(HttpStatus.CONFLICT, staleDraft.getStatus(),
                "a stale draft must remain a conflict");
        Assertions.assertEquals(
                com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus.READY_FOR_REVIEW,
                proposal.getStatus(), "stale draft must not advance the proposal");
        Mockito.verify(approvalRepository, Mockito.never()).saveAndFlush(
                ArgumentMatchers.any(TimetableAgentApproval.class));
    }
}


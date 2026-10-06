package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Instant;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentActionRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentApprovalRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentProposalRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TimetableAgentCommandExecutorTest {

    private final TimetableAgentActionRepository actionRepository = Mockito.mock(TimetableAgentActionRepository.class);
    private final TimetableAgentProposalRepository proposalRepository =
            Mockito.mock(TimetableAgentProposalRepository.class);
    private final TimetableAgentApprovalRepository approvalRepository =
            Mockito.mock(TimetableAgentApprovalRepository.class);
    private final TimetableHeadRepository headRepository = Mockito.mock(TimetableHeadRepository.class);
    private final TimetableRevisionRepository revisionRepository = Mockito.mock(TimetableRevisionRepository.class);
    private final TimetableAgentDraftRevalidator draftRevalidator = Mockito.mock(TimetableAgentDraftRevalidator.class);
    private final TimetableAgentDraftMutationService draftMutationService =
            Mockito.mock(TimetableAgentDraftMutationService.class);
    private TimetableAgentCommandExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new TimetableAgentCommandExecutor(actionRepository, proposalRepository, approvalRepository,
                headRepository, revisionRepository, draftRevalidator, draftMutationService);
    }

    @Test
    void saveRejectsSupersededLeaseBeforeAnyDraftWork() {
        TimetableAgentAction action = pendingAction();
        Mockito.when(actionRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(action));

        assertLeaseRejectedBeforeDraftWork("superseded-token", "A superseded lease must conflict.");
    }

    @Test
    void saveRejectsExpiredLeaseBeforeAnyDraftWork() {
        TimetableAgentAction action = pendingAction();
        action.setLeaseExpiresAt(Instant.now().minusSeconds(1));
        Mockito.when(actionRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(action));

        assertLeaseRejectedBeforeDraftWork(action.getLeaseToken(), "An expired lease must conflict.");
    }

    private TimetableAgentAction pendingAction() {
        TimetableAgentAction action = new TimetableAgentAction(31L, 7L, "key-1", "request-hash", null);
        action.setLeaseExpiresAt(Instant.now().plusSeconds(30));
        Mockito.when(actionRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(action));
        return action;
    }

    private void assertNoDraftWork(String reason) {
        Assertions.assertAll(reason,
                () -> Mockito.verifyNoInteractions(proposalRepository),
                () -> Mockito.verifyNoInteractions(approvalRepository),
                () -> Mockito.verifyNoInteractions(headRepository),
                () -> Mockito.verifyNoInteractions(revisionRepository),
                () -> Mockito.verifyNoInteractions(draftRevalidator),
                () -> Mockito.verifyNoInteractions(draftMutationService));
    }

    private void assertLeaseRejectedBeforeDraftWork(String token, String reason) {
        AppException exception = Assertions.assertThrows(AppException.class,
                () -> executor.save(55L, 7L, token, 31L, 2L), reason);
        Assertions.assertEquals(409, exception.getStatus().value(), reason);
        assertNoDraftWork(reason);
    }
}


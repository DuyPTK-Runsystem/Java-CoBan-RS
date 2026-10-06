package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGatewayResolver;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentActionStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqExecuteTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentActionStateDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalIdentity;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalPayload;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentActionRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentApprovalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class TimetableAgentExecutionServiceTest {

    private static final String IDEMPOTENCY_KEY = "same-key";

    private final TimetableAgentOrchestrator orchestrator = Mockito.mock(TimetableAgentOrchestrator.class);
    private final TimetableAgentApprovalRepository approvalRepository =
            Mockito.mock(TimetableAgentApprovalRepository.class);
    private final TimetableAgentActionRepository actionRepository = Mockito.mock(TimetableAgentActionRepository.class);
    private final TimetableAgentActionReservationService reservationService =
            Mockito.mock(TimetableAgentActionReservationService.class);
    private final TimetableAgentActionStateService actionStateService =
            Mockito.mock(TimetableAgentActionStateService.class);
    private final TimetableAgentOwnedActionService ownedActionService =
            Mockito.mock(TimetableAgentOwnedActionService.class);
    private final TimetableAgentModelGatewayResolver gatewayResolver =
            Mockito.mock(TimetableAgentModelGatewayResolver.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private TimetableAgentExecutionService service;
    private TimetableAgentProposal proposal;

    @BeforeEach
    void setUp() {
        service = new TimetableAgentExecutionService(orchestrator, approvalRepository, actionRepository,
                reservationService, actionStateService, ownedActionService, gatewayResolver);
        proposal = new TimetableAgentProposal(
                new TimetableAgentProposalIdentity(8L, 77L, 5L, 9L, 2L, "snap-1", "fingerprint", "proposal-hash"),
                new TimetableAgentProposalPayload("{}", "{}", "{}", "{}", "[]", "{}"),
                TimetableAgentProposalStatus.SAVED, Instant.now().minusSeconds(10));
        ReflectionTestUtils.setField(proposal, "id", 31L);
        Mockito.when(orchestrator.requireProposal(8L, 31L)).thenReturn(proposal);
    }

    @Test
    void executeSameKeySavedReplayReturnsOriginalReceiptBeforeProviderOrApprovalGates() throws Exception {
        ResTimetableAgentActionStateDTO receipt = new ResTimetableAgentActionStateDTO("55", "31", 77L,
                TimetableAgentActionStatus.SAVED_DRAFT, 10L, 4, Instant.parse("2026-10-01T10:00:00Z"),
                null, null, null);
        TimetableAgentAction prior = action("SAVED_DRAFT", requestHash(), objectMapper.writeValueAsString(receipt));
        Mockito.when(actionRepository.findByActorIdAndIdempotencyKey(8L, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(prior));
        Mockito.when(actionStateService.state(prior)).thenReturn(receipt);

        ResTimetableAgentActionStateDTO result = service.execute(8L, 31L, IDEMPOTENCY_KEY,
                new ReqExecuteTimetableAgentProposalDTO(2L));

        checkSavedReceiptReplayedBeforeExternalGates(result, receipt);
    }

    @Test
    void executeSameKeyBoundToDifferentProposalHashReturnsConflictWithoutSaving() throws Exception {
        TimetableAgentAction prior = action("PENDING", "different-request-hash", null);
        Mockito.when(actionRepository.findByActorIdAndIdempotencyKey(8L, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(prior));

        AppException conflict = Assertions.assertThrows(AppException.class,
                () -> service.execute(8L, 31L, IDEMPOTENCY_KEY,
                        new ReqExecuteTimetableAgentProposalDTO(2L)),
                "same key cannot be rebound to another request hash");
        checkRequestHashConflictPreventsExecution(conflict);
    }

    @Test
    void executeTerminalFailureLookupDoesNotExposeRetryCapability() throws Exception {
        TimetableAgentAction prior = action("FAILED", requestHash(), null);
        prior.setErrorCode("STALE");
        Mockito.when(actionRepository.findByActorIdAndIdempotencyKey(8L, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(prior));
        ResTimetableAgentActionStateDTO failed = new ResTimetableAgentActionStateDTO("55", "31", 77L,
                TimetableAgentActionStatus.FAILED, null, null, null, null, "STALE", false);
        Mockito.when(actionStateService.state(prior)).thenReturn(failed);

        ResTimetableAgentActionStateDTO result = service.execute(8L, 31L, IDEMPOTENCY_KEY,
                new ReqExecuteTimetableAgentProposalDTO(2L));

        checkFailedActionIsTerminalWithoutRetry(result);
    }

    @Test
    void executeProviderFailurePersistsRetryableFailureAndReturnsBadGatewayWithoutTimetableWrites()
            throws Exception {
        proposal.setStatus(TimetableAgentProposalStatus.APPROVED);
        proposal.setExpiresAt(Instant.now().plusSeconds(60));
        Mockito.when(actionRepository.findByActorIdAndIdempotencyKey(8L, "new-key"))
                .thenReturn(Optional.empty());
        Mockito.when(approvalRepository.existsByProposalIdAndProposalVersionAndProposalHashAndActorId(
                31L, 2L, "proposal-hash", 8L)).thenReturn(true);
        TimetableAgentAction reserved = action("PENDING", requestHash(), null);
        Mockito.when(reservationService.reserve(8L, "new-key", requestHash(), 31L, 77L))
                .thenReturn(new TimetableAgentActionReservationService.Reservation(reserved, true));
        TimetableAgentModelGateway gateway = Mockito.mock(TimetableAgentModelGateway.class);
        Mockito.when(gateway.supportsSaveToolCalling()).thenReturn(true);
        Mockito.when(gatewayResolver.getIfUnambiguous()).thenReturn(gateway);
        ReqExecuteTimetableAgentProposalDTO executeRequest = new ReqExecuteTimetableAgentProposalDTO(2L);
        Mockito.when(ownedActionService.execute(gateway, proposal, executeRequest, reserved, 8L, 31L))
                .thenThrow(new AppException(HttpStatus.BAD_GATEWAY, "model unavailable"));

        AppException providerFailure = Assertions.assertThrows(AppException.class,
                () -> service.execute(8L, 31L, "new-key", executeRequest),
                "provider errors must return after a durable failed action");
        checkProviderFailureDelegatesToOwnedAction(providerFailure, gateway, executeRequest, reserved);
    }

    private TimetableAgentAction action(String status, String requestHash, String receiptJson) {
        TimetableAgentAction result = new TimetableAgentAction(31L, 8L, IDEMPOTENCY_KEY, requestHash, receiptJson);
        ReflectionTestUtils.setField(result, "id", 55L);
        result.setStatus(status);
        result.setLeaseExpiresAt(Instant.now().plusSeconds(30));
        return result;
    }

    private String requestHash() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest("31:2:proposal-hash:77:9".getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private void checkSavedReceiptReplayedBeforeExternalGates(ResTimetableAgentActionStateDTO result,
            ResTimetableAgentActionStateDTO receipt) {
        Assertions.assertEquals(receipt, result, "same-key replay must return the original saved receipt");
        Mockito.verify(reservationService, Mockito.never()).reserve(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(gatewayResolver, Mockito.never()).getIfUnambiguous();
        Mockito.verify(approvalRepository, Mockito.never())
                .existsByProposalIdAndProposalVersionAndProposalHashAndActorId(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(ownedActionService, Mockito.never()).execute(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    private void checkRequestHashConflictPreventsExecution(AppException conflict) {
        Assertions.assertEquals(HttpStatus.CONFLICT, conflict.getStatus(),
                "an idempotency key cannot be rebound to another action hash");
        Mockito.verify(gatewayResolver, Mockito.never()).getIfUnambiguous();
        Mockito.verify(reservationService, Mockito.never()).reserve(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    private void checkFailedActionIsTerminalWithoutRetry(ResTimetableAgentActionStateDTO result) {
        Assertions.assertTrue(TimetableAgentActionStatus.FAILED.equals(result.status()) && !result.retryable(),
                "non-retryable failed action must remain terminal");
        Mockito.verify(gatewayResolver, Mockito.never()).getIfUnambiguous();
        Mockito.verify(reservationService, Mockito.never()).reserve(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    private void checkProviderFailureDelegatesToOwnedAction(AppException providerFailure,
            TimetableAgentModelGateway gateway, ReqExecuteTimetableAgentProposalDTO executeRequest,
            TimetableAgentAction reserved) {
        Assertions.assertEquals(HttpStatus.BAD_GATEWAY, providerFailure.getStatus(),
                "model failure must return 502");
        Mockito.verify(ownedActionService).execute(gateway, proposal, executeRequest, reserved, 8L, 31L);
        Mockito.verify(actionRepository, Mockito.never()).save(ArgumentMatchers.any());
    }
}


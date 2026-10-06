package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentInvalidOutputException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGatewayResolver;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentIssueSeverity;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqTimetableAgentDemandDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentDiffDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentProposalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class TimetableAgentOrchestratorTest {

    private final TimetableAgentProperties properties = new TimetableAgentProperties();
    private final TimetableSnapshotService snapshotService = Mockito.mock(TimetableSnapshotService.class);
    private final TimetableProposalValidator validator = Mockito.mock(TimetableProposalValidator.class);
    private final TimetableAgentProposalRepository proposalRepository =
            Mockito.mock(TimetableAgentProposalRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final TimetableAgentModelGatewayResolver gatewayResolver =
            Mockito.mock(TimetableAgentModelGatewayResolver.class);
    private TimetableAgentInputLimits inputLimits;
    private final TimetableAgentModelGateway gateway = Mockito.mock(TimetableAgentModelGateway.class);
    private TimetableAgentOrchestrator orchestrator;
    private TimetableAgentSnapshot snapshot;
    private ReqCreateTimetableAgentProposalDTO request;

    @BeforeEach
    void setUp() {
        properties.setMaxClassCount(10);
        properties.setMaxContextCharacters(20_000);
        properties.setMaxRequestCharacters(2_000);
        properties.setMaxPreferencesCharacters(1_000);
        properties.setMaxProposalEntries(100);
        inputLimits = new TimetableAgentInputLimits(properties);
        properties.setEnabled(true);
        properties.setMaxModelCalls(8);
        properties.setProviderTimeout(Duration.ofSeconds(3));
        properties.setProposalTtl(Duration.ofMinutes(10));
        orchestrator = new TimetableAgentOrchestrator(properties, snapshotService, validator,
                proposalRepository, objectMapper, gatewayResolver, inputLimits);
        Mockito.when(gatewayResolver.getIfUnambiguous()).thenReturn(gateway);
        Mockito.when(gateway.supportsNativeStructuredOutput()).thenReturn(true);
        request = new ReqCreateTimetableAgentProposalDTO(77L, 9L, List.of(11L),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31),
                List.of(new ReqTimetableAgentDemandDTO(501L, 1)), List.of(), "", "test");
        snapshot = new TimetableAgentSnapshot("snapshot-1", "fingerprint", "{}", "{}", "{}", 7L,
                77L, 5L, 9L, request.validFrom(), request.validTo(), List.of(11L), request.demands(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
        Mockito.when(snapshotService.create(7L, request)).thenReturn(snapshot);
        Mockito.when(proposalRepository.saveAndFlush(ArgumentMatchers.any(TimetableAgentProposal.class)))
                .thenAnswer(invocation -> {
                    TimetableAgentProposal stored = invocation.getArgument(0);
                    ReflectionTestUtils.setField(stored, "id", 88L);
                    return stored;
                });
    }

    @Test
    void createRepairsSchemaViolationOnceAndPersistsOnlyValidatedModelOutput() throws Exception {
        TimetableAgentModelProposalDTO valid = modelProposal("repaired output");
        Mockito.when(gateway.propose(ArgumentMatchers.any()))
                .thenThrow(new TimetableAgentInvalidOutputException("invalid schema"))
                .thenReturn(valid);
        Mockito.when(validator.validate(snapshot, valid)).thenReturn(readyResult());

        ResTimetableAgentProposalDTO response = orchestrator.create(7L, request);
        checkSchemaRepairPersistedOnlyValidatedResult(response, "repaired output", 2);
    }

    @Test
    void createCapsConflictRepairAttemptsAtThreeAndPersistsFinalBlockedProposal() throws Exception {
        TimetableAgentModelProposalDTO first = modelProposal("candidate 1");
        TimetableAgentModelProposalDTO second = modelProposal("candidate 2");
        TimetableAgentModelProposalDTO third = modelProposal("candidate 3");
        Mockito.when(gateway.propose(ArgumentMatchers.any())).thenReturn(first, second, third);
        Mockito.when(validator.validate(snapshot, first)).thenReturn(conflictResult("conflict 1"));
        Mockito.when(validator.validate(snapshot, second)).thenReturn(conflictResult("conflict 2"));
        Mockito.when(validator.validate(snapshot, third)).thenReturn(conflictResult("conflict 3"));

        ResTimetableAgentProposalDTO response = orchestrator.create(7L, request);
        checkFinalConflictStored(response, "candidate 3", 3);
    }

    @Test
    void createStopsRepeatedIdenticalConflictBeforeExhaustingBudget() {
        TimetableAgentModelProposalDTO unchanged = modelProposal("same proposal");
        Mockito.when(gateway.propose(ArgumentMatchers.any())).thenReturn(unchanged);
        Mockito.when(validator.validate(snapshot, unchanged)).thenReturn(conflictResult("same conflict"));

        orchestrator.create(7L, request);

        checkRepeatedConflictStoppedAtTwoCalls();
    }

    @Test
    void createFailsClosedOnUnconfiguredBoundsBeforeSnapshotOrModel() {
        properties.setMaxContextCharacters(0);

        AppException unavailable = Assertions.assertThrows(AppException.class,
                () -> orchestrator.create(7L, request), "zero bounds must disable proposal generation");

        Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailable.getStatus(),
                "unconfigured bounds must return 503");
        Mockito.verifyNoInteractions(snapshotService);
        Mockito.verify(gateway, Mockito.never()).propose(ArgumentMatchers.any());
        Mockito.verify(proposalRepository, Mockito.never()).saveAndFlush(ArgumentMatchers.any());
    }

    @Test
    void createRejectsOverLimitRequestBeforeSnapshotOrModel() {
        properties.setMaxClassCount(1);
        request = new ReqCreateTimetableAgentProposalDTO(77L, 9L, List.of(11L, 12L),
                request.validFrom(), request.validTo(), request.demands(), request.lockedEntryIds(),
                request.preferences(), request.userRequest());

        AppException tooLarge = Assertions.assertThrows(AppException.class,
                () -> orchestrator.create(7L, request), "requests exceeding configured scope must be rejected");

        Assertions.assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, tooLarge.getStatus(),
                "over-limit requests must return 413");
        Mockito.verifyNoInteractions(snapshotService);
        Mockito.verify(gateway, Mockito.never()).propose(ArgumentMatchers.any());
        Mockito.verify(proposalRepository, Mockito.never()).saveAndFlush(ArgumentMatchers.any());
    }

    @Test
    void createTimesOutProviderAndDoesNotPersistProposal() {
        properties.setProviderTimeout(Duration.ofMillis(20));
        Mockito.when(gateway.propose(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Thread.sleep(500);
            return modelProposal("too late");
        });

        checkTimeoutStopsWithoutPersistingProposal();
    }

    private void checkSchemaRepairPersistedOnlyValidatedResult(ResTimetableAgentProposalDTO response,
            String explanation, int callCount) throws Exception {
        Assertions.assertEquals(TimetableAgentProposalStatus.READY_FOR_REVIEW, response.status(),
                "a schema repair must persist only after validation succeeds");
        org.mockito.ArgumentCaptor<TimetableAgentProposal> saved =
                org.mockito.ArgumentCaptor.forClass(TimetableAgentProposal.class);
        Mockito.verify(proposalRepository).saveAndFlush(saved.capture());
        TimetableAgentModelProposalDTO persisted = objectMapper.readValue(saved.getValue().getProposalJson(),
                TimetableAgentModelProposalDTO.class);
        Assertions.assertEquals(explanation, persisted.explanation(),
                "the corrected provider response must be the saved model proposal");
        Mockito.verify(gateway, Mockito.times(callCount)).propose(ArgumentMatchers.any());
    }

    private void checkFinalConflictStored(ResTimetableAgentProposalDTO response,
            String explanation, int callCount) throws Exception {
        Assertions.assertEquals(TimetableAgentProposalStatus.CONFLICTS, response.status(),
                "the final unresolved conflict must remain visible for review");
        Mockito.verify(gateway, Mockito.times(callCount)).propose(ArgumentMatchers.any());
        org.mockito.ArgumentCaptor<TimetableAgentProposal> saved =
                org.mockito.ArgumentCaptor.forClass(TimetableAgentProposal.class);
        Mockito.verify(proposalRepository).saveAndFlush(saved.capture());
        TimetableAgentModelProposalDTO persisted = objectMapper.readValue(saved.getValue().getProposalJson(),
                TimetableAgentModelProposalDTO.class);
        Assertions.assertEquals(explanation, persisted.explanation(),
                "the stored conflict must contain the final candidate after bounded repair");
    }

    private void checkRepeatedConflictStoppedAtTwoCalls() {
        Mockito.verify(gateway, Mockito.times(2)).propose(ArgumentMatchers.any());
        Mockito.verify(proposalRepository).saveAndFlush(ArgumentMatchers.any(TimetableAgentProposal.class));
    }

    private void checkTimeoutStopsWithoutPersistingProposal() {
        AppException timeout = Assertions.assertThrows(AppException.class,
                () -> orchestrator.create(7L, request), "provider deadline must bound proposal generation");
        Assertions.assertEquals(HttpStatus.GATEWAY_TIMEOUT, timeout.getStatus(),
                "provider deadline must map to 504");
        Mockito.verify(proposalRepository, Mockito.never()).saveAndFlush(
                ArgumentMatchers.any(TimetableAgentProposal.class));
    }

    private TimetableAgentModelProposalDTO modelProposal(String explanation) {
        return new TimetableAgentModelProposalDTO("1", TimetableAgentModelProposalStatus.PROPOSED,
                snapshot.snapshotId(), List.of(), List.of(), explanation);
    }

    private TimetableAgentValidationResult readyResult() {
        return new TimetableAgentValidationResult(TimetableAgentProposalStatus.READY_FOR_REVIEW,
                List.of(), List.of(), new ResTimetableAgentDiffDTO(List.of(), List.of(), List.of()), "ready");
    }

    private TimetableAgentValidationResult conflictResult(String message) {
        return new TimetableAgentValidationResult(TimetableAgentProposalStatus.CONFLICTS, List.of(),
                List.of(new ResTimetableAgentIssueDTO("TEST_CONFLICT", TimetableAgentIssueSeverity.BLOCKING,
                        "entries", message)), new ResTimetableAgentDiffDTO(List.of(), List.of(), List.of()), message);
    }
}


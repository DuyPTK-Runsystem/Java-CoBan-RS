package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqTimetableAgentDemandDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class TimetableAgentInputLimitsTest {

    private final TimetableAgentProperties properties = new TimetableAgentProperties();
    private TimetableAgentInputLimits limits;

    @BeforeEach
    void setUp() {
        properties.setMaxClassCount(2);
        properties.setMaxContextCharacters(100);
        properties.setMaxRequestCharacters(20);
        properties.setMaxPreferencesCharacters(10);
        properties.setMaxProposalEntries(2);
        limits = new TimetableAgentInputLimits(properties);
    }

    @Test
    void requireConfiguredFailsClosedWhenBoundsAreUnset() {
        TimetableAgentProperties unset = new TimetableAgentProperties();
        TimetableAgentInputLimits unsetLimits = new TimetableAgentInputLimits(unset);
        Assertions.assertThrows(AppException.class, unsetLimits::requireConfigured,
                "zero defaults must keep the feature disabled until an operator configures bounds");
    }

    @Test
    void validateRequestAcceptsExactConfiguredBoundaries() {
        ReqCreateTimetableAgentProposalDTO request = request(List.of(1L, 2L),
                List.of(new ReqTimetableAgentDemandDTO(11L, 1), new ReqTimetableAgentDemandDTO(12L, 1)),
                List.of(21L, 22L), "1234567890", "abcdefghij");
        limits.validateRequest(request);
    }

    @Test
    void validateRequestRejectsTooManyClassesOrAssignments() {
        assertPayloadTooLarge(() -> limits.validateRequest(request(List.of(1L, 2L, 3L), List.of(), List.of(), "", "")));
        assertPayloadTooLarge(() -> limits.validateRequest(request(List.of(1L),
                List.of(new ReqTimetableAgentDemandDTO(11L, 1), new ReqTimetableAgentDemandDTO(12L, 1),
                        new ReqTimetableAgentDemandDTO(13L, 1)), List.of(), "", "")));
    }

    @Test
    void validateRequestRejectsTooManyLockedEntries() {
        assertPayloadTooLarge(() -> limits.validateRequest(request(List.of(1L), List.of(),
                List.of(21L, 22L, 23L), "", "")));
    }

    @Test
    void validateRequestRejectsOversizedUserTextOrPreferences() {
        assertPayloadTooLarge(() -> limits.validateRequest(request(List.of(1L), List.of(), List.of(),
                "123456789012345678901", "")));
        assertPayloadTooLarge(() -> limits.validateRequest(request(List.of(1L), List.of(), List.of(), "",
                "12345678901")));
    }

    @Test
    void validateSnapshotRejectsOversizedSerializedContext() {
        TimetableAgentSnapshot snapshot = snapshot("x".repeat(101));
        assertPayloadTooLarge(() -> limits.validateSnapshot(snapshot));
    }

    @Test
    void validateProposalRejectsMoreEntriesThanConfigured() {
        TimetableAgentModelProposalDTO proposal = new TimetableAgentModelProposalDTO("1",
                TimetableAgentModelProposalStatus.PROPOSED, "snapshot-1",
                List.of(entry(1L), entry(2L), entry(3L)), List.of(), "proposal");
        Assertions.assertThrows(AppException.class, () -> limits.validateProposal(proposal),
                "excess model output must be rejected before proposal persistence");
    }

    private ReqCreateTimetableAgentProposalDTO request(List<Long> classIds,
            List<ReqTimetableAgentDemandDTO> demands, List<Long> lockedIds, String userRequest, String preferences) {
        return new ReqCreateTimetableAgentProposalDTO(77L, 9L, classIds, LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 12, 31), demands, lockedIds, preferences, userRequest);
    }

    private TimetableAgentSnapshot snapshot(String json) {
        LocalDate from = LocalDate.of(2026, 10, 5);
        LocalDate to = LocalDate.of(2026, 12, 31);
        return new TimetableAgentSnapshot("snapshot-1", "fingerprint", "{}", json, "{}", 7L, 77L, 5L,
                9L, from, to, List.of(11L), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), Map.of());
    }

    private TimetableAgentProposalEntryDTO entry(Long assignmentId) {
        return new TimetableAgentProposalEntryDTO(assignmentId, 21L, null,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31));
    }

    private void assertPayloadTooLarge(org.junit.jupiter.api.function.Executable operation) {
        AppException exception = Assertions.assertThrows(AppException.class, operation,
                "over-limit input must be rejected before calling the provider");
        Assertions.assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exception.getStatus(),
                "configured request and context bounds must map to HTTP 413");
    }
}

package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqTimetableAgentDemandDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalIdentity;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalPayload;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class TimetableAgentDraftRevalidatorTest {

    private final TimetableSnapshotService snapshotService = Mockito.mock(TimetableSnapshotService.class);
    private final TimetableProposalValidator validator = Mockito.mock(TimetableProposalValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final TimetableAgentDraftRevalidator revalidator =
            new TimetableAgentDraftRevalidator(snapshotService, validator, objectMapper);

    @Test
    void changedSnapshotFingerprintRejectsBeforeProposalValidation() throws Exception {
        ReqCreateTimetableAgentProposalDTO request = request();
        TimetableAgentModelProposalDTO model = new TimetableAgentModelProposalDTO(
                "1", TimetableAgentModelProposalStatus.PROPOSED, "snapshot-old", List.of(), List.of(), "draft");
        TimetableAgentProposal proposal = proposal(objectMapper.writeValueAsString(request),
                objectMapper.writeValueAsString(model));
        Mockito.when(snapshotService.create(7L, request)).thenReturn(snapshot("changed-fingerprint"));

        AppException exception = Assertions.assertThrows(AppException.class, () -> revalidator.validate(proposal));

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus(),
                "Changed source inputs invalidate an approved draft.");
        Mockito.verifyNoInteractions(validator);
    }

    private TimetableAgentProposal proposal(String requestJson, String proposalJson) {
        TimetableAgentProposal proposal = new TimetableAgentProposal(
                new TimetableAgentProposalIdentity(7L, 77L, 5L, 9L, 2L, "snap-1",
                        "approved-fingerprint", "proposal-hash"),
                new TimetableAgentProposalPayload("{}", requestJson, "{}", proposalJson, "[]", "{}"),
                TimetableAgentProposalStatus.APPROVED, Instant.now().plusSeconds(60));
        proposal.setSnapshotFingerprint("approved-fingerprint");
        return proposal;
    }

    private ReqCreateTimetableAgentProposalDTO request() {
        return new ReqCreateTimetableAgentProposalDTO(77L, 9L, List.of(11L), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 12, 31), List.of(new ReqTimetableAgentDemandDTO(501L, 1)), List.of(), "", "test");
    }

    private TimetableAgentSnapshot snapshot(String fingerprint) {
        return new TimetableAgentSnapshot("fresh-snapshot", fingerprint, "{}", "{}", "{}", 7L, 77L, 5L,
                9L, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31), List.of(11L),
                List.of(new ReqTimetableAgentDemandDTO(501L, 1)), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
    }
}


package com.JavaTraining.BaiTap_RS.timetableagent.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentDraftRevalidator {

    private final TimetableSnapshotService snapshotService;
    private final TimetableProposalValidator validator;
    private final ObjectMapper objectMapper;

    public RevalidatedDraft validate(TimetableAgentProposal proposal) {
        ReqCreateTimetableAgentProposalDTO request = read(proposal.getRequestJson(),
                ReqCreateTimetableAgentProposalDTO.class);
        TimetableAgentSnapshot currentSnapshot = snapshotService.create(proposal.getActorId(), request);
        if (!proposal.getSnapshotFingerprint().equals(currentSnapshot.sourceFingerprint())) {
            throw conflict("The timetable inputs changed after proposal approval.");
        }
        TimetableAgentSnapshot stableSnapshot = withSnapshotId(currentSnapshot, proposal.getSnapshotId());
        TimetableAgentValidationResult result = validator.validate(stableSnapshot,
                read(proposal.getProposalJson(), TimetableAgentModelProposalDTO.class));
        if (result.status() != TimetableAgentProposalStatus.READY_FOR_REVIEW) {
            throw conflict("The approved proposal is no longer valid for this draft.");
        }
        return new RevalidatedDraft(stableSnapshot, request, result);
    }

    private TimetableAgentSnapshot withSnapshotId(TimetableAgentSnapshot snapshot, String stableSnapshotId) {
        return new TimetableAgentSnapshot(stableSnapshotId, snapshot.sourceFingerprint(), snapshot.sourceJson(),
                snapshot.snapshotJson(), snapshot.requestJson(), snapshot.actorId(), snapshot.targetRevisionId(),
                snapshot.semesterId(), snapshot.expectedVersion(), snapshot.validFrom(), snapshot.validTo(),
                snapshot.classIds(), snapshot.demands(), snapshot.lockedEntryIds(), snapshot.currentEntries(),
                snapshot.publishedContextEntries(), snapshot.assignments(), snapshot.periods(), snapshot.rooms(),
                snapshot.approvedAvailability(), snapshot.closedDates(), snapshot.subjectRoomIds());
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored agent data could not be read.", exception);
        }
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }

    public record RevalidatedDraft(TimetableAgentSnapshot snapshot, ReqCreateTimetableAgentProposalDTO request,
            TimetableAgentValidationResult result) {
    }
}

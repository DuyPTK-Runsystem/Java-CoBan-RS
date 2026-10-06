package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response;

import java.time.Instant;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;

public record ResTimetableAgentProposalDTO(
        String proposalId,
        long proposalVersion,
        String proposalHash,
        Long targetRevisionId,
        Long expectedVersion,
        TimetableAgentProposalStatus status,
        Instant expiresAt,
        String snapshotId,
        List<TimetableAgentProposalEntryDTO> entries,
        List<ResTimetableAgentIssueDTO> issues,
        String explanation,
        ResTimetableAgentDiffDTO diff,
        ResTimetableAgentCapabilitiesDTO capabilities) {
}

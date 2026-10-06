package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response;

import java.util.List;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;

public record ResTimetableAgentDiffDTO(
        List<TimetableAgentProposalEntryDTO> added,
        List<ResTimetableAgentExistingEntryDTO> removed,
        List<ResTimetableAgentExistingEntryDTO> unchanged) {
}

package com.JavaTraining.BaiTap_RS.timetableagent.domain;

import java.util.List;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentDiffDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;

public record TimetableAgentValidationResult(
        TimetableAgentProposalStatus status,
        List<TimetableAgentProposalEntryDTO> entries,
        List<ResTimetableAgentIssueDTO> issues,
        ResTimetableAgentDiffDTO diff,
        String explanation) {

    public boolean readyForReview() {
        return status == TimetableAgentProposalStatus.READY_FOR_REVIEW;
    }
}

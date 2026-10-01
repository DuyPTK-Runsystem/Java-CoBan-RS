package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentIssueSeverity;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentDiffDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;

public final class TimetableAgentValidationIssues {

    private TimetableAgentValidationIssues() {
    }

    public static ResTimetableAgentIssueDTO blocking(String code, String path, String message) {
        return new ResTimetableAgentIssueDTO(code, TimetableAgentIssueSeverity.BLOCKING, path, message);
    }

    public static ResTimetableAgentDiffDTO emptyDiff() {
        return new ResTimetableAgentDiffDTO(List.of(), List.of(), List.of());
    }

    public static TimetableAgentValidationResult terminal(TimetableAgentModelProposalDTO proposal,
            TimetableAgentProposalStatus status, TimetableAgentIssueSeverity severity) {
        return new TimetableAgentValidationResult(status, List.of(), proposal.unresolvedConstraints().stream()
                .map(item -> new ResTimetableAgentIssueDTO(item.code(), severity, item.field(), item.message()))
                .toList(), emptyDiff(), proposal.explanation());
    }
}

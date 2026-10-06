package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentIssueSeverity;

public record ResTimetableAgentIssueDTO(
        String code,
        TimetableAgentIssueSeverity severity,
        String path,
        String message) {
}

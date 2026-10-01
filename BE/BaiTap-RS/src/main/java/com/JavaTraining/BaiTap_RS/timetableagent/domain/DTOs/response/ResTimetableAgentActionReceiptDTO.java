package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response;

import java.time.Instant;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentActionStatus;

public record ResTimetableAgentActionReceiptDTO(
        String actionId,
        String proposalId,
        Long targetRevisionId,
        Long newVersion,
        int savedEntryCount,
        Instant committedAt,
        TimetableAgentActionStatus status) {
}

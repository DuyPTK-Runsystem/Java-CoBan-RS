package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response;

import java.time.Instant;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentActionStatus;

public record ResTimetableAgentActionStateDTO(
        String actionId,
        String proposalId,
        Long targetRevisionId,
        TimetableAgentActionStatus status,
        Long newVersion,
        Integer savedEntryCount,
        Instant committedAt,
        Instant leaseExpiresAt,
        String errorCode,
        Boolean retryable) {
}

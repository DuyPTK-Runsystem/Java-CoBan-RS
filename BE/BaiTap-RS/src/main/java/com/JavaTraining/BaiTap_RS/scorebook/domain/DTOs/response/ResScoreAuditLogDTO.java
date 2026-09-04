package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

public record ResScoreAuditLogDTO(
        Long auditLogId,
        Long actorUserId,
        String actorUsername,
        String action,
        String entityType,
        String entityId,
        JsonNode beforeData,
        JsonNode afterData,
        String requestId,
        String ipAddress,
        LocalDateTime occurredAt) {
}

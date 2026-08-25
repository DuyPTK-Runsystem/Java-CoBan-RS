package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterLockReportStatus;

public record ResSemesterCompletenessReportDTO(
                Long reportId,
                Long runId,
                Long semesterId,
                String checkpointCode,
                SemesterLockReportStatus reportStatus,
                LocalDateTime evaluatedAt,
                String scopeType,
                SemesterCompletenessSummaryDTO summary,
                String failureReason,
                String correlationId) {
}

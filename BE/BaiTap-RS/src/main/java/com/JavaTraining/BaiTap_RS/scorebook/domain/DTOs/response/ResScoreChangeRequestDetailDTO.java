package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequestStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreSnapshotStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;

public record ResScoreChangeRequestDetailDTO(
        Long requestId,
        Long assessmentColumnId,
        Long studentId,
        Long studentScoreId,
        ScoreSnapshotStatus beforeStatus,
        BigDecimal beforeValue,
        ScoreStatus proposedStatus,
        BigDecimal proposedValue,
        String reason,
        Long requestedBy,
        LocalDateTime requestedAt,
        ScoreChangeRequestStatus status,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String rejectionReason,
        LocalDateTime appliedAt) {
}

package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequestStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;

public record ResScoreChangeRequestDTO(
        Long requestId,
        Long assessmentColumnId,
        Long studentId,
        String studentCode,
        String studentName,
        ScoreStatus proposedStatus,
        BigDecimal proposedValue,
        Long requestedBy,
        LocalDateTime requestedAt,
        ScoreChangeRequestStatus status,
        Long reviewedBy,
        LocalDateTime reviewedAt) {
}

package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;

public record ResBulkScoreFileRowDTO(
        int rowNumber,
        String studentCode,
        Long studentId,
        String studentName,
        ScoreStatus oldStatus,
        BigDecimal oldValue,
        Long oldVersion,
        ScoreStatus newStatus,
        BigDecimal newValue,
        String note,
        String result,
        String errorCode,
        String message) {
}

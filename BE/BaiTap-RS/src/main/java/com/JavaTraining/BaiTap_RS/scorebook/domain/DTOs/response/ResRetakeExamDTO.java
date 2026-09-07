package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExamStatus;

public record ResRetakeExamDTO(
        Long retakeId,
        Long studentId,
        Long academicYearId,
        Long subjectId,
        BigDecimal preRetakeScore,
        BigDecimal retakeScore,
        LocalDate examDate,
        RetakeExamStatus status,
        String note,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

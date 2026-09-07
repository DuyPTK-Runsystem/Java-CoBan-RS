package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;

public record ResTranscriptCalculationStatusDTO(
        Long studentId,
        String studentCode,
        Long academicYearId,
        Long semesterId,
        CalculationStatus calculationStatus,
        Long sourceVersion,
        Long calculatedVersion,
        boolean isUpToDate,
        LocalDateTime calculatedAt,
        String lastError) {
}

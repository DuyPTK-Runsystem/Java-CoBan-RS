package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;

public record ResSemesterDTO(
        Long id,
        Long academicYearId,
        String code,
        String name,
        Integer displayOrder,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime automaticLockAt,
        SemesterStatus status,
        LocalDateTime lockedAt,
        Long lockedBy,
        String lockReason,
        LocalDateTime reopenUntil) {
}

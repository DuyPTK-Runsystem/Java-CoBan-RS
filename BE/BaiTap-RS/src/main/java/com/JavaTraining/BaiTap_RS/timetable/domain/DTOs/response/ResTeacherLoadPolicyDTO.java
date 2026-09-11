package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicyStatus;

public record ResTeacherLoadPolicyDTO(
                Long id,
                String version,
                String source,
                LocalDate effectiveFrom,
                LocalDate effectiveTo,
                int basePeriods,
                int homeroomReduction,
                int nursingReduction,
                TeacherLoadPolicyStatus status,
                Long versionLock,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}

package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadEligibilityStatus;

public record ResTeacherLoadEligibilityDTO(
                Long id,
                Long teacherId,
                String teacherName,
                String ruleCode,
                LocalDate validFrom,
                LocalDate validTo,
                String evidenceReference,
                TeacherLoadEligibilityStatus status,
                Long version,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}

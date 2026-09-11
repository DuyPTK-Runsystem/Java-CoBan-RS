package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailabilityStatus;

public record ResTeacherUnavailabilityDTO(
                Long id,
                Long teacherId,
                String teacherName,
                Long semesterId,
                Integer dayOfWeek,
                LocalDate specificDate,
                LocalDate validFrom,
                LocalDate validTo,
                SessionType session,
                String periodIndexes,
                String note,
                TeacherUnavailabilityStatus status,
                String decisionReason,
                Long decidedBy,
                LocalDateTime decidedAt,
                Long version,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}

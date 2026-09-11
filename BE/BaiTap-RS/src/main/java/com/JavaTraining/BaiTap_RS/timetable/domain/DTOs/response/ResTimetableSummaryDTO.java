package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;

public record ResTimetableSummaryDTO(
                Long timetableId,
                Long semesterId,
                String semesterName,
                Long currentRevisionId,
                Integer revisionNumber,
                TimetableRevisionStatus status,
                LocalDate effectiveFrom,
                LocalDate effectiveTo,
                Long version,
                Long headVersion) {
}

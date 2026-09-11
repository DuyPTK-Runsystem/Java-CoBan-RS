package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;

public record ResTimetableDetailDTO(
                Long timetableId,
                Long semesterId,
                String semesterName,
                Long revisionId,
                Integer revisionNumber,
                TimetableRevisionStatus status,
                LocalDate effectiveFrom,
                LocalDate effectiveTo,
                Long policyId,
                String policyVersion,
                Long version,
                Long headVersion,
                Integer blockingCount,
                Integer warningCount,
                List<String> capabilities) {
}

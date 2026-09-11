package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;

public record ResTimetableReviewDTO(
                Long revisionId,
                Long revisionVersion,
                LocalDateTime validatedAt,
                TimetableRevisionStatus status,
                int blockingCount,
                int warningCount,
                List<ResTimetableIssueDTO> issues,
                List<ResTeacherLoadDTO> teacherLoads) {
}

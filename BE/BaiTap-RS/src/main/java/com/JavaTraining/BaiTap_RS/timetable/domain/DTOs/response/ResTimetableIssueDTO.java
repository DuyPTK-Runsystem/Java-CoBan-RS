package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDate;
import java.util.List;

public record ResTimetableIssueDTO(
        String code,
        String severity,
        String message,
        List<Long> entryIds,
        Long periodId,
        LocalDate date,
        String className,
        String teacherName,
        String roomName
) {}

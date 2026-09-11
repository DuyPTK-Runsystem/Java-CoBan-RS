package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDate;

public record ResTeacherLoadDTO(
        Long teacherId,
        String teacherName,
        LocalDate weekStart,
        int assignedPeriods,
        int basePeriods,
        int reductions,
        int targetPeriods,
        int difference,
        String policyVersion,
        String evaluationStatus
) {}

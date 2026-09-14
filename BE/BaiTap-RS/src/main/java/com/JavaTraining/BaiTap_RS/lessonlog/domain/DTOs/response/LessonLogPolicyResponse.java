package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response;

import java.time.LocalDate;

public record LessonLogPolicyResponse(Long policyId, Integer policyVersion, Long version,
        LocalDate effectiveFrom, String timezone, String deadlineMode, Integer editWindowHours,
        boolean requireHomeroomReview, String rubric) { }

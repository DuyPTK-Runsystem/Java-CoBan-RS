package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response;

import java.time.LocalDateTime;

public record LessonLogRevisionResponse(Long revisionId, Long entryId, Long weeklyReviewId,
        Long policyId, String action, Long actorId, String reason, String beforeStateJson,
        String afterStateJson, String correlationId, LocalDateTime createdAt) { }

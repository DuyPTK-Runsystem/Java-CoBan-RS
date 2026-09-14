package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.WeeklyReviewStatus;

public record WeeklyReviewResponse(Long reviewId, Long classId, Long semesterId, LocalDate weekStart,
        LocalDate weekEnd, WeeklyReviewStatus status, String weeklyComment, String weeklyGrade,
        Long version, LocalDateTime signedAt, Long signedBy, boolean canSignWeek, String blockedReason,
        String signedSnapshotJson) { }

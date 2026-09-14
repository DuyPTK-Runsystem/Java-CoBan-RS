package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response;

import java.time.LocalDate;
import java.util.List;

public record LessonLogClassWeekResponse(Long classId, Long semesterId, LocalDate weekStart,
        LocalDate weekEnd, List<LessonLogEntryResponse> items, WeeklyReviewResponse weeklyReview) { }

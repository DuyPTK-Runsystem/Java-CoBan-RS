package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response;

import java.time.LocalDate;
import java.util.List;

public record LessonLogScheduleResponse(LocalDate date, String timezone, List<LessonLogScheduleItemResponse> items) { }

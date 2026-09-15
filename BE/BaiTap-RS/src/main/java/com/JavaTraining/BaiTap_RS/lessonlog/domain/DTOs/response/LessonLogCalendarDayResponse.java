package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response;

import java.time.LocalDate;

public record LessonLogCalendarDayResponse(LocalDate date, String label, String kind) { }

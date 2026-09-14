package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReqWeeklyReviewDTO(@NotNull Long semesterId, @NotNull LocalDate weekStart,
        @NotNull @PositiveOrZero Long expectedVersion, List<ExpectedEntry> expectedEntries,
        String weeklyComment, String weeklyGrade, String reason) {
    public record ExpectedEntry(@NotNull Long entryId, @NotNull @PositiveOrZero Long version) { }
}

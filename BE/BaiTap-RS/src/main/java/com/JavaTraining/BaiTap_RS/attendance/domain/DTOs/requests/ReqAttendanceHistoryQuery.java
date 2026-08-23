package com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record ReqAttendanceHistoryQuery(
        @Positive Long academicYearId,
        @Positive Long semesterId,
        LocalDate from,
        LocalDate to,
        @Min(0) Integer page,
        @Positive Integer size) {

    public int resolvedPage() {
        return page == null ? 0 : page;
    }

    public int resolvedSize() {
        return size == null ? 10 : size;
    }
}

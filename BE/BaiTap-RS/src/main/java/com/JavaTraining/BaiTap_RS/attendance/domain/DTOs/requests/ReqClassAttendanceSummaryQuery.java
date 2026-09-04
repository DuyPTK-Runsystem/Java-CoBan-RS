package com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

public record ReqClassAttendanceSummaryQuery(
        @NotNull(message = "Học kỳ không được để trống")
        @Positive(message = "ID học kỳ phải là số dương")
        Long semesterId,

        @NotNull(message = "Từ ngày không được để trống")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @NotNull(message = "Đến ngày không được để trống")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,

        @Min(0)
        Integer page,

        @Positive
        Integer size) {

    public int resolvedPage() {
        return page == null ? 0 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : size;
    }
}

package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import jakarta.validation.constraints.NotNull;

public record ReqPublishTimetableDTO(
        @NotNull(message = "expectedVersion không được để trống")
        Long expectedVersion,

        Long expectedHeadVersion
) {}

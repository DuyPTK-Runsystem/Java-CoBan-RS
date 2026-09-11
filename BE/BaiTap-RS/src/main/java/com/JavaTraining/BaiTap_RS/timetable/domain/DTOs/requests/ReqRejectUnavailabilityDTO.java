package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReqRejectUnavailabilityDTO(
        @NotNull(message = "expectedVersion không được để trống")
        Long expectedVersion,

        @NotBlank(message = "Lý do từ chối không được để trống")
        String reason
) {}

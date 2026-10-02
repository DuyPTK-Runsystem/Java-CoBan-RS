package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqConfirmTimetableTeacherLoadPolicyDTO(
        @NotNull(message = "policyId không được để trống")
        @Positive(message = "policyId phải lớn hơn 0")
        Long policyId,

        @NotNull(message = "expectedVersion không được để trống")
        Long expectedVersion) {}

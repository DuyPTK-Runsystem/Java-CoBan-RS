package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReqCreatePolicyDTO(
                @NotBlank(message = "Phiên bản không được để trống") String version,

                @NotBlank(message = "Nguồn / căn cứ văn bản không được để trống") String source,

                @NotNull(message = "effectiveFrom không được để trống") LocalDate effectiveFrom,

                LocalDate effectiveTo,

                Integer basePeriods,

                Integer homeroomReduction,

                Integer nursingReduction) {
}

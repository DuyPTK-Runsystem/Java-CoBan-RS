package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReqCreateEligibilityDTO(
                @NotNull(message = "Giáo viên không được để trống") Long teacherId,

                @NotBlank(message = "Mã quy tắc không được để trống") String ruleCode,

                @NotNull(message = "validFrom không được để trống") LocalDate validFrom,

                @NotNull(message = "validTo không được để trống") LocalDate validTo,

                @NotBlank(message = "Căn cứ chứng minh không được để trống") String evidenceReference) {
}

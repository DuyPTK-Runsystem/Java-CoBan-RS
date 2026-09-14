package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadRuleTriggerType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReqCreateTeacherLoadRuleDTO(
        @NotBlank(message = "Mã rule không được để trống") String ruleCode,
        @NotBlank(message = "Tên rule không được để trống") String ruleName,
        @NotNull(message = "Loại kích hoạt không được để trống") TeacherLoadRuleTriggerType triggerType,
        @NotNull(message = "Mức giảm không được để trống") @Min(value = 0, message = "Mức giảm không được âm") Integer reductionPeriods,
        @NotBlank(message = "Nguồn rule không được để trống") String source) {
}

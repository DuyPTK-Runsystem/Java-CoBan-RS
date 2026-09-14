package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadRuleTriggerType;

public record ResTeacherLoadRuleDTO(
        Long id,
        Long policyId,
        String ruleCode,
        String ruleName,
        TeacherLoadRuleTriggerType triggerType,
        Integer reductionPeriods,
        String source,
        boolean active) {
}

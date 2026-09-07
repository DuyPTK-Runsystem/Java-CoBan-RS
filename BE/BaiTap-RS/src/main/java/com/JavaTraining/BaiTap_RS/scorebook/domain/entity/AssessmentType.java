package com.JavaTraining.BaiTap_RS.scorebook.domain.entity;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssessmentType {
    KTTT(new BigDecimal("1.00")),
    KTDK(new BigDecimal("2.00")),
    KTCK(new BigDecimal("3.00"));

    private final BigDecimal standardWeightValue;

    AssessmentType(BigDecimal standardWeightValue) {
        this.standardWeightValue = standardWeightValue;
    }

    public BigDecimal standardWeight() {
        return standardWeightValue;
    }

    @JsonValue
    public String code() {
        return this == KTDK ? "KTĐK" : name();
    }

    @JsonCreator
    public static AssessmentType fromCode(String code) {
        if ("KTĐK".equals(code) || "KTDK".equals(code)) {
            return KTDK;
        }
        return valueOf(code);
    }

    public boolean isRequiredByStructure() {
        return this != KTTT;
    }

    public int maxAllowedActiveColumns(SubjectType subjectType) {
        if (subjectType == SubjectType.SKILL) {
            return 1;
        }
        if (this == KTCK) {
            return 1;
        }
        return Integer.MAX_VALUE;
    }
}

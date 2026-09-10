package com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests;

import java.math.BigDecimal;
import java.util.List;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSourceType;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqCreatePlacementSessionDTO(
        @NotNull @Positive Long academicYearId,
        @NotNull @Positive Long targetGradeId,
        @NotEmpty List<@Valid TargetClass> targetClasses,
        @NotEmpty List<@Valid Candidate> candidates,
        @NotNull @Size(max = 50) String ruleVersion) {
    public record Candidate(
            @NotNull @Positive Long studentId,
            @NotNull @Positive Long targetGradeId,
            @NotNull PlacementSourceType sourceType,
            BigDecimal score,
            @Size(max = 255) String scoreSourceReference,
            @Size(max = 50) String genderSnapshot,
            @Size(max = 1000) String eligibilityEvidence,
            @Size(max = 255) String approvalReference) { }
    public record TargetClass(
            @NotNull @Positive Long classId,
            @NotNull PlacementClassProfile profile,
            @Positive Integer capacity) { }
}

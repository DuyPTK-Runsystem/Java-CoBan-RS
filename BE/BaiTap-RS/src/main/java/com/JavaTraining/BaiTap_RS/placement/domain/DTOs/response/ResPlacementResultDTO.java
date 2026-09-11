package com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementIssueSeverity;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResultStatus;

public record ResPlacementResultDTO(
        Long id, Long studentId, Long targetClassId, PlacementResultStatus resultStatus,
        BigDecimal score, String issueCode, PlacementIssueSeverity issueSeverity, String explanation) { }

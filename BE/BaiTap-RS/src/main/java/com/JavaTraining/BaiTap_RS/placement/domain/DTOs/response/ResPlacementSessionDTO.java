package com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response;

import java.util.List;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSessionStatus;

public record ResPlacementSessionDTO(
        Long id, Long academicYearId, Long targetGradeId, PlacementSessionStatus status,
        String ruleVersion, Long version, List<Long> targetClassIds, List<ResPlacementTargetClassDTO> targetClasses,
        List<ResPlacementResultDTO> results) { }

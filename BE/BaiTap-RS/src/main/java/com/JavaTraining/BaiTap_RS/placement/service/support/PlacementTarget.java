package com.JavaTraining.BaiTap_RS.placement.service.support;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;

public record PlacementTarget(Long classId, String classCode, String className,
        PlacementClassProfile profile, Integer capacity) { }

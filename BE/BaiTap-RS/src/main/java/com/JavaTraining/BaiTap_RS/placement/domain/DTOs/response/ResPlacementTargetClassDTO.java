package com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;

public record ResPlacementTargetClassDTO(
        Long classId, String classCode, String className, PlacementClassProfile profile, Integer capacity,
        Integer genderTargetMale, Integer genderTargetFemale) { }

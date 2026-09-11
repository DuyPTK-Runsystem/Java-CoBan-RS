package com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests;

import java.util.List;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ReqUpdatePlacementSessionDTO(
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotEmpty List<@Valid TargetClass> targetClasses) {
    public record TargetClass(@NotNull @Positive Long classId,
            @NotNull PlacementClassProfile profile, @Positive Integer capacity) { }
}

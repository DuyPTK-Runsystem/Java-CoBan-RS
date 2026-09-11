package com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ReqPlacementActionDTO(
        @NotNull @PositiveOrZero Long expectedVersion,
        @Size(max = 100) String idempotencyKey) { }

package com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request contract for confirmation, including the mandatory replay key. */
public record ReqConfirmPlacementDTO(
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotBlank @Size(max = 100) String idempotencyKey) { }

package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ReqLibraryPolicyDTO(
        @NotBlank @Size(max = 64) String expectedVersion,
        @NotNull LocalDateTime effectiveAt,
        @Positive int maxActiveLoans,
        @Positive int loanDurationDays,
        @PositiveOrZero int maxRenewals,
        @Positive int renewalDurationDays,
        @Positive int reservationPickupDays,
        @NotEmpty List<@Valid FineTier> fineTiers,
        @NotNull @Positive BigDecimal fineCapPerLoan,
        @NotNull @Positive BigDecimal fineSuspensionThreshold) {

    public record FineTier(@Positive Integer throughDay, @NotNull @Positive BigDecimal dailyRate) {
    }
}

package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record LibraryPolicyDTO(String policyVersion, OffsetDateTime effectiveAt, int maxActiveLoans,
        int loanDurationDays, int maxRenewals, int renewalDurationDays, int reservationPickupDays,
        List<FineTier> fineTiers, BigDecimal fineCapPerLoan, BigDecimal fineSuspensionThreshold,
        OffsetDateTime updatedAt, Long updatedBy) {

    public record FineTier(Integer throughDay, BigDecimal dailyRate) {
    }
}

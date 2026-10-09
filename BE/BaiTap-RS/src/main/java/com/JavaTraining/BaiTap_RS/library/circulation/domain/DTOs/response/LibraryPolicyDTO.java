package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LibraryPolicyDTO(String policyVersion, LocalDateTime effectiveAt, int maxActiveLoans,
        int loanDurationDays, int maxRenewals, int renewalDurationDays, int reservationPickupDays,
        List<FineTier> fineTiers, BigDecimal fineCapPerLoan, BigDecimal fineSuspensionThreshold,
        LocalDateTime updatedAt, Long updatedBy) {

    public record FineTier(Integer throughDay, BigDecimal dailyRate) {
    }
}

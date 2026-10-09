package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.math.BigDecimal;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Access(AccessType.FIELD)
@Getter
@NoArgsConstructor
public class LibraryCirculationPolicyTerms {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    // Versioned policy terms remain non-final for Hibernate field access and are never changed after insertion.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "max_active_loans", nullable = false)
    private int maxActiveLoans;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "loan_duration_days", nullable = false)
    private int loanDurationDays;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "max_renewals", nullable = false)
    private int maxRenewals;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "renewal_duration_days", nullable = false)
    private int renewalDurationDays;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "reservation_pickup_days", nullable = false)
    private int reservationPickupDays;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "fine_cap_per_loan", nullable = false, precision = 12, scale = 2)
    private BigDecimal fineCapPerLoan;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "fine_suspension_threshold", nullable = false, precision = 12, scale = 2)
    private BigDecimal fineSuspensionThreshold;

    public LibraryCirculationPolicyTerms(int maxActiveLoans, int loanDurationDays, int maxRenewals,
            int renewalDurationDays, int reservationPickupDays, BigDecimal fineCapPerLoan,
            BigDecimal fineSuspensionThreshold) {
        this.maxActiveLoans = maxActiveLoans;
        this.loanDurationDays = loanDurationDays;
        this.maxRenewals = maxRenewals;
        this.renewalDurationDays = renewalDurationDays;
        this.reservationPickupDays = reservationPickupDays;
        this.fineCapPerLoan = fineCapPerLoan;
        this.fineSuspensionThreshold = fineSuspensionThreshold;
    }

}

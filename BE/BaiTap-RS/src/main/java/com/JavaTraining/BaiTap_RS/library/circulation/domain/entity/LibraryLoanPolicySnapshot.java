package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

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
public class LibraryLoanPolicySnapshot {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    // Creation-time policy terms remain non-final because Hibernate populates persistent fields.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "policy_version", nullable = false, length = 64)
    private String policyVersion;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "max_active_loans_snapshot", nullable = false)
    private int maxActiveLoans;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "loan_duration_days_snapshot", nullable = false)
    private int loanDurationDays;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "max_renewals_snapshot", nullable = false)
    private int maxRenewals;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "renewal_duration_days_snapshot", nullable = false)
    private int renewalDurationDays;

    public LibraryLoanPolicySnapshot(String policyVersion, int maxActiveLoans, int loanDurationDays,
            int maxRenewals, int renewalDurationDays) {
        this.policyVersion = policyVersion;
        this.maxActiveLoans = maxActiveLoans;
        this.loanDurationDays = loanDurationDays;
        this.maxRenewals = maxRenewals;
        this.renewalDurationDays = renewalDurationDays;
    }
}

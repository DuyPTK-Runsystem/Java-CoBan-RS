package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Access(AccessType.FIELD)
@Getter
@NoArgsConstructor
public class LibraryCirculationPolicySnapshot {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    // Version identity, effective boundary, and updater audit form one persisted policy revision.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "policy_version", nullable = false, unique = true, length = 64)
    private String policyVersion;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Embedded
    private LibraryCirculationPolicyTerms terms;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LibraryCirculationPolicySnapshot(String policyVersion, LocalDateTime effectiveAt,
            LibraryCirculationPolicyTerms terms, Long updatedBy, LocalDateTime updatedAt) {
        this.policyVersion = policyVersion;
        this.effectiveAt = effectiveAt;
        setTerms(terms);
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public int getMaxActiveLoans() {
        return terms.getMaxActiveLoans();
    }

    public int getLoanDurationDays() {
        return terms.getLoanDurationDays();
    }

    public int getMaxRenewals() {
        return terms.getMaxRenewals();
    }

    public int getRenewalDurationDays() {
        return terms.getRenewalDurationDays();
    }

    public int getReservationPickupDays() {
        return terms.getReservationPickupDays();
    }

    public BigDecimal getFineCapPerLoan() {
        return terms.getFineCapPerLoan();
    }

    public BigDecimal getFineSuspensionThreshold() {
        return terms.getFineSuspensionThreshold();
    }

    private void setTerms(LibraryCirculationPolicyTerms terms) {
        this.terms = terms;
    }
}

package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library_fine")
@Getter
@NoArgsConstructor
public class LibraryFine {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fine_id")
    private Long id;

    // Loan identity, fine kind, and currency are fixed when the fine is created; JPA field access requires non-final fields.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Enumerated(EnumType.STRING)
    @Column(name = "fine_type", nullable = false, length = 20)
    private FineType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(nullable = false)
    private boolean provisional;

    @Column(name = "calculated_through")
    private LocalDate calculatedThrough;

    @Column(name = "policy_version", nullable = false, length = 64)
    private String policyVersion;

    @Transient
    private LibraryFineSettlement settlement;

    @Version
    @Column(nullable = false)
    private Long version;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LibraryFine(Long loanId, FineType type, BigDecimal amount, boolean provisional,
            LocalDate calculatedThrough, String policyVersion, LocalDateTime now) {
        this.loanId = loanId;
        this.type = type;
        this.status = FineStatus.UNPAID;
        this.amount = amount;
        this.provisional = provisional;
        this.calculatedThrough = calculatedThrough;
        this.policyVersion = policyVersion;
        this.settlement = new LibraryFineSettlement();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateAmount(BigDecimal amount, boolean provisional, LocalDate through, String policyVersion,
            LocalDateTime now) {
        if (status == FineStatus.UNPAID
                && (calculatedThrough == null || through == null || !through.isBefore(calculatedThrough))) {
            this.amount = amount;
            this.provisional = provisional;
            this.calculatedThrough = through;
            this.policyVersion = policyVersion;
            this.updatedAt = now;
        }
    }

    public void pay(String reference, Long actorId, LocalDateTime now) {
        status = FineStatus.PAID;
        settlement.pay(reference, actorId, now);
        updatedAt = now;
    }

    public void waive(String reason, Long actorId, LocalDateTime now) {
        status = FineStatus.WAIVED;
        settlement.waive(reason, actorId, now);
        updatedAt = now;
    }

    public LocalDateTime getPaidAt() {
        return settlement.getPaidAt();
    }

    public String getPaymentReference() {
        return settlement.getPaymentReference();
    }

    public Long getPaidBy() {
        return settlement.getPaidBy();
    }

    public LocalDateTime getWaivedAt() {
        return settlement.getWaivedAt();
    }

    public String getWaiveReason() {
        return settlement.getWaiveReason();
    }

    public Long getWaivedBy() {
        return settlement.getWaivedBy();
    }

    @Embedded
    @Access(AccessType.PROPERTY)
    protected LibraryFineSettlement getSettlement() {
        return settlement;
    }

    protected void setSettlement(LibraryFineSettlement settlement) {
        this.settlement = settlement;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

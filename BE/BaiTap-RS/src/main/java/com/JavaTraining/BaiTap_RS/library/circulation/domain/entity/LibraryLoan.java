package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library_loan")
@Getter
@NoArgsConstructor
public class LibraryLoan {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long id;

    // Borrower, copy, card, and policy values are creation-time snapshots; JPA field access requires non-final fields.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "patron_id", nullable = false)
    private Long patronId;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "copy_id", nullable = false)
    private Long copyId;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "borrowed_at", nullable = false)
    private LocalDateTime borrowedAt;

    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "lost_at")
    private LocalDateTime lostAt;

    @Column(name = "renew_count", nullable = false)
    private int renewCount;

    @Transient
    private LibraryLoanPolicySnapshot policySnapshot;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LibraryLoan(Long patronId, Long copyId, Long cardId, LocalDateTime borrowedAt, LocalDateTime dueAt,
            String policyVersion, int maxActiveLoansSnapshot, int loanDurationDaysSnapshot,
            int maxRenewalsSnapshot, int renewalDurationDaysSnapshot) {
        this.patronId = patronId;
        this.copyId = copyId;
        this.cardId = cardId;
        this.status = LoanStatus.ACTIVE;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
        this.policySnapshot = new LibraryLoanPolicySnapshot(policyVersion, maxActiveLoansSnapshot,
                loanDurationDaysSnapshot, maxRenewalsSnapshot, renewalDurationDaysSnapshot);
        this.renewCount = 0;
        this.createdAt = borrowedAt;
        this.updatedAt = borrowedAt;
    }

    public void renew(LocalDateTime newDueAt, LocalDateTime now) {
        dueAt = newDueAt;
        renewCount++;
        updatedAt = now;
    }

    public void returnAt(LocalDateTime now) {
        status = LoanStatus.RETURNED;
        returnedAt = now;
        updatedAt = now;
    }

    public void markLost(LocalDateTime now) {
        status = LoanStatus.LOST;
        lostAt = now;
        updatedAt = now;
    }

    public String getPolicyVersion() {
        return policySnapshot.getPolicyVersion();
    }

    public int getMaxActiveLoansSnapshot() {
        return policySnapshot.getMaxActiveLoans();
    }

    public int getLoanDurationDaysSnapshot() {
        return policySnapshot.getLoanDurationDays();
    }

    public int getMaxRenewalsSnapshot() {
        return policySnapshot.getMaxRenewals();
    }

    public int getRenewalDurationDaysSnapshot() {
        return policySnapshot.getRenewalDurationDays();
    }

    @Embedded
    @Access(AccessType.PROPERTY)
    protected LibraryLoanPolicySnapshot getPolicySnapshot() {
        return policySnapshot;
    }

    protected void setPolicySnapshot(LibraryLoanPolicySnapshot policySnapshot) {
        this.policySnapshot = policySnapshot;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library_loan_renewal")
@Getter
@NoArgsConstructor
public class LibraryLoanRenewal {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renewal_id")
    private Long id;

    // Renewal rows preserve the original loan/policy terms as an audit snapshot; JPA field access requires non-final fields.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "renewal_number", nullable = false)
    private int renewalNumber;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "policy_version", nullable = false, length = 64)
    private String policyVersion;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "renewal_days_snapshot", nullable = false)
    private int renewalDaysSnapshot;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "previous_due_at", nullable = false)
    private LocalDateTime previousDueAt;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "new_due_at", nullable = false)
    private LocalDateTime newDueAt;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public LibraryLoanRenewal(Long loanId, int renewalNumber, String policyVersion, int renewalDaysSnapshot,
            LocalDateTime previousDueAt, LocalDateTime newDueAt, Long actorUserId, LocalDateTime createdAt) {
        this.loanId = loanId;
        this.renewalNumber = renewalNumber;
        this.policyVersion = policyVersion;
        this.renewalDaysSnapshot = renewalDaysSnapshot;
        this.previousDueAt = previousDueAt;
        this.newDueAt = newDueAt;
        this.actorUserId = actorUserId;
        this.createdAt = createdAt;
    }
}

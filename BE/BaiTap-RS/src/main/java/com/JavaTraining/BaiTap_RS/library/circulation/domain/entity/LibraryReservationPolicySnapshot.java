package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.time.LocalDateTime;

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
public class LibraryReservationPolicySnapshot {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    // Queue time and pickup terms are fixed when the reservation is created; Hibernate needs writable fields.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "policy_version", nullable = false, length = 64)
    private String policyVersion;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "pickup_days_snapshot", nullable = false)
    private int pickupDays;

    public LibraryReservationPolicySnapshot(LocalDateTime reservedAt, String policyVersion, int pickupDays) {
        this.reservedAt = reservedAt;
        this.policyVersion = policyVersion;
        this.pickupDays = pickupDays;
    }
}

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library_reservation")
@Getter
@NoArgsConstructor
public class LibraryReservation {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    // Reservation owner and policy values are fixed when queued; JPA field access requires non-final fields.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "patron_id", nullable = false)
    private Long patronId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Transient
    private LibraryReservationPolicySnapshot policySnapshot;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "pickup_due_at")
    private LocalDateTime pickupDueAt;

    @Column(name = "allocated_copy_id")
    private Long allocatedCopyId;

    @Column(name = "fulfilled_at")
    private LocalDateTime fulfilledAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LibraryReservation(Long bookId, Long patronId, LocalDateTime now,
            String policyVersion, int pickupDaysSnapshot) {
        this.bookId = bookId;
        this.patronId = patronId;
        this.status = ReservationStatus.WAITING;
        this.policySnapshot = new LibraryReservationPolicySnapshot(now, policyVersion, pickupDaysSnapshot);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void ready(Long copyId, LocalDateTime now, LocalDateTime pickupDueAt) {
        status = ReservationStatus.READY;
        allocatedCopyId = copyId;
        readyAt = now;
        this.pickupDueAt = pickupDueAt;
        updatedAt = now;
    }

    public void fulfill(LocalDateTime now) {
        status = ReservationStatus.FULFILLED;
        fulfilledAt = now;
        updatedAt = now;
    }

    public void cancel(LocalDateTime now) {
        status = ReservationStatus.CANCELLED;
        cancelledAt = now;
        updatedAt = now;
    }

    public void expire(LocalDateTime now) {
        status = ReservationStatus.EXPIRED;
        expiredAt = now;
        updatedAt = now;
    }

    public LocalDateTime getReservedAt() {
        return policySnapshot.getReservedAt();
    }

    public String getPolicyVersion() {
        return policySnapshot.getPolicyVersion();
    }

    public int getPickupDaysSnapshot() {
        return policySnapshot.getPickupDays();
    }

    @Embedded
    @Access(AccessType.PROPERTY)
    protected LibraryReservationPolicySnapshot getPolicySnapshot() {
        return policySnapshot;
    }

    protected void setPolicySnapshot(LibraryReservationPolicySnapshot policySnapshot) {
        this.policySnapshot = policySnapshot;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

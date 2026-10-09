package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response;

import java.time.OffsetDateTime;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;

public record LibraryReservationDTO(Long reservationId, Long bookId, String bookTitle, Long patronId,
        ReservationStatus status, OffsetDateTime reservedAt, OffsetDateTime readyAt, OffsetDateTime pickupDueAt,
        String allocatedCopyBarcode, OffsetDateTime fulfilledAt, OffsetDateTime cancelledAt, String policyVersion) {
}

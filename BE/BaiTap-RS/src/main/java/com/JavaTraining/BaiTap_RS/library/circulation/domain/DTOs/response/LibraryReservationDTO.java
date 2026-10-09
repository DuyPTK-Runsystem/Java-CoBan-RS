package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;

public record LibraryReservationDTO(Long reservationId, Long bookId, String bookTitle, Long patronId,
        ReservationStatus status, LocalDateTime reservedAt, LocalDateTime readyAt, LocalDateTime pickupDueAt,
        String allocatedCopyBarcode, LocalDateTime fulfilledAt, LocalDateTime cancelledAt, String policyVersion) {
}

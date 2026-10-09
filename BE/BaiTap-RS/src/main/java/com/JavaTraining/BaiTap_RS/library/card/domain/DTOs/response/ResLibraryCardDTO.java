package com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;

public record ResLibraryCardDTO(String cardNo, Long patronId, LibraryCardStatus status,
        LocalDateTime issuedAt, LocalDate expiresAt, String payloadVersion, String policyVersion,
        LocalDateTime revokedAt, String revokedReason) {
}

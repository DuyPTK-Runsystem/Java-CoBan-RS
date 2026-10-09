package com.JavaTraining.BaiTap_RS.library.card.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;

public final class LibraryCardMapper {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private LibraryCardMapper() {
    }

    public static ResLibraryCardDTO toResponse(LibraryCard card) {
        LibraryCardStatus status = card.getStatus();
        if (status == LibraryCardStatus.ACTIVE && card.getExpiresAt().isBefore(LocalDate.now(LIBRARY_ZONE))) {
            status = LibraryCardStatus.EXPIRED;
        }
        return new ResLibraryCardDTO(card.getCardNo(), card.getPatronId(), status, card.getIssuedAt(),
                card.getExpiresAt(), card.getPayloadVersion(), card.getPolicyVersion(), card.getRevokedAt(),
                card.getRevokedReason());
    }

    public static Map<String, Object> auditSnapshot(LibraryCard card) {
        return Map.of("cardNo", card.getCardNo(), "patronId", card.getPatronId(),
                "status", card.getStatus().name(), "expiresAt", card.getExpiresAt().toString(),
                "revokedAt", card.getRevokedAt() == null ? "" : card.getRevokedAt().toString(),
                "revokedReason", card.getRevokedReason() == null ? "" : card.getRevokedReason());
    }
}

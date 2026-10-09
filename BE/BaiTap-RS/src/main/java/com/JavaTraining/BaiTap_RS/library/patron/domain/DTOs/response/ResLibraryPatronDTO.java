package com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;

public record ResLibraryPatronDTO(Long patronId, Long userId, String displayName,
        LibraryPatronStatus status, LocalDateTime joinedAt, List<String> suspensionReasons,
        ResLibraryCardDTO currentCard) {
}

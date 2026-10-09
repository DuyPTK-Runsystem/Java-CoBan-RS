package com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;

public record ResLibraryCardVerificationDTO(boolean valid, ResLibraryCardDTO card,
        LibraryPatronStatus patronStatus) {
}

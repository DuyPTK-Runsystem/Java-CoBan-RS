package com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqReissueLibraryCardDTO(
        @NotNull LocalDate expiresAt,
        @NotBlank @Size(max = 500) String reason) {
}

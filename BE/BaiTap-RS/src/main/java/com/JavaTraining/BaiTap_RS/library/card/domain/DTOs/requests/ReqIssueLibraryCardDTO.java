package com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqIssueLibraryCardDTO(@NotNull @Positive Long patronId, @NotNull LocalDate expiresAt) {
}

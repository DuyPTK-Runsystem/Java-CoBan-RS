package com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReqRevokeLibraryCardDTO(@NotBlank @Size(max = 500) String reason) {
}

package com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReqVerifyLibraryCardDTO(@NotBlank @Size(max = 1024) String payload) {
}

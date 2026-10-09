package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqBorrowCopiesDTO(
        @NotNull @Positive Long patronId,
        @NotBlank @Size(max = 32) String cardNo,
        @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 64) String> copyBarcodes) {
}

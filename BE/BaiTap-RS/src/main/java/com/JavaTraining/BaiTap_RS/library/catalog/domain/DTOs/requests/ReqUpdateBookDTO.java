package com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqUpdateBookDTO(
        @NotNull @Min(0) Long expectedVersion,
        @Size(max = 20) String isbn,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 200) String author,
        @Size(max = 200) String publisher,
        @Min(1) @Max(9999) Integer publishedYear,
        @Size(max = 100) String category,
        @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal listPrice,
        @Size(max = 2048) String coverUrl) {
}

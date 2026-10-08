package com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response;

import java.math.BigDecimal;

public record ResBookDetailDTO(Long id, String isbn, String title, String author, String publisher,
        Integer publishedYear, String category, BigDecimal listPrice, String coverUrl,
        long totalCopyCount, long availableBorrowableCopyCount, Long version) {
}

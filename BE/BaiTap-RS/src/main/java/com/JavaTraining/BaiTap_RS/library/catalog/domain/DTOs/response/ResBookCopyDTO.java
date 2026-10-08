package com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;

public record ResBookCopyDTO(Long id, Long bookId, String barcode, String shelfLocation,
        BookCopyStatus status, boolean referenceOnly, Long version, LocalDateTime createdAt) {
}

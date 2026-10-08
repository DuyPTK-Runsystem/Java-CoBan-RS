package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;

public final class BookCopyMapper {

    private BookCopyMapper() {
    }

    public static ResBookCopyDTO toResponse(BookCopy copy) {
        return new ResBookCopyDTO(copy.getId(), copy.getBook().getId(), copy.getBarcode(), copy.getShelfLocation(),
                copy.getStatus(), copy.isReferenceOnly(), copy.getVersion(), copy.getCreatedAt());
    }

    public static Map<String, Object> auditSnapshot(BookCopy copy) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("barcode", copy.getBarcode());
        snapshot.put("shelfLocation", copy.getShelfLocation());
        snapshot.put("status", copy.getStatus());
        snapshot.put("referenceOnly", copy.isReferenceOnly());
        snapshot.put("version", copy.getVersion());
        return snapshot;
    }
}

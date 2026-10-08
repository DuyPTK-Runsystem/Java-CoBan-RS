package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookDetailDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookSummaryDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;

public final class BookCatalogMapper {

    private BookCatalogMapper() {
    }

    public static Map<Long, Long> counts(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    public static ResBookSummaryDTO summary(Book book, long total, long available) {
        return new ResBookSummaryDTO(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getPublisher(), book.getPublishedYear(), book.getCategory(), book.getListPrice(),
                book.getCoverUrl(), total, available);
    }

    public static ResBookDetailDTO detail(Book book, ResBookSummaryDTO summary) {
        return new ResBookDetailDTO(summary.id(), summary.isbn(), summary.title(), summary.author(),
                summary.publisher(), summary.publishedYear(), summary.category(), summary.listPrice(),
                summary.coverUrl(), summary.totalCopyCount(), summary.availableBorrowableCopyCount(),
                book.getVersion());
    }

    public static Map<String, Object> audit(Book book) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("isbn", book.getIsbn());
        snapshot.put("title", book.getTitle());
        snapshot.put("author", book.getAuthor());
        snapshot.put("publisher", book.getPublisher());
        snapshot.put("publishedYear", book.getPublishedYear());
        snapshot.put("category", book.getCategory());
        snapshot.put("listPrice", book.getListPrice());
        snapshot.put("coverUrl", book.getCoverUrl());
        snapshot.put("archivedAt", book.getArchivedAt());
        snapshot.put("version", book.getVersion());
        return snapshot;
    }
}

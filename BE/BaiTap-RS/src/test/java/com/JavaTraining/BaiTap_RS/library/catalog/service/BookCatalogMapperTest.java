package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookDetailDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookSummaryDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class BookCatalogMapperTest {

    @Test
    void mapsTotalCopyCountToCatalogSummary() {
        assertEquals(5L, summary().totalCopyCount(), "summary maps total copies");
    }

    @Test
    void mapsBorrowableCopyCountToCatalogSummary() {
        assertEquals(2L, summary().availableBorrowableCopyCount(), "summary maps borrowable copies");
    }

    @Test
    void mapsVersionToCatalogDetail() {
        ResBookSummaryDTO summary = summary();
        ResBookDetailDTO detail = BookCatalogMapper.detail(book(), summary);
        assertEquals(3L, detail.version(), "detail maps optimistic-lock version");
    }

    @Test
    void mapsSelectedMetadataToAuditSnapshot() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("publisher", "Press");
        expected.put("publishedYear", 2024);
        expected.put("category", "Science");
        expected.put("listPrice", new BigDecimal("8.50"));
        expected.put("coverUrl", "https://example.org/cover.jpg");
        expected.put("archivedAt", null);
        Map<String, Object> actual = BookCatalogMapper.audit(book());
        actual.keySet().retainAll(expected.keySet());
        assertEquals(expected, actual, "audit snapshot maps all asserted metadata fields");
    }

    @Test
    void mapsFirstRepositoryCountRowByBookId() {
        assertEquals(5L, counts().get(12L), "first book count is mapped");
    }

    @Test
    void mapsSecondRepositoryCountRowByBookId() {
        assertEquals(2L, counts().get(13L), "second book count is mapped");
    }

    private Book book() {
        Book book = new Book("9780306406157", "Title", "Author", "Press", 2024, "Science",
                new BigDecimal("8.50"), "https://example.org/cover.jpg");
        book.setId(12L);
        book.setVersion(3L);
        return book;
    }

    private ResBookSummaryDTO summary() {
        return BookCatalogMapper.summary(book(), 5L, 2L);
    }

    private Map<Long, Long> counts() {
        return BookCatalogMapper.counts(List.<Object[]>of(
                new Object[] { 12L, 5L }, new Object[] { 13L, 2L }));
    }
}

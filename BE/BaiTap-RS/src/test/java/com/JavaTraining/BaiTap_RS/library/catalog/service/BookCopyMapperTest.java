package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.time.LocalDateTime;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class BookCopyMapperTest {

    @Test
    void mapsCopyResponseFields() {
        BookCopy copy = copy();

        ResBookCopyDTO response = BookCopyMapper.toResponse(copy);

        assertEquals(new ResBookCopyDTO(501L, 12L, "LIB-000000501", "Shelf A", BookCopyStatus.DAMAGED,
                        true, 2L, LocalDateTime.of(2026, 10, 8, 10, 0)),
                response, "Response should preserve the complete copy state and its book identity");
    }

    @Test
    void mapsCopyAuditState() {
        BookCopy copy = copy();

        Map<String, Object> audit = BookCopyMapper.auditSnapshot(copy);

        assertEquals(Map.of("barcode", "LIB-000000501", "shelfLocation", "Shelf A", "status",
                        BookCopyStatus.DAMAGED, "referenceOnly", true, "version", 2L),
                audit, "Audit snapshot should contain all mutable copy fields");
    }

    private BookCopy copy() {
        Book book = new Book(null, "Title", "Author", null, null, null, null, null);
        book.setId(12L);
        BookCopy copy = new BookCopy(book, "LIB-000000501", "Shelf A", true);
        copy.setId(501L);
        copy.setVersion(2L);
        copy.setCreatedAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        copy.setStatus(BookCopyStatus.DAMAGED);
        return copy;
    }
}

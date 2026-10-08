package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.math.BigDecimal;
import java.util.List;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookSummaryDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:library-catalog-query;MODE=MySQL;"
                + "DATABASE_TO_UPPER=false;NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.ai.openai.api-key=qa-placeholder-not-a-secret",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookCatalogQueryJpaTest {

    private static final String FICTION_CATEGORY = "Fiction";

    @Autowired private BookService bookService;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository copyRepository;

    @MockitoBean private LibraryCatalogAuditService auditService;

    @BeforeEach
    void clearCatalog() {
        copyRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    void availabilitySearchUsesEligibleCopiesWithoutDuplicatingBooksAndCountsInventory() {
        AvailabilityFixture fixture = createAvailabilityFixture();

        ResultPaginationDTO<ResBookSummaryDTO> available = bookService.search(
                null, null, null, true, 0, 20, List.of("title,asc"));
        ResBookSummaryDTO availableBook = available.result().get(0);
        assertEquals(new AvailabilityProjection(1L, fixture.eligibleBookId(), 4L, 2L),
                new AvailabilityProjection(available.meta().totalItems(), availableBook.id(),
                        availableBook.totalCopyCount(), availableBook.availableBorrowableCopyCount()),
                "availability search returns one eligible book with its total and borrowable counts");
    }

    @Test
    void unavailableSearchExcludesArchivedBooks() {
        AvailabilityFixture fixture = createAvailabilityFixture();
        ResultPaginationDTO<ResBookSummaryDTO> unavailable = bookService.search(
                null, null, null, false, 0, 20, List.of("title,asc"));
        boolean archivedBookReturned = unavailable.result().stream()
                .anyMatch(item -> item.id().equals(fixture.archivedBookId()));
        assertEquals(new UnavailableProjection(2L, false),
                new UnavailableProjection(unavailable.meta().totalItems(), archivedBookReturned),
                "unavailable search returns active inventory and excludes archived inventory");
    }

    private AvailabilityFixture createAvailabilityFixture() {
        Book eligible = saveBook("Searchable volume", FICTION_CATEGORY, 2020);
        saveCopy(eligible, "LIB-000000101", BookCopyStatus.AVAILABLE, false);
        saveCopy(eligible, "LIB-000000102", BookCopyStatus.AVAILABLE, false);
        saveCopy(eligible, "LIB-000000103", BookCopyStatus.AVAILABLE, true);
        saveCopy(eligible, "LIB-000000104", BookCopyStatus.DAMAGED, false);

        Book referenceOnly = saveBook("Reference volume", FICTION_CATEGORY, 2020);
        saveCopy(referenceOnly, "LIB-000000105", BookCopyStatus.AVAILABLE, true);
        Book damagedOnly = saveBook("Damaged volume", FICTION_CATEGORY, 2020);
        saveCopy(damagedOnly, "LIB-000000106", BookCopyStatus.DAMAGED, false);
        Book archived = saveBook("Archived volume", FICTION_CATEGORY, 2020);
        saveCopy(archived, "LIB-000000107", BookCopyStatus.AVAILABLE, false);
        archived.setArchivedAt(java.time.LocalDateTime.now());
        bookRepository.saveAndFlush(archived);
        return new AvailabilityFixture(eligible.getId(), archived.getId());
    }

    @Test
    void keywordEscapesLikeWildcardsAndFiltersCategoryAndExactYearWithRealPages() {
        Book literal = saveBook("Percent %_ title", "Drama", 2024);
        saveCopy(literal, "LIB-000000201", BookCopyStatus.AVAILABLE, false);
        Book broad = saveBook("Percent XX title", "Drama", 2024);
        saveCopy(broad, "LIB-000000202", BookCopyStatus.AVAILABLE, false);
        Book otherCategory = saveBook("Percent %_ anthology", "History", 2024);
        saveCopy(otherCategory, "LIB-000000203", BookCopyStatus.AVAILABLE, false);
        Book otherYear = saveBook("Percent %_ older", "Drama", 2023);
        saveCopy(otherYear, "LIB-000000204", BookCopyStatus.AVAILABLE, false);

        ResultPaginationDTO<ResBookSummaryDTO> result = bookService.search(
                "%_", " drama ", 2024, null, 0, 1, List.of("title,asc"));
        assertEquals(new KeywordPageProjection(1L, 1, literal.getId()),
                new KeywordPageProjection(result.meta().totalItems(), result.meta().totalPages(),
                        result.result().get(0).id()),
                "keyword, category, year, and pagination filters compose correctly");
    }

    private Book saveBook(String title, String category, int year) {
        return bookRepository.saveAndFlush(new Book(null, title, "Author", null, year, category,
                new BigDecimal("10.00"), null));
    }

    private void saveCopy(Book book, String barcode, BookCopyStatus status, boolean referenceOnly) {
        BookCopy copy = new BookCopy(book, barcode, null, referenceOnly);
        copy.setStatus(status);
        copyRepository.saveAndFlush(copy);
    }

    private record AvailabilityProjection(long totalItems, Long bookId, long totalCopies, long borrowableCopies) {
    }

    private record UnavailableProjection(long totalItems, boolean archivedBookReturned) {
    }

    private record KeywordPageProjection(long totalItems, int totalPages, Long firstBookId) {
    }

    private record AvailabilityFixture(Long eligibleBookId, Long archivedBookId) {
    }
}

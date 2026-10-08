package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqUpdateBookDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookDetailDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookSummaryDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final String NORMALIZED_ISBN = "9780306406157";
    private static final String EXAMPLE_PRESS = "Example Press";
    private static final String OLD_PRESS = "Old Press";
    private static final String PUBLISHER_KEY = "publisher";

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCopyRepository copyRepository;

    @Mock
    private BookCatalogLookupService lookupService;

    @Mock
    private BookSortResolver sortResolver;

    @Mock
    private LibraryCatalogAuditService auditService;

    @InjectMocks
    private BookService bookService;

    @Nested
    // default access modifier required for JUnit Jupiter nested tests
    class CreateBookTests {

        @Test
        void createReturnsNormalizedMetadataAndCounts() {
            stubCreate();

            ResBookDetailDTO result = bookService.create(createRequest());

            assertEquals(List.of(17L, NORMALIZED_ISBN, "Domain Design", "Ada Writer", EXAMPLE_PRESS, "Computing",
                            2L, 1L),
                    List.of(result.id(), result.isbn(), result.title(), result.author(), result.publisher(),
                            result.category(), result.totalCopyCount(), result.availableBorrowableCopyCount()),
                    "Creation response should contain normalized metadata and persisted copy counts");
        }

        @Test
        void createWritesCompletePersistedSnapshotToAudit() {
            stubCreate();

            bookService.create(createRequest());

            Mockito.verify(auditService).record(org.mockito.ArgumentMatchers.eq("BOOK_CREATED"),
                    org.mockito.ArgumentMatchers.eq("book"), org.mockito.ArgumentMatchers.eq(17L),
                    org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.argThat(snapshot ->
                            NORMALIZED_ISBN.equals(snapshot.get("isbn"))
                                    && EXAMPLE_PRESS.equals(snapshot.get(PUBLISHER_KEY))
                                    && Integer.valueOf(2020).equals(snapshot.get("publishedYear"))
                                    && "Computing".equals(snapshot.get("category"))
                                    && new BigDecimal("12.50").equals(snapshot.get("listPrice"))
                                    && "https://example.org/cover.jpg".equals(snapshot.get("coverUrl"))));
        }

        @Test
        void duplicateNormalizedIsbnReturnsConflictCode() throws Throwable {
            when(bookRepository.existsByIsbn("0306406152")).thenReturn(true);

            assertEquals("DUPLICATE_ISBN", catalogErrorCode(() -> bookService.create(
                    new ReqCreateBookDTO("0-306-40615-2", "Title", "Author", null, null, null, null, null))),
                    "Duplicate normalized ISBN should use the stable conflict code");
        }

        @Test
        void duplicateNormalizedIsbnDoesNotPersist() {
            when(bookRepository.existsByIsbn("0306406152")).thenReturn(true);

            try {
                bookService.create(new ReqCreateBookDTO("0-306-40615-2", "Title", "Author",
                        null, null, null, null, null));
                throw new AssertionError("Duplicate normalized ISBN should be rejected");
            } catch (LibraryCatalogException expected) {
                // Rejection is the setup for the persistence interaction check.
            }

            Mockito.verify(bookRepository, Mockito.never()).saveAndFlush(any(Book.class));
        }
    }

    @Nested
    // default access modifier required for JUnit Jupiter nested tests
    class UpdateBookTests {

        @Test
        void staleExpectedVersionReturnsConflictCode() throws Throwable {
            Book book = book(9L, "Current", 4L);
            when(lookupService.activeBookForUpdate(9L)).thenReturn(book);

            assertEquals("VERSION_CONFLICT", catalogErrorCode(() -> bookService.update(9L, update(3L, "Changed"))),
                    "Stale expected version should use the version conflict code");
        }

        @Test
        void staleExpectedVersionDoesNotCheckIsbnUniqueness() {
            Book book = book(9L, "Current", 4L);
            when(lookupService.activeBookForUpdate(9L)).thenReturn(book);

            try {
                bookService.update(9L, update(3L, "Changed"));
                throw new AssertionError("Stale expected version should be rejected");
            } catch (LibraryCatalogException expected) {
                // Rejection is the setup for the repository interaction check.
            }

            Mockito.verify(bookRepository, Mockito.never()).existsByIsbnAndIdNot(any(), any());
        }

        @Test
        void staleExpectedVersionDoesNotWriteAudit() {
            Book book = book(9L, "Current", 4L);
            when(lookupService.activeBookForUpdate(9L)).thenReturn(book);

            try {
                bookService.update(9L, update(3L, "Changed"));
                throw new AssertionError("Stale expected version should be rejected");
            } catch (LibraryCatalogException expected) {
                // Rejection is the setup for the audit interaction check.
            }

            Mockito.verify(auditService, Mockito.never()).record(any(), any(), any(), any(), any());
        }

        @Test
        void updateReturnsNewMetadata() {
            stubUpdate();

            ResBookDetailDTO result = bookService.update(9L, updateRequest());

            assertEquals(List.of("After", "New Press", 2024, "Science", new BigDecimal("25.75")),
                    List.of(result.title(), result.publisher(), result.publishedYear(), result.category(),
                            result.listPrice()),
                    "Update response should return the complete normalized metadata");
        }

        @Test
        void updateWritesCompleteBeforeAndAfterSnapshotsToAudit() {
            stubUpdate();

            bookService.update(9L, updateRequest());

            Mockito.verify(auditService).record(org.mockito.ArgumentMatchers.eq("BOOK_UPDATED"),
                    org.mockito.ArgumentMatchers.eq("book"), org.mockito.ArgumentMatchers.eq(9L),
                    org.mockito.ArgumentMatchers.argThat(before -> OLD_PRESS.equals(before.get(PUBLISHER_KEY))
                            && Integer.valueOf(2010).equals(before.get("publishedYear"))
                            && "History".equals(before.get("category"))
                            && new BigDecimal("10.00").equals(before.get("listPrice"))
                            && "https://example.org/old.jpg".equals(before.get("coverUrl"))),
                    org.mockito.ArgumentMatchers.argThat(after -> "New Press".equals(after.get(PUBLISHER_KEY))
                            && Integer.valueOf(2024).equals(after.get("publishedYear"))
                            && "Science".equals(after.get("category"))
                            && new BigDecimal("25.75").equals(after.get("listPrice"))
                            && "https://example.org/new.jpg".equals(after.get("coverUrl"))));
        }
    }

    @Nested
    // default access modifier required for JUnit Jupiter nested tests
    class ArchiveBookTests {

        @Test
        void activeCirculationReturnsConflictAndLeavesBookActive() throws Throwable {
            Book book = book(9L, "Current", 4L);
            when(lookupService.activeBookForUpdate(9L)).thenReturn(book);
            when(copyRepository.existsByBookIdAndStatusIn(9L,
                    List.of(BookCopyStatus.ON_LOAN, BookCopyStatus.RESERVED))).thenReturn(true);

            assertEquals(List.of("BOOK_HAS_ACTIVE_CIRCULATION", false),
                    List.of(catalogErrorCode(() -> bookService.archive(9L, 4L)), book.isArchived()),
                    "Active circulation should block archive and preserve the active book");
        }

        @Test
        void activeCirculationDoesNotFlushBook() {
            stubArchiveWithCirculation();

            try {
                bookService.archive(9L, 4L);
                throw new AssertionError("Active circulation should block archival");
            } catch (LibraryCatalogException expected) {
                // Rejection is the setup for the flush interaction check.
            }

            Mockito.verify(bookRepository, Mockito.never()).flush();
        }

        @Test
        void activeCirculationDoesNotWriteAudit() {
            stubArchiveWithCirculation();

            try {
                bookService.archive(9L, 4L);
                throw new AssertionError("Active circulation should block archival");
            } catch (LibraryCatalogException expected) {
                // Rejection is the setup for the audit interaction check.
            }

            Mockito.verify(auditService, Mockito.never()).record(any(), any(), any(), any(), any());
        }

        @Test
        void archiveMarksBookArchived() {
            Book book = book(9L, "Current", 4L);
            book.setPublisher(EXAMPLE_PRESS);
            when(lookupService.activeBookForUpdate(9L)).thenReturn(book);
            when(copyRepository.existsByBookIdAndStatusIn(9L,
                    List.of(BookCopyStatus.ON_LOAN, BookCopyStatus.RESERVED))).thenReturn(false);

            bookService.archive(9L, 4L);

            assertEquals(true, book.getArchivedAt() != null, "Archival should set the book archived timestamp");
        }

        @Test
        void archiveWritesBeforeAndAfterSnapshotsToAudit() {
            Book book = book(9L, "Current", 4L);
            book.setPublisher(EXAMPLE_PRESS);
            when(lookupService.activeBookForUpdate(9L)).thenReturn(book);
            when(copyRepository.existsByBookIdAndStatusIn(9L,
                    List.of(BookCopyStatus.ON_LOAN, BookCopyStatus.RESERVED))).thenReturn(false);

            bookService.archive(9L, 4L);

            Mockito.verify(auditService).record(org.mockito.ArgumentMatchers.eq("BOOK_ARCHIVED"),
                    org.mockito.ArgumentMatchers.eq("book"), org.mockito.ArgumentMatchers.eq(9L),
                    org.mockito.ArgumentMatchers.argThat(before -> EXAMPLE_PRESS.equals(before.get(PUBLISHER_KEY))
                            && before.get("archivedAt") == null),
                    org.mockito.ArgumentMatchers.argThat(after -> EXAMPLE_PRESS.equals(after.get(PUBLISHER_KEY))
                            && after.get("archivedAt") != null));
        }
    }

    private void stubCreate() {
        when(bookRepository.existsByIsbn(NORMALIZED_ISBN)).thenReturn(false);
        when(bookRepository.saveAndFlush(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId(17L);
            return saved;
        });
        when(lookupService.summary(any(Book.class))).thenAnswer(invocation -> summary(invocation.getArgument(0)));
    }

    private void stubUpdate() {
        Book book = book(9L, "Before", 4L);
        book.setIsbn("0306406152");
        book.setPublisher(OLD_PRESS);
        book.setPublishedYear(2010);
        book.setCategory("History");
        book.setListPrice(new BigDecimal("10.00"));
        book.setCoverUrl("https://example.org/old.jpg");
        when(lookupService.activeBookForUpdate(9L)).thenReturn(book);
        when(bookRepository.existsByIsbnAndIdNot(NORMALIZED_ISBN, 9L)).thenReturn(false);
        when(lookupService.summary(any(Book.class))).thenAnswer(invocation -> summary(invocation.getArgument(0)));
    }

    private void stubArchiveWithCirculation() {
        when(lookupService.activeBookForUpdate(9L)).thenReturn(book(9L, "Current", 4L));
        when(copyRepository.existsByBookIdAndStatusIn(9L,
                List.of(BookCopyStatus.ON_LOAN, BookCopyStatus.RESERVED))).thenReturn(true);
    }

    private ReqCreateBookDTO createRequest() {
        return new ReqCreateBookDTO("978-0-306-40615-7", "  Domain Design  ", "  Ada Writer ",
                "  Example Press  ", 2020, "  Computing  ", new BigDecimal("12.50"),
                " https://example.org/cover.jpg ");
    }

    private ReqUpdateBookDTO updateRequest() {
        return new ReqUpdateBookDTO(4L, NORMALIZED_ISBN, "After", "New Author", "New Press", 2024,
                "Science", new BigDecimal("25.75"), "https://example.org/new.jpg");
    }

    private String catalogErrorCode(Executable action) throws Throwable {
        try {
            action.execute();
        } catch (LibraryCatalogException error) {
            return error.getCode();
        }
        throw new AssertionError("Invalid catalog operation should be rejected");
    }

    private Book book(Long id, String title, Long version) {
        Book book = new Book(null, title, "Author", null, null, null, null, null);
        book.setId(id);
        book.setVersion(version);
        return book;
    }

    private ReqUpdateBookDTO update(Long version, String title) {
        return new ReqUpdateBookDTO(version, null, title, "Author", null, null, null, null, null);
    }

    private ResBookSummaryDTO summary(Book book) {
        return new ResBookSummaryDTO(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getPublisher(), book.getPublishedYear(), book.getCategory(), book.getListPrice(),
                book.getCoverUrl(), 2L, 1L);
    }
}

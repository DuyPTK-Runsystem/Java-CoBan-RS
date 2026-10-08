package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookSummaryDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookCatalogLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCopyRepository copyRepository;

    @InjectMocks
    private BookCatalogLookupService lookupService;

    @Test
    void activeBookHidesArchivedBooksAsNotFound() {
        Book archivedBook = book();
        archivedBook.setArchivedAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        when(bookRepository.findById(12L)).thenReturn(Optional.of(archivedBook));

        assertEquals(new ExceptionDiagnostic(LibraryCatalogException.class, "BOOK_NOT_FOUND"),
                activeBookFailure(), "active lookup preserves exception type and not-found code");
    }

    @Test
    void updateLookupLocksAndHidesArchivedBooks() {
        Book archivedBook = book();
        archivedBook.setArchivedAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        when(bookRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(archivedBook));

        assertEquals(new ExceptionDiagnostic(LibraryCatalogException.class, "BOOK_NOT_FOUND"),
                updateBookFailure(), "update lookup preserves exception type and not-found code");
    }

    @Test
    void administrativeLookupCanResolveArchivedBookForHistory() {
        Book archivedBook = book();
        archivedBook.setArchivedAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        when(bookRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(archivedBook));

        assertEquals(archivedBook, lookupService.bookForUpdate(12L), "history lookup can resolve archived books");
    }

    @Test
    void summaryCountsAllCopiesAndOnlyBorrowableAvailableCopies() {
        Book book = book();
        when(copyRepository.countByBookId(12L)).thenReturn(5L);
        when(copyRepository.countByBookIdAndStatusAndReferenceOnlyFalse(12L, BookCopyStatus.AVAILABLE))
                .thenReturn(2L);

        ResBookSummaryDTO summary = lookupService.summary(book);
        SummaryProjection actual = new SummaryProjection(summary.totalCopyCount(), borrowableCountLookupCount());
        assertEquals(new SummaryProjection(5L, 1L), actual,
                "summary counts all inventory and queries available non-reference copies");
    }

    @Test
    void summaryCountsOnlyBorrowableAvailableCopies() {
        Book book = book();
        when(copyRepository.countByBookId(12L)).thenReturn(5L);
        when(copyRepository.countByBookIdAndStatusAndReferenceOnlyFalse(12L, BookCopyStatus.AVAILABLE))
                .thenReturn(2L);

        ResBookSummaryDTO summary = lookupService.summary(book);
        SummaryProjection actual = new SummaryProjection(summary.availableBorrowableCopyCount(),
                borrowableCountLookupCount());
        assertEquals(new SummaryProjection(2L, 1L), actual,
                "summary counts borrowable copies and queries available non-reference inventory");
    }

    private Book book() {
        Book book = new Book(null, "Title", "Author", null, null, null, null, null);
        book.setId(12L);
        return book;
    }

    private ExceptionDiagnostic activeBookFailure() {
        try {
            lookupService.activeBook(12L);
        } catch (LibraryCatalogException exception) {
            return new ExceptionDiagnostic(exception.getClass(), exception.getCode());
        }
        throw new AssertionError("archived book is rejected by active lookup");
    }

    private ExceptionDiagnostic updateBookFailure() {
        try {
            lookupService.activeBookForUpdate(12L);
        } catch (LibraryCatalogException exception) {
            return new ExceptionDiagnostic(exception.getClass(), exception.getCode());
        }
        throw new AssertionError("archived book is rejected by update lookup");
    }

    private long borrowableCountLookupCount() {
        return org.mockito.Mockito.mockingDetails(copyRepository).getInvocations().stream()
                .filter(invocation -> "countByBookIdAndStatusAndReferenceOnlyFalse"
                        .equals(invocation.getMethod().getName()))
                .filter(invocation -> Long.valueOf(12L).equals(invocation.getArgument(0)))
                .filter(invocation -> BookCopyStatus.AVAILABLE.equals(invocation.getArgument(1)))
                .count();
    }

    private record ExceptionDiagnostic(Class<? extends Throwable> exceptionType, String code) {
    }

    private record SummaryProjection(long countedCopies, long matchingCountQueries) {
    }
}

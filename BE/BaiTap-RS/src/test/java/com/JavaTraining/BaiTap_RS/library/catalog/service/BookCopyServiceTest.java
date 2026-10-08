package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqUpdateBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookCopyServiceTest {

    @Mock
    private BookCatalogLookupService bookLookup;

    @Mock
    private BookCopyRepository copyRepository;

    @Mock
    private BookSortResolver sortResolver;

    @Mock
    private BookCopyBatchService batchService;

    @Mock
    private LibraryCatalogAuditService auditService;

    @InjectMocks
    private BookCopyService bookCopyService;

    @Test
    void omittedShelfLocationPreservesStoredValue() {
        BookCopy copy = editableCopy();
        ReqUpdateBookCopyDTO request = new ReqUpdateBookCopyDTO();
        request.setExpectedVersion(0L);
        stubCopyForUpdate(copy);

        bookCopyService.update(copy.getBarcode(), request);

        Assertions.assertEquals(java.util.Arrays.asList(false, "Shelf A"),
                java.util.Arrays.asList(request.isShelfLocationProvided(), copy.getShelfLocation()),
                "Omitted shelf location should preserve the stored value without marking the field as provided");
    }

    @Test
    void explicitNullOrBlankShelfLocationClearsStoredValue() {
        BookCopy explicitNullCopy = editableCopy();
        ReqUpdateBookCopyDTO explicitNull = new ReqUpdateBookCopyDTO();
        explicitNull.setExpectedVersion(0L);
        explicitNull.setShelfLocation(null);
        stubCopyForUpdate(explicitNullCopy);

        bookCopyService.update(explicitNullCopy.getBarcode(), explicitNull);

        BookCopy blankCopy = editableCopy();
        ReqUpdateBookCopyDTO blank = new ReqUpdateBookCopyDTO();
        blank.setExpectedVersion(0L);
        blank.setShelfLocation("   ");
        stubCopyForUpdate(blankCopy);

        bookCopyService.update(blankCopy.getBarcode(), blank);

        Assertions.assertEquals(java.util.Arrays.asList(null, null),
                java.util.Arrays.asList(explicitNullCopy.getShelfLocation(), blankCopy.getShelfLocation()),
                "Explicit null and blank shelf locations should both clear the stored value");
    }

    @Test
    void rejectsCirculationManagedStatusTransitions() throws Throwable {
        BookCopy copy = editableCopy();
        ReqUpdateBookCopyDTO request = new ReqUpdateBookCopyDTO();
        request.setExpectedVersion(0L);
        request.setStatus(BookCopyStatus.ON_LOAN);
        stubCopyForUpdate(copy);

        Assertions.assertEquals(java.util.Arrays.asList("COPY_STATE_CONFLICT", BookCopyStatus.AVAILABLE),
                java.util.Arrays.asList(catalogErrorCode(() -> bookCopyService.update(copy.getBarcode(), request)),
                        copy.getStatus()),
                "Circulation-managed status change should fail and leave the copy available");
    }

    @Test
    void rejectedCirculationManagedTransitionDoesNotFlush() {
        BookCopy copy = editableCopy();
        ReqUpdateBookCopyDTO request = new ReqUpdateBookCopyDTO();
        request.setExpectedVersion(0L);
        request.setStatus(BookCopyStatus.ON_LOAN);
        stubCopyForUpdate(copy);

        try {
            bookCopyService.update(copy.getBarcode(), request);
            throw new AssertionError("Circulation-managed status transition should be rejected");
        } catch (LibraryCatalogException expected) {
            // The rejection is the setup for the interaction assertion below.
        }

        Mockito.verify(copyRepository, Mockito.never()).flush();
    }

    @Test
    void rejectedCirculationManagedTransitionDoesNotAudit() {
        BookCopy copy = editableCopy();
        ReqUpdateBookCopyDTO request = new ReqUpdateBookCopyDTO();
        request.setExpectedVersion(0L);
        request.setStatus(BookCopyStatus.ON_LOAN);
        stubCopyForUpdate(copy);

        try {
            bookCopyService.update(copy.getBarcode(), request);
            throw new AssertionError("Circulation-managed status transition should be rejected");
        } catch (LibraryCatalogException expected) {
            // The rejection is the setup for the interaction assertion below.
        }

        Mockito.verify(auditService, Mockito.never()).record(any(), any(), any(), any(), any());
    }

    private BookCopy editableCopy() {
        Book book = new Book(null, "Catalog QA", "Author", null, null, null, null, null);
        book.setId(40L);
        BookCopy copy = new BookCopy(book, "LIB-000000501", "Shelf A", false);
        copy.setId(501L);
        copy.setVersion(0L);
        copy.setCreatedAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        return copy;
    }

    private void stubCopyForUpdate(BookCopy copy) {
        Mockito.when(copyRepository.findBookIdByBarcode(copy.getBarcode())).thenReturn(Optional.of(40L));
        Mockito.when(bookLookup.bookForUpdate(40L)).thenReturn(copy.getBook());
        Mockito.when(copyRepository.findByBarcodeForUpdate(copy.getBarcode())).thenReturn(Optional.of(copy));
    }

    private String catalogErrorCode(Executable action) throws Throwable {
        try {
            action.execute();
        } catch (LibraryCatalogException error) {
            return error.getCode();
        }
        throw new AssertionError("Circulation-managed status transition should be rejected");
    }
}

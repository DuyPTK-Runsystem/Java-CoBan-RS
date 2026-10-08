package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqUpdateBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class BookCopyLifecycleTest {

    private static final String BOOK_COPY_ENTITY = "book_copy";
    private static final String COPY_STATE_CONFLICT = "COPY_STATE_CONFLICT";

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
    private BookCopyService service;

    @Nested
    // default access modifier required for JUnit Jupiter nested tests
    class CopyReadTests {

        @Test
        void copyListReturnsRequestedPageAndFilteredRows() {
            Pageable pageable = stubCopyList();

            ResultPaginationDTO<ResBookCopyDTO> result =
                    service.list(40L, BookCopyStatus.DAMAGED, false, 1, 2, List.of("id,asc"));

            Assertions.assertEquals(List.of(1, 2, 3, 5L, List.of("LIB-000000051")),
                    List.of(result.meta().page(), result.meta().pageSize(), result.meta().totalPages(),
                            result.meta().totalItems(), result.result().stream().map(ResBookCopyDTO::barcode).toList()),
                    "Copy page should preserve requested pagination and filtered content");
        }

        @Test
        void copyListQueriesFilteredPage() {
            Pageable pageable = stubCopyList();

            service.list(40L, BookCopyStatus.DAMAGED, false, 1, 2, List.of("id,asc"));

            Mockito.verify(copyRepository).searchByBookId(40L, BookCopyStatus.DAMAGED, false, pageable);
        }

        @Test
        void lookupDistinguishesMissingAndArchivedCopies() {
            BookCopy archived = copy(BookCopyStatus.AVAILABLE);
            archived.getBook().setArchivedAt(LocalDateTime.now());
            Mockito.when(copyRepository.findByBarcode("missing")).thenReturn(Optional.empty());
            Mockito.when(copyRepository.findByBarcode(archived.getBarcode())).thenReturn(Optional.of(archived));

            Assertions.assertEquals(List.of(List.of("COPY_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND),
                            List.of("BOOK_ARCHIVED", org.springframework.http.HttpStatus.CONFLICT)),
                    List.of(lookupFailure("missing"), lookupFailure(archived.getBarcode())),
                    "Lookup should distinguish missing copies from archived-book conflicts");
        }
    }

    @Nested
    // default access modifier required for JUnit Jupiter nested tests
    class CopyMutationTests {

        @Nested
        // default access modifier required for JUnit Jupiter nested tests
        class CopyUpdateTests {

            @Test
            void updateReturnsNormalizedEditableFields() {
                BookCopy copy = copy(BookCopyStatus.AVAILABLE);
                stubForMutation(copy);
                ReqUpdateBookCopyDTO update = request(0L);
                update.setShelfLocation(" Shelf B ");
                update.setReferenceOnly(true);
                update.setStatus(BookCopyStatus.DAMAGED);

                ResBookCopyDTO response = service.update(copy.getBarcode(), update);

                Assertions.assertEquals(List.of("Shelf B", BookCopyStatus.DAMAGED, true),
                        List.of(response.shelfLocation(), response.status(), response.referenceOnly()),
                        "Copy update should apply all requested editable fields");
            }

            @Test
            void updateFlushesMutation() {
                BookCopy copy = copy(BookCopyStatus.AVAILABLE);
                stubForMutation(copy);

                service.update(copy.getBarcode(), request(0L));

                Mockito.verify(copyRepository).flush();
            }

            @Test
            void updateRecordsBeforeAndAfterAuditSnapshots() {
                BookCopy copy = copy(BookCopyStatus.AVAILABLE);
                stubForMutation(copy);
                ReqUpdateBookCopyDTO update = request(0L);
                update.setStatus(BookCopyStatus.DAMAGED);

                service.update(copy.getBarcode(), update);

                Mockito.verify(auditService).record(org.mockito.ArgumentMatchers.eq("BOOK_COPY_UPDATED"),
                        org.mockito.ArgumentMatchers.eq(BOOK_COPY_ENTITY), org.mockito.ArgumentMatchers.eq(51L),
                        org.mockito.ArgumentMatchers.argThat(before -> before.toString().contains("AVAILABLE")),
                        org.mockito.ArgumentMatchers.argThat(after -> after.toString().contains("DAMAGED")));
            }

        }

        @Nested
        // default access modifier required for JUnit Jupiter nested tests
        class CopyWithdrawalTests {

            @Test
            void withdrawsDamagedCopy() {
                BookCopy copy = copy(BookCopyStatus.DAMAGED);
                stubForMutation(copy);

                service.withdraw(copy.getBarcode(), 0L);

                Assertions.assertEquals(BookCopyStatus.WITHDRAWN, copy.getStatus(),
                        "Withdrawal should set the terminal status");
            }

            @Test
            void damagedCopyWithdrawalFlushes() {
                BookCopy copy = copy(BookCopyStatus.DAMAGED);
                stubForMutation(copy);

                service.withdraw(copy.getBarcode(), 0L);

                Mockito.verify(copyRepository).flush();
            }

            @Test
            void damagedCopyWithdrawalAuditsStateTransition() {
                BookCopy copy = copy(BookCopyStatus.DAMAGED);
                stubForMutation(copy);

                service.withdraw(copy.getBarcode(), 0L);

                Mockito.verify(auditService).record(org.mockito.ArgumentMatchers.eq("BOOK_COPY_WITHDRAWN"),
                        org.mockito.ArgumentMatchers.eq(BOOK_COPY_ENTITY), org.mockito.ArgumentMatchers.eq(51L),
                        org.mockito.ArgumentMatchers.argThat(before -> before.toString().contains("DAMAGED")),
                        org.mockito.ArgumentMatchers.argThat(after -> after.toString().contains("WITHDRAWN")));
            }

            @Test
            void withdrawsAvailableCopyAndKeepsWithdrawnInventoryTerminal() {
                BookCopy copy = copy(BookCopyStatus.AVAILABLE);
                stubForMutation(copy);

                service.withdraw(copy.getBarcode(), 0L);

                Assertions.assertEquals(BookCopyStatus.WITHDRAWN, copy.getStatus(),
                        "Available copy should become withdrawn");
            }

            @Test
            void availableCopyWithdrawalFlushes() {
                BookCopy copy = copy(BookCopyStatus.AVAILABLE);
                stubForMutation(copy);

                service.withdraw(copy.getBarcode(), 0L);

                Mockito.verify(copyRepository).flush();
            }

            @Test
            void availableCopyWithdrawalAuditsTransition() {
                BookCopy copy = copy(BookCopyStatus.AVAILABLE);
                stubForMutation(copy);

                service.withdraw(copy.getBarcode(), 0L);

                Mockito.verify(auditService).record(org.mockito.ArgumentMatchers.eq("BOOK_COPY_WITHDRAWN"),
                        org.mockito.ArgumentMatchers.eq(BOOK_COPY_ENTITY), org.mockito.ArgumentMatchers.eq(51L),
                        org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.anyMap());
            }

        }

        @Nested
        // default access modifier required for JUnit Jupiter nested tests
        class CopyConflictTests {

            @ParameterizedTest
            @EnumSource(value = BookCopyStatus.class, names = { "ON_LOAN", "RESERVED", "LOST", "WITHDRAWN" })
            void circulationStateRejectsUpdateAndWithdrawal(BookCopyStatus state) throws Throwable {
                BookCopy copy = copy(state);
                stubForMutation(copy);
                List<String> codes = new ArrayList<>();
                codes.add(catalogErrorCode(() -> service.update(copy.getBarcode(), request(0L))));
                codes.add(catalogErrorCode(() -> service.withdraw(copy.getBarcode(), 0L)));

                Assertions.assertEquals(List.of(COPY_STATE_CONFLICT, COPY_STATE_CONFLICT), codes,
                        "Circulation and terminal states should reject edits and withdrawal");
            }

            @ParameterizedTest
            @EnumSource(value = BookCopyStatus.class, names = { "ON_LOAN", "RESERVED", "LOST", "WITHDRAWN" })
            void circulationStateDoesNotFlush(BookCopyStatus state) throws Throwable {
                BookCopy copy = copy(state);
                stubForMutation(copy);
                rejectUpdateAndWithdrawal(copy);

                Mockito.verify(copyRepository, Mockito.never()).flush();
            }

            @ParameterizedTest
            @EnumSource(value = BookCopyStatus.class, names = { "ON_LOAN", "RESERVED", "LOST", "WITHDRAWN" })
            void circulationStateDoesNotAudit(BookCopyStatus state) throws Throwable {
                BookCopy copy = copy(state);
                stubForMutation(copy);
                rejectUpdateAndWithdrawal(copy);

                Mockito.verify(auditService, Mockito.never()).record(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            }

            @Test
            void staleVersionReturnsConflictCode() throws Throwable {
                BookCopy stale = copy(BookCopyStatus.AVAILABLE);
                stale.setVersion(2L);
                stubForMutation(stale);

                Assertions.assertEquals("VERSION_CONFLICT",
                        catalogErrorCode(() -> service.update(stale.getBarcode(), request(1L))),
                        "Stale expected version should use the version conflict code");
            }

            @Test
            void staleVersionDoesNotFlush() throws Throwable {
                BookCopy stale = staleCopy();
                catalogErrorCode(() -> service.update(stale.getBarcode(), request(1L)));

                Mockito.verify(copyRepository, Mockito.never()).flush();
            }

            @Test
            void staleVersionDoesNotAudit() throws Throwable {
                BookCopy stale = staleCopy();
                catalogErrorCode(() -> service.update(stale.getBarcode(), request(1L)));

                Mockito.verify(auditService, Mockito.never()).record(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            }

        }

        @Nested
        // default access modifier required for JUnit Jupiter nested tests
        class CopyAuditFailureTests {

            @Test
            void auditFailurePropagatesAndLeavesMutationApplied() throws Throwable {
                BookCopy copy = copy(BookCopyStatus.AVAILABLE);
                stubForMutation(copy);
                Mockito.doThrow(new IllegalStateException("audit unavailable")).when(auditService)
                        .record(org.mockito.ArgumentMatchers.eq("BOOK_COPY_UPDATED"),
                                org.mockito.ArgumentMatchers.eq(BOOK_COPY_ENTITY), org.mockito.ArgumentMatchers.eq(51L),
                                org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.anyMap());
                ReqUpdateBookCopyDTO update = request(0L);
                update.setStatus(BookCopyStatus.DAMAGED);

                Assertions.assertEquals(List.of("audit unavailable", BookCopyStatus.DAMAGED),
                        List.of(runtimeFailureMessage(() -> service.update(copy.getBarcode(), update)),
                                copy.getStatus()),
                        "Audit failure should propagate after the copy mutation is flushed");
            }

            @Test
            void auditFailureOccursAfterCopyFlush() throws Throwable {
                BookCopy copy = copy(BookCopyStatus.AVAILABLE);
                stubForMutation(copy);
                Mockito.doThrow(new IllegalStateException("audit unavailable")).when(auditService)
                        .record(org.mockito.ArgumentMatchers.eq("BOOK_COPY_UPDATED"),
                                org.mockito.ArgumentMatchers.eq(BOOK_COPY_ENTITY), org.mockito.ArgumentMatchers.eq(51L),
                                org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.anyMap());
                ReqUpdateBookCopyDTO update = request(0L);
                update.setStatus(BookCopyStatus.DAMAGED);

                runtimeFailureMessage(() -> service.update(copy.getBarcode(), update));

                Mockito.verify(copyRepository).flush();
            }

        }
    }

    private Pageable stubCopyList() {
        BookCopy first = copy(BookCopyStatus.DAMAGED);
        Pageable pageable = PageRequest.of(1, 2, Sort.by("id").ascending());
        Mockito.when(bookLookup.activeBook(40L)).thenReturn(first.getBook());
        Mockito.when(sortResolver.resolveCopies(1, 2, List.of("id,asc"))).thenReturn(pageable);
        Mockito.when(copyRepository.searchByBookId(40L, BookCopyStatus.DAMAGED, false, pageable))
                .thenReturn(new PageImpl<>(List.of(first), pageable, 5));
        return pageable;
    }

    private void stubForMutation(BookCopy copy) {
        Mockito.when(copyRepository.findBookIdByBarcode(copy.getBarcode())).thenReturn(Optional.of(40L));
        Mockito.when(bookLookup.bookForUpdate(40L)).thenReturn(copy.getBook());
        Mockito.when(copyRepository.findByBarcodeForUpdate(copy.getBarcode())).thenReturn(Optional.of(copy));
    }

    private void rejectUpdateAndWithdrawal(BookCopy copy) throws Throwable {
        catalogErrorCode(() -> service.update(copy.getBarcode(), request(0L)));
        catalogErrorCode(() -> service.withdraw(copy.getBarcode(), 0L));
    }

    private BookCopy staleCopy() {
        BookCopy stale = copy(BookCopyStatus.AVAILABLE);
        stale.setVersion(2L);
        stubForMutation(stale);
        return stale;
    }

    private List<Object> lookupFailure(String barcode) {
        try {
            service.getByBarcode(barcode);
        } catch (LibraryCatalogException error) {
            return List.of(error.getCode(), error.getStatus());
        }
        throw new AssertionError("Lookup should reject this barcode");
    }

    private String catalogErrorCode(Executable action) throws Throwable {
        try {
            action.execute();
        } catch (LibraryCatalogException error) {
            return error.getCode();
        }
        throw new AssertionError("Invalid copy operation should be rejected");
    }

    private String runtimeFailureMessage(Executable action) throws Throwable {
        try {
            action.execute();
        } catch (IllegalStateException error) {
            return error.getMessage();
        }
        throw new AssertionError("Audit failure should propagate");
    }

    private ReqUpdateBookCopyDTO request(long version) {
        ReqUpdateBookCopyDTO request = new ReqUpdateBookCopyDTO();
        request.setExpectedVersion(version);
        return request;
    }

    private BookCopy copy(BookCopyStatus status) {
        Book book = new Book(null, "Lifecycle", "Author", null, null, null, null, null);
        book.setId(40L);
        BookCopy copy = new BookCopy(book, "LIB-000000051", "Shelf A", false);
        copy.setId(51L);
        copy.setVersion(0L);
        copy.setStatus(status);
        copy.setCreatedAt(LocalDateTime.now());
        return copy;
    }

}

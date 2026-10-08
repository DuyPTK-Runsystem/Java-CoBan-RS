package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyBatchDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyBatchRequest;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyBatchRequestRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookCopyBatchServiceTest {

    private static final String IDEMPOTENCY_KEY = "request-1";
    private static final String INVALID_IDEMPOTENCY_KEY_CODE = "INVALID_IDEMPOTENCY_KEY";
    private static final String SHELF_LOCATION = "Shelf A";
    private static final String BOOK_COPIES_CREATED_EVENT = "BOOK_COPIES_CREATED";
    private static final String BOUNDARY_KEY = "boundary-key";

    @Mock
    private BookCatalogLookupService bookLookup;

    @Mock
    private BookCopyRepository copyRepository;

    @Mock
    private BookCopyBatchRequestRepository batchRepository;

    @Mock
    private LibraryCatalogAuditService auditService;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private BookCopyBatchService batchService;

    private final List<BookCopyBatchRequest> storedRequests = new java.util.ArrayList<>();

    @BeforeEach
    void authenticateActor() {
        User user = new User("catalog-qa", "unused");
        ReflectionTestUtils.setField(user, "id", 31L);
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void replaysSameBatchWithoutCreatingOrAuditingAgain() {
        configureBatchCreation();

        ResBookCopyBatchDTO first = batchService.create(40L,
                new ReqCreateBookCopiesDTO(2, " " + SHELF_LOCATION + " ", false), IDEMPOTENCY_KEY);
        ResBookCopyBatchDTO replay = batchService.create(40L,
                new ReqCreateBookCopiesDTO(2, SHELF_LOCATION, false), IDEMPOTENCY_KEY);

        Assertions.assertEquals(first, replay, "replaying a normalized request must return the stored batch");
    }

    @Test
    void replayCreatesCopiesOnlyOnce() {
        configureBatchCreation();
        createSameBatchTwice();

        Mockito.verify(copyRepository, Mockito.times(1)).saveAllAndFlush(Mockito.anyList());
    }

    @Test
    void replayAuditsOnlyOnce() {
        configureBatchCreation();
        createSameBatchTwice();

        Mockito.verify(auditService, Mockito.times(1)).record(Mockito.eq(BOOK_COPIES_CREATED_EVENT),
                Mockito.eq("book"), Mockito.eq(40L), Mockito.isNull(), Mockito.anyMap());
    }

    @Test
    void rejectsDifferentPayloadForReusedKeyWithoutCreatingAgain() {
        configureBatchCreation();
        batchService.create(40L, new ReqCreateBookCopiesDTO(2, SHELF_LOCATION, false), IDEMPOTENCY_KEY);

        Assertions.assertEquals("IDEMPOTENCY_CONFLICT", differentPayloadFailureCode(),
                "a key reused with a different payload must return the conflict code");
    }

    @Test
    void differentPayloadDoesNotCreateCopiesAgain() {
        configureBatchCreation();
        createDifferentPayloadTwice();

        Mockito.verify(copyRepository, Mockito.times(1)).saveAllAndFlush(Mockito.anyList());
    }

    @Test
    void differentPayloadDoesNotAuditAgain() {
        configureBatchCreation();
        createDifferentPayloadTwice();

        Mockito.verify(auditService, Mockito.times(1)).record(Mockito.eq(BOOK_COPIES_CREATED_EVENT),
                Mockito.eq("book"), Mockito.eq(40L), Mockito.isNull(), Mockito.anyMap());
    }

    @Test
    void rejectsBlankIdempotencyKeyBeforeLockingBook() {
        Assertions.assertEquals(INVALID_IDEMPOTENCY_KEY_CODE, failureCode(1, " "),
                "a blank idempotency key must be rejected");
    }

    @Test
    void blankIdempotencyKeyDoesNotLockBook() {
        failureCode(1, " ");
        Mockito.verify(bookLookup, Mockito.never()).activeBookForUpdate(40L);
    }

    @Test
    void rejectsOverlongIdempotencyKeyBeforeLockingBook() {
        Assertions.assertEquals(INVALID_IDEMPOTENCY_KEY_CODE, failureCode(1, "k".repeat(129)),
                "an overlong idempotency key must be rejected");
    }

    @Test
    void overlongIdempotencyKeyDoesNotLockBook() {
        failureCode(1, "k".repeat(129));
        Mockito.verify(bookLookup, Mockito.never()).activeBookForUpdate(40L);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 101 })
    void rejectsQuantityOutsideApprovedRangeBeforeLockingBook(int quantity) {
        Assertions.assertEquals("INVALID_COPY_QUANTITY", failureCode(quantity, "valid-key"),
                "an out-of-range quantity must be rejected");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 101 })
    void rejectsQuantityBeforeAnyRepositoryWork(int quantity) {
        failureCode(quantity, "valid-key");
        Mockito.verifyNoInteractions(bookLookup, copyRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 100 })
    void acceptsQuantityAtApprovedBoundaries(int quantity) {
        configureBoundaryCreation();

        ResBookCopyBatchDTO result = batchService.create(40L,
                new ReqCreateBookCopiesDTO(quantity, null, false), BOUNDARY_KEY);

        Assertions.assertEquals(List.of(quantity, quantity), List.of(result.createdCount(), result.copies().size()),
                "both accepted quantity boundaries must create exactly the requested number of copies");
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 100 })
    void savesCopiesForApprovedQuantityBoundaries(int quantity) {
        configureBoundaryCreation();
        batchService.create(40L, new ReqCreateBookCopiesDTO(quantity, null, false), BOUNDARY_KEY);
        Mockito.verify(copyRepository).saveAllAndFlush(Mockito.anyList());
    }

    private void createSameBatchTwice() {
        batchService.create(40L, new ReqCreateBookCopiesDTO(2, " " + SHELF_LOCATION + " ", false), IDEMPOTENCY_KEY);
        batchService.create(40L, new ReqCreateBookCopiesDTO(2, SHELF_LOCATION, false), IDEMPOTENCY_KEY);
    }

    private String differentPayloadFailureCode() {
        return failureCode(1, IDEMPOTENCY_KEY);
    }

    private void createDifferentPayloadTwice() {
        batchService.create(40L, new ReqCreateBookCopiesDTO(2, SHELF_LOCATION, false), IDEMPOTENCY_KEY);
        failureCode(1, IDEMPOTENCY_KEY);
    }

    private String failureCode(int quantity, String idempotencyKey) {
        try {
            batchService.create(40L, new ReqCreateBookCopiesDTO(quantity, null, false), idempotencyKey);
            throw new AssertionError("Expected the batch operation to fail");
        } catch (LibraryCatalogException exception) {
            return exception.getCode();
        }
    }

    private void configureBoundaryCreation() {
        stubBookLookup();
        Mockito.when(batchRepository.findByActorUserIdAndBookIdAndIdempotencyKey(31L, 40L, BOUNDARY_KEY))
                .thenReturn(java.util.Optional.empty());
        Mockito.when(copyRepository.saveAllAndFlush(Mockito.anyList())).thenAnswer(invocation -> {
            List<BookCopy> copies = invocation.getArgument(0);
            for (int index = 0; index < copies.size(); index++) {
                copies.get(index).setId(1_000L + index);
                copies.get(index).setCreatedAt(LocalDateTime.of(2026, 10, 8, 10, 0));
            }
            return copies;
        });
        Mockito.when(batchRepository.saveAndFlush(Mockito.any(BookCopyBatchRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void configureBatchCreation() {
        stubBookLookup();
        Mockito.when(batchRepository.findByActorUserIdAndBookIdAndIdempotencyKey(31L, 40L, IDEMPOTENCY_KEY))
                .thenAnswer(invocation -> storedRequests.stream().findFirst());
        Mockito.when(copyRepository.saveAllAndFlush(Mockito.anyList())).thenAnswer(invocation -> {
            List<BookCopy> copies = invocation.getArgument(0);
            for (int index = 0; index < copies.size(); index++) {
                BookCopy copy = copies.get(index);
                copy.setId(501L + index);
                copy.setCreatedAt(LocalDateTime.of(2026, 10, 8, 10, 0));
            }
            return copies;
        });
        Mockito.when(batchRepository.saveAndFlush(Mockito.any(BookCopyBatchRequest.class)))
                .thenAnswer(invocation -> {
                    BookCopyBatchRequest request = invocation.getArgument(0);
                    storedRequests.add(request);
                    return request;
                });
    }

    private void stubBookLookup() {
        Book book = new Book(null, "Catalog QA", "Author", null, null, null, null, null);
        book.setId(40L);
        Mockito.when(bookLookup.activeBookForUpdate(40L)).thenReturn(book);
    }
}

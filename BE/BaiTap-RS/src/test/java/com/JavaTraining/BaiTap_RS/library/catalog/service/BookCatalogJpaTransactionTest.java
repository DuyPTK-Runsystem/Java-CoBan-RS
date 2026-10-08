package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.UUID;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqUpdateBookDTO;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyBatchRequestRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:library-catalog-transactions;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.ai.openai.api-key=qa-placeholder-not-a-secret",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookCatalogJpaTransactionTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookCopyBatchService batchService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private BookCopyBatchRequestRepository batchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private LibraryCatalogAuditService auditService;

    @BeforeEach
    void resetAuditMock() {
        Mockito.reset(auditService);
        // H2 JSON returns a JSON string scalar for a JDBC String binding; MySQL
        // JSON returns the stored document text. Keep this JPA test focused on
        // transactional behavior while the MySQL fixture validates native JSON.
        jdbcTemplate.execute("ALTER TABLE book_copy_batch_request ALTER COLUMN response_json "
                + "SET DATA TYPE VARCHAR(1000000)");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bookUpdateReturnsTheVersionWrittenByJpaFlush() {
        Book book = saveBook("Versioned book");

        com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookDetailDTO result = bookService.update(book.getId(), new ReqUpdateBookDTO(book.getVersion(), null,
                "Updated versioned book", "Author", null, null, null, null, null));

        VersionProjection actual = new VersionProjection(result.version(),
                bookRepository.findById(book.getId()).orElseThrow().getVersion());
        Assertions.assertEquals(new VersionProjection(1L, 1L), actual,
                "update response and database row retain the incremented version");
    }

    @Test
    void matchingBatchRetryReturnsOriginalCopies() {
        Book book = saveBook("Idempotent book");
        Long actorId = authenticateAsNewActor();
        String key = UUID.randomUUID().toString();

        com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyBatchDTO first = batchService.create(
                book.getId(), new ReqCreateBookCopiesDTO(2, " Shelf A ", false), key);
        com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyBatchDTO replay = batchService.create(
                book.getId(), new ReqCreateBookCopiesDTO(2, "Shelf A", false), key);

        BatchSuccessProjection actual = new BatchSuccessProjection(first.equals(replay), first.createdCount(),
                first.copies().size(), copyRepository.countByBookId(book.getId()),
                batchRepository.findByActorUserIdAndBookIdAndIdempotencyKey(actorId, book.getId(), key).isPresent(),
                auditRecordCount());
        Assertions.assertEquals(new BatchSuccessProjection(true, 2, 2, 2L, true, 1L), actual,
                "retry returns the original batch, persists one idempotency record, and audits once");

    }

    @Test
    void mismatchedBatchRetryConflictsWithoutCreatingCopies() {
        Book book = saveBook("Idempotent conflict book");
        authenticateAsNewActor();
        String key = UUID.randomUUID().toString();
        batchService.create(book.getId(), new ReqCreateBookCopiesDTO(2, "Shelf A", false), key);
        batchService.create(book.getId(), new ReqCreateBookCopiesDTO(2, "Shelf A", false), key);

        ExceptionDiagnostic conflict = batchConflict(book, key);
        ConflictProjection actual = new ConflictProjection(conflict, copyRepository.countByBookId(book.getId()));
        Assertions.assertEquals(new ConflictProjection(
                new ExceptionDiagnostic(LibraryCatalogException.class, "IDEMPOTENCY_CONFLICT"), 2L),
                actual, "mismatched retry conflicts without changing copy count");
    }

    @Test
    void auditFailureRollsBackTheBatchCopiesAndIdempotencyRecord() {
        Book book = saveBook("Rollback book");
        Long actorId = authenticateAsNewActor();
        String key = UUID.randomUUID().toString();
        Mockito.doThrow(new IllegalStateException("forced audit failure")).when(auditService).record(
                ArgumentMatchers.eq("BOOK_COPIES_CREATED"), ArgumentMatchers.eq("book"), ArgumentMatchers.eq(book.getId()), ArgumentMatchers.isNull(), ArgumentMatchers.anyMap());

        ExceptionDiagnostic failure = auditFailure(book, key);
        RollbackProjection actual = new RollbackProjection(failure, copyRepository.countByBookId(book.getId()),
                batchRepository.findByActorUserIdAndBookIdAndIdempotencyKey(actorId, book.getId(), key).isPresent());
        Assertions.assertEquals(new RollbackProjection(new ExceptionDiagnostic(IllegalStateException.class,
                "forced audit failure"), 0L, false), actual,
                "audit failure propagates and rolls back both copies and idempotency record");
    }

    private Book saveBook(String title) {
        return bookRepository.saveAndFlush(new Book(null, title, "Author", null, null, null, null, null));
    }

    private Long authenticateAsNewActor() {
        String username = "qa-" + UUID.randomUUID().toString().substring(0, 12);
        User actor = userRepository.saveAndFlush(new User(username, "unused"));
        UserPrincipal principal = new UserPrincipal(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return actor.getId();
    }

    private ExceptionDiagnostic auditFailure(Book book, String key) {
        try {
            batchService.create(book.getId(), new ReqCreateBookCopiesDTO(2, null, false), key);
        } catch (IllegalStateException exception) {
            return new ExceptionDiagnostic(exception.getClass(), exception.getMessage());
        }
        throw new AssertionError("audit failure propagates from batch creation");
    }

    private ExceptionDiagnostic batchConflict(Book book, String key) {
        try {
            batchService.create(book.getId(), new ReqCreateBookCopiesDTO(1, "Shelf A", false), key);
        } catch (LibraryCatalogException exception) {
            return new ExceptionDiagnostic(exception.getClass(), exception.getCode());
        }
        throw new AssertionError("mismatched retry throws a catalog conflict");
    }

    private long auditRecordCount() {
        return Mockito.mockingDetails(auditService).getInvocations().stream()
                .filter(invocation -> "record".equals(invocation.getMethod().getName()))
                .count();
    }

    private record VersionProjection(long responseVersion, long persistedVersion) {
    }

    private record BatchSuccessProjection(boolean replayMatches, int createdCount, int responseCopyCount,
            long persistedCopyCount, boolean idempotencyRecordPresent, long auditRecordCount) {
    }

    private record ConflictProjection(ExceptionDiagnostic exception, long persistedCopyCount) {
    }

    private record RollbackProjection(ExceptionDiagnostic exception, long persistedCopyCount,
            boolean idempotencyRecordPresent) {
    }

    private record ExceptionDiagnostic(Class<? extends Throwable> exceptionType, String detail) {
    }
}

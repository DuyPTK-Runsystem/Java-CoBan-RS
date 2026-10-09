package com.JavaTraining.BaiTap_RS.library;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.JavaTraining.BaiTap_RS.BaiTapRsApplication;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqIssueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqReissueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.service.LibraryCardService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests.ReqActivateLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.service.LibraryPatronService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/** Isolated runner for Plan 097 MySQL service transaction and concurrency evidence. */
public final class Plan097MySqlRuntimeProbe {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private Plan097MySqlRuntimeProbe() {
    }

    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(BaiTapRsApplication.class);
        application.setAdditionalProfiles("qa");
        String databaseUrl = System.getenv("PLAN097_MYSQL_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("PLAN097_MYSQL_URL is required for this isolated probe");
        }
        try (ConfigurableApplicationContext context = application.run(
                "--spring.datasource.url=" + databaseUrl,
                "--spring.datasource.username=root",
                "--spring.datasource.password=",
                "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.flyway.enabled=true",
                "--server.port=0",
                "--app.library.card.hmac-secret=plan097-isolated-test-hmac-secret-32-bytes")) {
            verifyConcurrentActivation(context.getBean(LibraryPatronService.class),
                    context.getBean(JdbcTemplate.class));
            verifyConcurrentIssueAndReissueRollback(
                    context.getBean(LibraryPatronService.class),
                    context.getBean(LibraryCardService.class),
                    context.getBean(JdbcTemplate.class));
        }
        System.out.println("PASS: Spring service duplicate activation race, issue race, and reissue rollback "
                + "verified on MySQL.");
    }

    private static void verifyConcurrentActivation(LibraryPatronService patronService, JdbcTemplate jdbcTemplate)
            throws Exception {
        Long userId = createTestUser(jdbcTemplate, "activation");
        List<Outcome> outcomes = race(() -> patronService.activate(new ReqActivateLibraryPatronDTO(userId)));
        requireOneSuccessAndOneConflict(outcomes, "PATRON_ALREADY_EXISTS");
        Integer patronCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM library_patron WHERE user_id = ?", Integer.class, userId);
        require(patronCount != null && patronCount == 1, "duplicate activation persisted more than one patron");
    }

    private static void verifyConcurrentIssueAndReissueRollback(LibraryPatronService patronService,
            LibraryCardService cardService, JdbcTemplate jdbcTemplate) throws Exception {
        Long userId = createTestUser(jdbcTemplate, "card");
        var patron = patronService.activate(new ReqActivateLibraryPatronDTO(userId));
        LocalDate expiry = LocalDate.now(LIBRARY_ZONE).plusDays(60);
        List<Outcome> issues = race(() -> cardService.issue(new ReqIssueLibraryCardDTO(patron.patronId(), expiry)));
        requireOneSuccessAndOneConflict(issues, "CARD_ALREADY_ACTIVE");
        String cardNo = jdbcTemplate.queryForObject(
                "SELECT card_no FROM library_card WHERE patron_id = ? AND status = 'ACTIVE'",
                String.class, patron.patronId());
        require(cardNo != null, "successful issue did not persist an active card");

        int year = LocalDate.now(LIBRARY_ZONE).getYear();
        jdbcTemplate.update("UPDATE library_card_sequence SET sequence_value = 999999 WHERE sequence_year = ?", year);
        try {
            cardService.reissue(cardNo, new ReqReissueLibraryCardDTO(expiry.plusDays(30), "Rollback probe"));
            throw new IllegalStateException("reissue unexpectedly succeeded with exhausted sequence");
        } catch (LibraryPatronException exception) {
            require("CARD_SEQUENCE_EXHAUSTED".equals(exception.getCode()),
                    "unexpected reissue failure: " + exception.getCode());
        }
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM library_card WHERE patron_id = ? AND status = 'ACTIVE'",
                Integer.class, patron.patronId());
        Integer cardCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM library_card WHERE patron_id = ?", Integer.class, patron.patronId());
        require(activeCount != null && activeCount == 1 && cardCount != null && cardCount == 1,
                "failed reissue did not roll back old-card revoke and replacement insert atomically");
    }

    private static Long createTestUser(JdbcTemplate jdbcTemplate, String label) {
        String nonce = Long.toString(System.nanoTime(), 36);
        String username = "q" + label.charAt(0) + nonce.substring(Math.max(0, nonce.length() - 10));
        jdbcTemplate.update("INSERT INTO app_user (user_name, password, created_at) VALUES (?, ?, NOW(6))",
                username, "unused-test-password");
        return jdbcTemplate.queryForObject("SELECT user_id FROM app_user WHERE user_name = ?", Long.class, username);
    }

    private static List<Outcome> race(ThrowingOperation operation) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Future<Outcome> first = executor.submit(() -> runAfter(start, operation));
            Future<Outcome> second = executor.submit(() -> runAfter(start, operation));
            start.countDown();
            return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
    }

    private static Outcome runAfter(CountDownLatch start, ThrowingOperation operation) throws Exception {
        require(start.await(5, TimeUnit.SECONDS), "concurrency start latch timed out");
        try {
            operation.run();
            return new Outcome(true, null);
        } catch (LibraryPatronException exception) {
            return new Outcome(false, exception.getCode());
        }
    }

    private static void requireOneSuccessAndOneConflict(List<Outcome> outcomes, String expectedCode) {
        long successes = outcomes.stream().filter(Outcome::success).count();
        long expectedConflicts = outcomes.stream()
                .filter(outcome -> expectedCode.equals(outcome.errorCode())).count();
        require(successes == 1 && expectedConflicts == 1,
                "expected one success and one " + expectedCode + ", got " + outcomes);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws Exception;
    }

    private record Outcome(boolean success, String errorCode) { }
}

package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryLostServiceTest {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Long COPY_ID = 31L;
    private static final Long LOAN_ID = 22L;

    @Mock private BookCopyRepository copyRepository;
    @Mock private BookRepository bookRepository;
    @Mock private LibraryLoanRepository loanRepository;
    @Mock private LibraryPatronRepository patronRepository;
    @Mock private LibraryPolicyService policyService;
    @Mock private LibraryFineService fineService;
    @Mock private LibraryCirculationMapper mapper;
    @Mock private LibraryOperationAuditService auditService;

    @InjectMocks
    private LibraryLostService service;

    private BookCopy copy;
    private LibraryLoan loan;
    private LibraryCirculationPolicy policy;

    @BeforeEach
    void setUp() {
        User user = new User("librarian", "not-a-real-password");
        ReflectionTestUtils.setField(user, "id", 17L);
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));

        Book book = new Book("9780000000002", "Lost book", "Author", "Publisher", 2020, "Fiction",
                new BigDecimal("123456.78"), null);
        ReflectionTestUtils.setField(book, "id", 12L);
        copy = new BookCopy(book, "LOST-31", "Shelf", false);
        ReflectionTestUtils.setField(copy, "id", COPY_ID);
        loan = new LibraryLoan(7L, COPY_ID, 51L, LocalDateTime.now().minusDays(25),
                LocalDateTime.now().minusDays(5), "LIB-POL-1", 5, 14, 2, 7);
        ReflectionTestUtils.setField(loan, "id", LOAN_ID);
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(5, 14, 2, 7, 3,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
        policy = new LibraryCirculationPolicy(new LibraryCirculationPolicySnapshot(
                "LIB-POL-1", now, terms, 17L, now));
        when(copyRepository.findBookIdByBarcode("LOST-31")).thenReturn(Optional.of(12L));
        when(bookRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(copy.getBook()));
        when(copyRepository.findByBarcodeForUpdate("LOST-31")).thenReturn(Optional.of(copy));
        when(loanRepository.findFirstByCopyIdAndStatus(COPY_ID, LoanStatus.ACTIVE)).thenReturn(Optional.of(loan));
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        LibraryPatron patron = new LibraryPatron(17L, now);
        ReflectionTestUtils.setField(patron, "id", 7L);
        when(patronRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(patron));
        when(policyService.resolveCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(policy);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void freezesOverdueCalculationAtLossDateAndUsesCopyListPriceForLostCharge() {
        LocalDate lossDate = LocalDate.now(LIBRARY_ZONE);

        service.markLost("LOST-31", "confirmed missing");

        org.junit.jupiter.api.Assertions.assertEquals(LoanStatus.LOST, loan.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(BookCopyStatus.LOST, copy.getStatus());
        verify(fineService).recalculateOverdue(eq(loan), eq(lossDate), eq(false), eq(policy));
        verify(fineService).createLostFine(eq(loan), eq(new BigDecimal("123456.78")), eq(lossDate), eq(policy));
    }
}

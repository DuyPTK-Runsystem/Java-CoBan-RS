package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryFineRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRenewalRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryLoanRenewalPriorityTest {

    private static final Long LOAN_ID = 42L;
    private static final Long PATRON_ID = 7L;
    private static final Long COPY_ID = 31L;
    private static final Long BOOK_ID = 12L;

    @Mock private LibraryLoanRepository loanRepository;
    @Mock private LibraryLoanRenewalRepository renewalRepository;
    @Mock private LibraryReservationRepository reservationRepository;
    @Mock private LibraryFineRepository fineRepository;
    @Mock private BookCopyRepository copyRepository;
    @Mock private BookRepository bookRepository;
    @Mock private LibraryPatronRepository patronRepository;
    @Mock private UserRepository userRepository;
    @Mock private LibraryPolicyService policyService;
    @Mock private LibraryFineService fineService;
    @Mock private LibraryCirculationMapper mapper;
    @Mock private LibraryOperationAuditService auditService;
    @Mock private LibraryLoanRenewalEligibilityService eligibilityService;
    @Mock private LibraryInAppNotificationService notificationService;

    @InjectMocks
    private LibraryLoanRenewalService service;

    @Test
    void refusesRenewalWhenAnotherPatronIsWaitingForTheBook() {
        LocalDateTime now = LocalDateTime.now();
        LibraryLoan loan = new LibraryLoan(PATRON_ID, COPY_ID, 51L, now.minusDays(5), now.plusDays(9),
                "LIB-POL-1", 5, 14, 2, 7);
        ReflectionTestUtils.setField(loan, "id", LOAN_ID);
        LibraryPatron patron = new LibraryPatron(17L, now);
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
        Book book = new Book("9780000000003", "Title", "Author", "Publisher", 2020, "Fiction",
                new BigDecimal("100000.00"), null);
        ReflectionTestUtils.setField(book, "id", BOOK_ID);
        BookCopy copy = new BookCopy(book, "COPY-31", "Shelf", false);
        ReflectionTestUtils.setField(copy, "id", COPY_ID);
        LibraryReservation waiting = new LibraryReservation(BOOK_ID, 8L, now.minusDays(1), "LIB-POL-1", 3);
        ReflectionTestUtils.setField(waiting, "id", 99L);
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(5, 14, 2, 7, 3,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
        LibraryCirculationPolicy policy = new LibraryCirculationPolicy(new LibraryCirculationPolicySnapshot(
                "LIB-POL-2", now, terms, 17L, now));

        LibraryLoanRepository.LoanLockInfo lockInfo = org.mockito.Mockito.mock(LibraryLoanRepository.LoanLockInfo.class);
        when(lockInfo.getCopyId()).thenReturn(COPY_ID);
        when(lockInfo.getPatronId()).thenReturn(PATRON_ID);
        when(loanRepository.findLockInfoById(LOAN_ID)).thenReturn(Optional.of(lockInfo));
        when(copyRepository.findBookIdById(COPY_ID)).thenReturn(Optional.of(BOOK_ID));
        when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));
        when(copyRepository.findByIdForUpdate(COPY_ID)).thenReturn(Optional.of(copy));
        when(copyRepository.findById(COPY_ID)).thenReturn(Optional.of(copy));
        when(policyService.resolveCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(policy);
        when(reservationRepository.findQueue(BOOK_ID, ReservationStatus.WAITING)).thenReturn(List.of(waiting));

        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.renew(LOAN_ID));

        assertEquals("COPY_RESERVED", exception.getCode());
        assertEquals(0, loan.getRenewCount());
        verify(renewalRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditService, never()).record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}

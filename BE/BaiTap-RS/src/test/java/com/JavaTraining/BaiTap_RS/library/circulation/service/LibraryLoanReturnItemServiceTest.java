package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryLoanReturnItemServiceTest {

    private static final Long COPY_ID = 31L;
    private static final Long LOAN_ID = 22L;
    private static final LocalDate RETURN_DATE = LocalDate.of(2026, 10, 9);
    private static final LocalDateTime RETURNED_AT = RETURN_DATE.atTime(15, 30);

    @Mock private LibraryLoanRepository loanRepository;
    @Mock private LibraryFineService fineService;
    @Mock private LibraryCirculationMapper mapper;
    @Mock private LibraryReservationAllocationService allocationService;
    @Mock private LibraryOperationAuditService auditService;

    @InjectMocks
    private LibraryLoanReturnItemService service;

    @Test
    void closesActiveLoanAndFinalizesOverdueFineAtReturnDate() {
        LibraryLoan loan = new LibraryLoan(7L, COPY_ID, 51L, RETURNED_AT.minusDays(20),
                RETURNED_AT.plusDays(4), "LIB-POL-1", 5, 14, 2, 7);
        ReflectionTestUtils.setField(loan, "id", LOAN_ID);
        Book book = new Book("9780000000001", "Title", "Author", "Publisher", 2020, "Fiction",
                new BigDecimal("100000.00"), null);
        BookCopy copy = new BookCopy(book, "COPY-31", "Shelf", false);
        ReflectionTestUtils.setField(copy, "id", COPY_ID);
        copy.setStatus(BookCopyStatus.ON_LOAN);
        LibraryCirculationPolicy policy = policy();
        when(loanRepository.findFirstByCopyIdAndStatus(COPY_ID, LoanStatus.ACTIVE)).thenReturn(Optional.of(loan));

        service.returnCopy(copy, RETURN_DATE, RETURNED_AT, policy);

        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        assertEquals(RETURNED_AT, loan.getReturnedAt());
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
        verify(fineService).recalculateOverdue(loan, RETURN_DATE, false, policy);
        verify(allocationService).allocateNext(eq(copy), eq(RETURNED_AT), nullable(Long.class));
        verify(auditService).record("LOAN_RETURNED", "library_loan", LOAN_ID, null,
                java.util.Map.of("copyId", COPY_ID, "returnedAt", RETURNED_AT.toString()));
    }

    private LibraryCirculationPolicy policy() {
        LocalDateTime now = LocalDateTime.now();
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(5, 14, 2, 7, 3,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
        return new LibraryCirculationPolicy(new LibraryCirculationPolicySnapshot(
                "LIB-POL-1", now, terms, 1L, now));
    }
}

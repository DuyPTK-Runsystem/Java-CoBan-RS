package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryLoanBorrowValidationServiceTest {

    private static final Long PATRON_ID = 4L;
    private static final Long BOOK_ID = 101L;
    private static final Long COPY_ID = 31L;

    @Mock private LibraryLoanRepository loanRepository;
    @Mock private LibraryReservationRepository reservationRepository;

    @InjectMocks
    private LibraryLoanBorrowValidationService service;

    @Test
    void rejectsBorrowThatWouldExceedTheEffectiveActiveLoanLimit() {
        LibraryPatron patron = patron();
        when(loanRepository.findAllByPatronIdAndStatusForUpdate(PATRON_ID, LoanStatus.ACTIVE))
                .thenReturn(List.of(loan(1L), loan(2L), loan(3L), loan(4L)));

        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.validateLoanCapacity(patron, 2, policy(5)));

        assertEquals("MAX_ACTIVE_LOANS", exception.getCode());
    }

    @Test
    void rejectsCopyThatIsAlreadyOnLoanEvenWhenNoReservationWaits() {
        BookCopy copy = copy();
        copy.setStatus(BookCopyStatus.ON_LOAN);
        when(reservationRepository.findReadyByPatronAndCopyForUpdate(PATRON_ID, COPY_ID,
                com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus.READY))
                .thenReturn(Optional.empty());

        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.validateCopyForBorrow(copy, PATRON_ID));

        assertEquals("COPY_ALREADY_ON_LOAN", exception.getCode());
    }

    @Test
    void acceptsAvailableCopyWhenNoReservationHasPriority() {
        BookCopy copy = copy();
        when(reservationRepository.findReadyByPatronAndCopyForUpdate(PATRON_ID, COPY_ID,
                com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus.READY))
                .thenReturn(Optional.empty());
        when(reservationRepository.findQueue(BOOK_ID,
                com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus.WAITING))
                .thenReturn(List.of());

        assertNull(service.validateCopyForBorrow(copy, PATRON_ID));
    }

    private LibraryPatron patron() {
        LibraryPatron patron = new LibraryPatron(15L, LocalDateTime.now());
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
        return patron;
    }

    private LibraryLoan loan(Long id) {
        LibraryLoan loan = new LibraryLoan(PATRON_ID, id, 51L, LocalDateTime.now(), LocalDateTime.now().plusDays(14),
                "LIB-POL-1", 5, 14, 2, 7);
        ReflectionTestUtils.setField(loan, "id", id);
        return loan;
    }

    private BookCopy copy() {
        Book book = new Book("9780000000001", "Title", "Author", "Publisher", 2020, "Fiction",
                new BigDecimal("100000.00"), null);
        ReflectionTestUtils.setField(book, "id", BOOK_ID);
        BookCopy copy = new BookCopy(book, "COPY-31", "Shelf", false);
        ReflectionTestUtils.setField(copy, "id", COPY_ID);
        return copy;
    }

    private LibraryCirculationPolicy policy(int maxLoans) {
        LocalDateTime now = LocalDateTime.now();
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(maxLoans, 14, 2, 7, 3,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
        return new LibraryCirculationPolicy(new LibraryCirculationPolicySnapshot(
                "LIB-POL-1", now, terms, 1L, now));
    }
}

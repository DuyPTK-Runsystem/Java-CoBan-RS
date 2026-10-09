package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqBorrowCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.RoleCode;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryLoanBorrowPreparationServiceTest {

    private static final Long PATRON_ID = 4L;
    private static final Long USER_ID = 15L;
    private static final String CARD_NO = "CARD-15";

    @Mock private BookCopyRepository copyRepository;
    @Mock private BookRepository bookRepository;
    @Mock private LibraryPatronRepository patronRepository;
    @Mock private LibraryCardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private LibraryPolicyService policyService;
    @Mock private LibraryLoanRepository loanRepository;
    @Mock private LibraryReservationRepository reservationRepository;

    private LibraryLoanBorrowPreparationService service;
    private LibraryPatron patron;

    @BeforeEach
    void setUp() {
        LibraryBorrowerEligibilityService eligibilityService =
                new LibraryBorrowerEligibilityService(cardRepository, userRepository);
        LibraryLoanBorrowValidationService validationService =
                new LibraryLoanBorrowValidationService(loanRepository, reservationRepository);
        service = new LibraryLoanBorrowPreparationService(copyRepository, bookRepository, patronRepository,
                eligibilityService, policyService, validationService);
        patron = new LibraryPatron(USER_ID, LocalDateTime.now());
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));
    }

    @Test
    void rejectsSuspendedPatronBeforeCardLookupOrCopyLocking() {
        patron.setStatus(LibraryPatronStatus.BORROWING_SUSPENDED, LocalDateTime.now());
        Book book = book(101L);
        when(copyRepository.findBookIdByBarcode("COPY-1")).thenReturn(Optional.of(101L));
        when(bookRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(book));

        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.prepare(new ReqBorrowCopiesDTO(PATRON_ID, CARD_NO, List.of("COPY-1"))));

        assertEquals("PATRON_BORROWING_SUSPENDED", exception.getCode());
        verify(cardRepository, never()).findByCardNoForUpdate(any());
        verify(copyRepository, never()).findAllByBarcodesForUpdate(any());
    }

    @Test
    void rejectsBatchThatWouldExceedConfiguredLoanLimitBeforeReturningContext() {
        User user = new User("reader", "not-a-real-password");
        user.addRole(new Role(RoleCode.STUDENT.code(), RoleCode.STUDENT.name(), null));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cardRepository.findByCardNoForUpdate(CARD_NO)).thenReturn(Optional.of(
                new LibraryCard(PATRON_ID, CARD_NO, LocalDateTime.now(), LocalDate.now().plusDays(1), "v1", "test")));
        when(copyRepository.findBookIdByBarcode("COPY-1")).thenReturn(Optional.of(101L));
        when(copyRepository.findBookIdByBarcode("COPY-2")).thenReturn(Optional.of(102L));
        when(bookRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(book(101L)));
        when(bookRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(book(102L)));
        when(copyRepository.findAllByBarcodesForUpdate(List.of("COPY-1", "COPY-2")))
                .thenReturn(List.of(copy(101L), copy(102L)));
        when(policyService.resolveCurrent(any())).thenReturn(policy(5));
        when(loanRepository.findAllByPatronIdAndStatusForUpdate(PATRON_ID, LoanStatus.ACTIVE))
                .thenReturn(List.of(loan(11L), loan(12L), loan(13L), loan(14L)));

        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.prepare(new ReqBorrowCopiesDTO(PATRON_ID, CARD_NO,
                        List.of("COPY-1", "COPY-2"))));

        assertEquals("MAX_ACTIVE_LOANS", exception.getCode());
        verify(copyRepository).findAllByBarcodesForUpdate(List.of("COPY-1", "COPY-2"));
    }

    private Book book(Long id) {
        Book book = new Book("9780000000001", "Title", "Author", "Publisher", 2020, "Fiction",
                new BigDecimal("100000.00"), null);
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private BookCopy copy(Long id) {
        return new BookCopy(book(id), "COPY-" + id, "Shelf", false);
    }

    private LibraryLoan loan(Long copyId) {
        return new LibraryLoan(PATRON_ID, copyId, 51L, LocalDateTime.now(), LocalDateTime.now().plusDays(14),
                "LIB-POL-1", 5, 14, 2, 7);
    }

    private LibraryCirculationPolicy policy(int maxLoans) {
        LocalDateTime now = LocalDateTime.now();
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(maxLoans, 14, 2, 7, 3,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
        return new LibraryCirculationPolicy(new LibraryCirculationPolicySnapshot(
                "LIB-POL-1", now, terms, 1L, now));
    }
}

package com.JavaTraining.BaiTap_RS.library.circulation.service;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.patron.service.LibraryEligibilityService;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryReservationCommandEligibilityServiceTest {

    private static final Long BOOK_ID = 101L;
    private static final Long USER_ID = 17L;
    private static final Long PATRON_ID = 12L;
    private static final List<ReservationStatus> ACTIVE = List.of(ReservationStatus.WAITING, ReservationStatus.READY);

    @Mock private LibraryReservationRepository reservationRepository;
    @Mock private LibraryPatronRepository patronRepository;
    @Mock private LibraryEligibilityService eligibilityService;
    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository copyRepository;
    @Mock private LibraryAccessPolicy accessPolicy;

    private LibraryReservationCommandEligibilityService service;
    private LibraryPatron patron;

    @BeforeEach
    void setUp() {
        service = new LibraryReservationCommandEligibilityService(reservationRepository, patronRepository,
                eligibilityService, bookRepository, copyRepository, accessPolicy);
        Book book = Mockito.mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);
        patron = Mockito.mock(LibraryPatron.class);
        when(eligibilityService.assertCanBorrow(USER_ID, false))
                .thenReturn(new LibraryEligibilityService.BorrowingEligibility(USER_ID, PATRON_ID, null));
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));
        when(patron.getStatus()).thenReturn(LibraryPatronStatus.ACTIVE);
    }

    @Test
    void allowsCreationWhenNoAvailableCopyOrActiveDuplicateExists() {
        when(patron.getId()).thenReturn(PATRON_ID);
        assertEquals(PATRON_ID, service.lockAndValidateCreationEligibility(BOOK_ID, USER_ID));
        when(copyRepository.findAvailableCopiesForUpdate(BOOK_ID, BookCopyStatus.AVAILABLE)).thenReturn(List.of());
        when(reservationRepository.findActiveByPatronAndBookForUpdate(PATRON_ID, BOOK_ID, ACTIVE))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.validateCreationAvailabilityAndDuplicate(BOOK_ID, PATRON_ID));
    }

    @Test
    void rejectsCreationWhenAnAvailableCopyRemainsAfterExpiry() {
        service.lockAndValidateCreationEligibility(BOOK_ID, USER_ID);
        when(copyRepository.findAvailableCopiesForUpdate(BOOK_ID, BookCopyStatus.AVAILABLE))
                .thenReturn(List.of(Mockito.mock(com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy.class)));

        LibraryCirculationException error = assertThrows(LibraryCirculationException.class,
                () -> service.validateCreationAvailabilityAndDuplicate(BOOK_ID, PATRON_ID));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("RESERVATION_NOT_NEEDED", error.getCode());
    }

    @Test
    void rejectsCreationWhenWaitingOrReadyDuplicateRemainsAfterExpiry() {
        service.lockAndValidateCreationEligibility(BOOK_ID, USER_ID);
        when(copyRepository.findAvailableCopiesForUpdate(BOOK_ID, BookCopyStatus.AVAILABLE)).thenReturn(List.of());
        when(reservationRepository.findActiveByPatronAndBookForUpdate(PATRON_ID, BOOK_ID, ACTIVE))
                .thenReturn(List.of(Mockito.mock(com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation.class)));

        LibraryCirculationException error = assertThrows(LibraryCirculationException.class,
                () -> service.validateCreationAvailabilityAndDuplicate(BOOK_ID, PATRON_ID));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("RESERVATION_ALREADY_EXISTS", error.getCode());
    }
}

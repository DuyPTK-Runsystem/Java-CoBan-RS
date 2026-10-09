package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryReservationExpirationServiceTest {

    private static final Long BOOK_ID = 101L;
    private static final Long COPY_ID = 31L;
    private static final Long RESERVATION_ID = 42L;
    private static final Long ACTOR_ID = 17L;

    @Mock private LibraryReservationRepository reservationRepository;
    @Mock private BookCopyRepository copyRepository;
    @Mock private BookRepository bookRepository;
    @Mock private LibraryReservationAllocationService allocationService;
    @Mock private LibraryOperationAuditService auditService;

    @InjectMocks
    private LibraryReservationExpirationService service;

    @Test
    void expiresReadyReservationAndMakesItsAllocatedCopyAvailable() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 10, 0, 0);
        LibraryReservation reservation = new LibraryReservation(BOOK_ID, 8L, now.minusDays(4), "LIB-POL-1", 3);
        reservation.ready(COPY_ID, now.minusDays(3), now);
        ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);
        Book book = new Book("9780000000001", "Title", "Author", "Publisher", 2020, "Fiction",
                new BigDecimal("100000.00"), null);
        BookCopy copy = new BookCopy(book, "COPY-31", "Shelf", false);
        ReflectionTestUtils.setField(copy, "id", COPY_ID);
        copy.setStatus(BookCopyStatus.RESERVED);
        when(reservationRepository.findExpiredByBookForUpdate(BOOK_ID, ReservationStatus.READY, now))
                .thenReturn(List.of(reservation));
        when(copyRepository.findByIdForUpdate(COPY_ID)).thenReturn(Optional.of(copy));

        int expiredCount = service.expireForBook(BOOK_ID, now, ACTOR_ID);

        assertEquals(1, expiredCount);
        assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
        assertEquals(now, reservation.getExpiredAt());
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
        verify(allocationService).allocateNext(copy, now, ACTOR_ID);
        verify(auditService).record(ACTOR_ID, "RESERVATION_EXPIRED", "library_reservation", RESERVATION_ID,
                null, java.util.Map.of("expiredAt", now.toString()));
    }
}

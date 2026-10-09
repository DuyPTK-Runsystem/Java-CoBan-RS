package com.JavaTraining.BaiTap_RS.library.circulation.service;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryReservationAllocationServiceTest {

    private static final Long BOOK_ID = 101L;
    private static final Long COPY_ID = 31L;
    private static final Long ACTOR_ID = 17L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 0, 0);

    @Mock private LibraryReservationRepository reservationRepository;
    @Mock private LibraryPatronRepository patronRepository;
    @Mock private LibraryOperationAuditService auditService;
    @Mock private LibraryInAppNotificationService notificationService;

    @Test
    void allocatesReleasedCopyToFirstEligibleReservationInFifoQueue() {
        LibraryReservationAllocationService service = new LibraryReservationAllocationService(
                reservationRepository, patronRepository, auditService, notificationService);
        LibraryReservation first = reservation(21L, 8L);
        LibraryReservation second = reservation(22L, 9L);
        LibraryPatron firstPatron = new LibraryPatron(108L, NOW.minusDays(5));
        ReflectionTestUtils.setField(firstPatron, "id", 8L);
        Book book = new Book("9780000000001", "Title", "Author", "Publisher", 2020, "Fiction",
                new BigDecimal("100000.00"), null);
        ReflectionTestUtils.setField(book, "id", BOOK_ID);
        BookCopy copy = new BookCopy(book, "COPY-31", "Shelf", false);
        ReflectionTestUtils.setField(copy, "id", COPY_ID);
        when(reservationRepository.findQueue(BOOK_ID, ReservationStatus.WAITING)).thenReturn(List.of(first, second));
        when(patronRepository.findById(8L)).thenReturn(Optional.of(firstPatron));

        service.allocateNext(copy, NOW, ACTOR_ID);

        assertEquals(ReservationStatus.READY, first.getStatus());
        assertEquals(ReservationStatus.WAITING, second.getStatus());
        assertEquals(COPY_ID, first.getAllocatedCopyId());
        assertEquals(BookCopyStatus.RESERVED, copy.getStatus());
        verify(reservationRepository).findQueue(BOOK_ID, ReservationStatus.WAITING);
        org.mockito.Mockito.verify(patronRepository, org.mockito.Mockito.never()).findById(9L);
        verify(notificationService).publish("library-reservation-ready-21", "Sách đã sẵn sàng để nhận",
                "Đầu sách “Title” đã sẵn sàng. Hạn nhận: " + NOW.toLocalDate().plusDays(4), 108L, ACTOR_ID);
    }

    private LibraryReservation reservation(Long id, Long patronId) {
        LibraryReservation reservation = new LibraryReservation(BOOK_ID, patronId, NOW.minusDays(2), "LIB-POL-1", 3);
        ReflectionTestUtils.setField(reservation, "id", id);
        return reservation;
    }
}

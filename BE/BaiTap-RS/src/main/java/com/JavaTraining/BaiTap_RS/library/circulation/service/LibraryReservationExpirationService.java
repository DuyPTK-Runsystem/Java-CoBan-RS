package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import org.springframework.stereotype.Service;

@Service
public class LibraryReservationExpirationService {

    private static final String AUDIT_TARGET = "library_reservation";

    private final LibraryReservationRepository reservationRepository;
    private final BookCopyRepository copyRepository;
    private final BookRepository bookRepository;
    private final LibraryReservationAllocationService allocationService;
    private final LibraryOperationAuditService auditService;

    public LibraryReservationExpirationService(LibraryReservationRepository reservationRepository,
            BookCopyRepository copyRepository, BookRepository bookRepository,
            LibraryReservationAllocationService allocationService, LibraryOperationAuditService auditService) {
        this.reservationRepository = reservationRepository;
        this.copyRepository = copyRepository;
        this.bookRepository = bookRepository;
        this.allocationService = allocationService;
        this.auditService = auditService;
    }

    public int expireForBook(Long bookId, LocalDateTime now, Long actorUserId) {
        List<LibraryReservation> expired = reservationRepository.findExpiredByBookForUpdate(
                bookId, ReservationStatus.READY, now);
        int count = 0;
        for (LibraryReservation reservation : expired) {
            expire(reservation, now, actorUserId);
            count++;
        }
        return count;
    }

    public int expireReadyReservations(LocalDateTime now, Long actorUserId) {
        List<Long> expiredBookIds = reservationRepository.findExpiredBookIds(ReservationStatus.READY, now)
                .stream().sorted().toList();
        int count = 0;
        for (Long bookId : expiredBookIds) {
            bookRepository.findByIdForUpdate(bookId);
            List<LibraryReservation> expired = reservationRepository.findExpiredByBookForUpdate(
                    bookId, ReservationStatus.READY, now);
            for (LibraryReservation reservation : expired) {
                expire(reservation, now, actorUserId);
                count++;
            }
        }
        return count;
    }

    public void releaseAllocatedCopy(Long copyId, LocalDateTime now, Long actorUserId) {
        BookCopy copy = copyRepository.findByIdForUpdate(copyId).orElse(null);
        if (copy != null && copy.getStatus() == BookCopyStatus.RESERVED) {
            copy.setStatus(BookCopyStatus.AVAILABLE);
            allocationService.allocateNext(copy, now, actorUserId);
        }
    }

    private void expire(LibraryReservation reservation, LocalDateTime now, Long actorUserId) {
        BookCopy copy = reservation.getAllocatedCopyId() == null ? null
                : copyRepository.findByIdForUpdate(reservation.getAllocatedCopyId()).orElse(null);
        reservation.expire(now);
        if (copy != null) {
            copy.setStatus(BookCopyStatus.AVAILABLE);
            allocationService.allocateNext(copy, now, actorUserId);
        }
        auditService.record(actorUserId, "RESERVATION_EXPIRED", AUDIT_TARGET, reservation.getId(),
                null, Map.of("expiredAt", now.toString()));
    }
}

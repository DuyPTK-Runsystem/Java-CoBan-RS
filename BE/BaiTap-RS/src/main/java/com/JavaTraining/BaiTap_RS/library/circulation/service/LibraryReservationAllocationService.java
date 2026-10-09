package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryReservationAllocationService {
    private final LibraryReservationRepository reservationRepository;
    private final LibraryPatronRepository patronRepository;
    private final LibraryOperationAuditService auditService;
    private final LibraryInAppNotificationService notificationService;

    public void allocateNext(BookCopy copy, LocalDateTime now, Long actorUserId) {
        for (LibraryReservation waiting : reservationRepository.findQueue(copy.getBook().getId(),
                ReservationStatus.WAITING)) {
            LibraryPatron queued = patronRepository.findById(waiting.getPatronId()).orElse(null);
            if (queued != null && queued.getStatus() == LibraryPatronStatus.ACTIVE) {
                LocalDateTime pickupDue = now.toLocalDate().plusDays(waiting.getPickupDaysSnapshot() + 1L).atStartOfDay();
                waiting.ready(copy.getId(), now, pickupDue);
                copy.setStatus(BookCopyStatus.RESERVED);
                auditService.record(actorUserId, "RESERVATION_READY", "library_reservation", waiting.getId(), null,
                        Map.of("copyId", copy.getId(), "pickupDueAt", pickupDue.toString()));
                Long recipientUserId = patronRepository.findById(queued.getId()).map(LibraryPatron::getUserId)
                        .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND",
                                "Không tìm thấy hồ sơ bạn đọc"));
                notificationService.publish("library-reservation-ready-" + waiting.getId(),
                        "Sách đã sẵn sàng để nhận", "Đầu sách “" + copy.getBook().getTitle()
                                + "” đã sẵn sàng. Hạn nhận: " + pickupDue.toLocalDate(),
                        recipientUserId, actorUserId);
                return;
            }
        }
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}

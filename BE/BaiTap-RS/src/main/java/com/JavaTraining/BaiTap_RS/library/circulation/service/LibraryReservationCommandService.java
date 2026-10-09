package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryReservationCommandService {

    private static final String RESERVATION_AUDIT_TARGET = "library_reservation";
    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<ReservationStatus> ACTIVE_STATES =
            List.of(ReservationStatus.WAITING, ReservationStatus.READY);
    private final LibraryReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final LibraryPolicyService policyService;
    private final LibraryCirculationMapper mapper;
    private final LibraryOperationAuditService auditService;
    private final LibraryReservationExpirationService expirationService;
    private final LibraryReservationCommandEligibilityService eligibilityService;

    @Transactional
    public LibraryReservationDTO create(ReqReservationDTO request) {
        Long patronId = eligibilityService.validateCreation(request.bookId(), AuditContext.currentUserId());
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        expirationService.expireForBook(request.bookId(), now, AuditContext.currentUserId());
        LibraryCirculationPolicy policy = policyService.resolveCurrent(now);
        LibraryReservation reservation = reservationRepository.save(new LibraryReservation(request.bookId(),
                patronId, now, policy.getPolicyVersion(), policy.getReservationPickupDays()));
        auditService.record("RESERVATION_CREATED", RESERVATION_AUDIT_TARGET, reservation.getId(), null,
                Map.of("bookId", request.bookId(), "patronId", patronId));
        return mapper.reservation(reservation);
    }

    @Transactional
    public LibraryReservationDTO cancel(Long reservationId) {
        LibraryReservationRepository.ReservationLockInfo initial =
                reservationRepository.findLockInfoById(reservationId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Không tìm thấy yêu cầu đặt giữ"));
        bookRepository.findByIdForUpdate(initial.getBookId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Không tìm thấy đầu sách"));
        initial = reservationRepository.findLockInfoById(reservationId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Không tìm thấy yêu cầu đặt giữ"));
        eligibilityService.validateCancellationOwner(initial.getPatronId());
        if (initial.getAllocatedCopyId() != null) {
            copyRepository.findByIdForUpdate(initial.getAllocatedCopyId());
        }
        LibraryReservation reservation = reservationRepository.findByIdForUpdate(reservationId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Không tìm thấy yêu cầu đặt giữ"));
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        expirationService.expireForBook(reservation.getBookId(), now, AuditContext.currentUserId());
        if (!ACTIVE_STATES.contains(reservation.getStatus())) {
            throw error(HttpStatus.CONFLICT, "RESERVATION_NOT_ACTIVE", "Yêu cầu đặt giữ đã kết thúc");
        }
        Long copyId = reservation.getAllocatedCopyId();
        reservation.cancel(now);
        if (copyId != null) {
            expirationService.releaseAllocatedCopy(copyId, now, AuditContext.currentUserId());
        }
        auditService.record("RESERVATION_CANCELLED", RESERVATION_AUDIT_TARGET, reservation.getId(), null,
                Map.of("patronId", reservation.getPatronId()));
        return mapper.reservation(reservation);
    }

    @Transactional
    public int expireReadyReservations(Long actorUserId) {
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        return expirationService.expireReadyReservations(now, actorUserId);
    }



    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}

package com.JavaTraining.BaiTap_RS.library.circulation.service;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LibraryLoanBorrowValidationService {

    private final LibraryLoanRepository loanRepository;
    private final LibraryReservationRepository reservationRepository;

    public LibraryLoanBorrowValidationService(LibraryLoanRepository loanRepository,
            LibraryReservationRepository reservationRepository) {
        this.loanRepository = loanRepository;
        this.reservationRepository = reservationRepository;
    }

    public void validateLoanCapacity(LibraryPatron patron, int requestedCopies, LibraryCirculationPolicy policy) {
        long activeCount = loanRepository.findAllByPatronIdAndStatusForUpdate(
                patron.getId(), LoanStatus.ACTIVE).size();
        if (activeCount + requestedCopies > policy.getMaxActiveLoans()) {
            throw error(HttpStatus.CONFLICT, "MAX_ACTIVE_LOANS", "Bạn đọc đã đạt giới hạn mượn đang hoạt động");
        }
    }

    public LibraryReservation validateCopyForBorrow(BookCopy copy, Long patronId) {
        validatePhysicalCopy(copy);
        LibraryReservation ready = reservationRepository.findReadyByPatronAndCopyForUpdate(
                patronId, copy.getId(), ReservationStatus.READY).orElse(null);
        validateReservationAccess(copy, ready);
        return ready;
    }

    private void validatePhysicalCopy(BookCopy copy) {
        if (copy.isReferenceOnly()) {
            throw error(HttpStatus.BAD_REQUEST, "REFERENCE_ONLY", "Bản sao chỉ được sử dụng tại thư viện");
        }
        if (copy.getStatus() == BookCopyStatus.WITHDRAWN || copy.getStatus() == BookCopyStatus.LOST
                || copy.getStatus() == BookCopyStatus.DAMAGED || copy.getBook().isArchived()) {
            throw error(HttpStatus.CONFLICT, "COPY_NOT_BORROWABLE", "Bản sao không thể được mượn");
        }
    }

    private void validateReservationAccess(BookCopy copy, LibraryReservation ready) {
        validateReservationOwnership(copy, ready);
        validateCopyAvailability(copy, ready);
        validateWaitingQueue(copy, ready);
    }

    private void validateReservationOwnership(BookCopy copy, LibraryReservation ready) {
        if (copy.getStatus() == BookCopyStatus.RESERVED && ready == null) {
            throw error(HttpStatus.CONFLICT, "COPY_RESERVED", "Bản sao đã được đặt trước");
        }
    }

    private void validateCopyAvailability(BookCopy copy, LibraryReservation ready) {
        if (copy.getStatus() != BookCopyStatus.AVAILABLE && ready == null) {
            throw error(HttpStatus.CONFLICT, "COPY_ALREADY_ON_LOAN", "Bản sao không còn khả dụng");
        }
    }

    private void validateWaitingQueue(BookCopy copy, LibraryReservation ready) {
        if (ready == null && !reservationRepository.findQueue(copy.getBook().getId(),
                ReservationStatus.WAITING).isEmpty()) {
            throw error(HttpStatus.CONFLICT, "COPY_RESERVED", "Có bạn đọc đang chờ bản sao này");
        }
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}

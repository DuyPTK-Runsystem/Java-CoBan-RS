package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLoanDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LibraryLoanRenewalService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String COPY_NOT_FOUND = "COPY_NOT_FOUND";
    private static final String PATRON_NOT_FOUND = "PATRON_NOT_FOUND";
    private final LibraryLoanRepository loanRepository;
    private final LibraryReservationRepository reservationRepository;
    private final BookCopyRepository copyRepository;
    private final BookRepository bookRepository;
    private final LibraryPatronRepository patronRepository;
    private final LibraryLoanRenewalEligibilityService eligibilityService;
    private final LibraryPolicyService policyService;
    private final LibraryLoanRenewalRecorderService recorderService;

    @Transactional
    public LibraryLoanDTO renew(Long loanId) {
        LibraryLoanRepository.LoanLockInfo initial = loanRepository.findLockInfoById(loanId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "ACTIVE_LOAN_NOT_FOUND", "Không tìm thấy khoản mượn"));
        Long bookId = copyRepository.findBookIdById(initial.getCopyId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, COPY_NOT_FOUND, "Không tìm thấy bản sao"));
        lockBook(bookId);
        LibraryPatron patron = patronRepository.findByIdForUpdate(initial.getPatronId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc"));
        copyRepository.findByIdForUpdate(initial.getCopyId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, COPY_NOT_FOUND, "Không tìm thấy bản sao"));
        LibraryLoan loan = loanRepository.findByIdForUpdate(loanId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "ACTIVE_LOAN_NOT_FOUND", "Không tìm thấy khoản mượn"));
        eligibilityService.validate(patron, loan);
        LibraryCirculationPolicy policy = policyService.resolveCurrent(LocalDateTime.now(LIBRARY_ZONE));
        if (loan.getRenewCount() >= policy.getMaxRenewals()) {
            throw error(HttpStatus.CONFLICT, "RENEW_LIMIT_REACHED", "Khoản mượn đã đạt số lần gia hạn tối đa");
        }
        BookCopy copy = copyRepository.findById(loan.getCopyId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, COPY_NOT_FOUND, "Không tìm thấy bản sao"));
        List<LibraryReservation> queue = reservationRepository.findQueue(copy.getBook().getId(), ReservationStatus.WAITING);
        List<LibraryReservation> readyQueue = reservationRepository.findQueue(
                copy.getBook().getId(), ReservationStatus.READY);
        validateReservationQueue(queue, readyQueue);
        return recorderService.renewAndRecord(loan, policy, LocalDateTime.now(LIBRARY_ZONE));
    }

    private void validateReservationQueue(List<LibraryReservation> queue, List<LibraryReservation> readyQueue) {
        if (!queue.isEmpty() || !readyQueue.isEmpty()) {
            throw error(HttpStatus.CONFLICT, "COPY_RESERVED", "Có bạn đọc đang chờ đầu sách này");
        }
    }

    private void lockBook(Long bookId) {
        bookRepository.findByIdForUpdate(bookId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Không tìm thấy đầu sách"));
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}

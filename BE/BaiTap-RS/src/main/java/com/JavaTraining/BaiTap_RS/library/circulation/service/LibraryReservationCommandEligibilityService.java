package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.util.List;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LibraryReservationCommandEligibilityService {

    private static final String PATRON_NOT_FOUND = "PATRON_NOT_FOUND";
    private static final List<ReservationStatus> ACTIVE_STATES =
            List.of(ReservationStatus.WAITING, ReservationStatus.READY);

    private final LibraryReservationRepository reservationRepository;
    private final LibraryPatronRepository patronRepository;
    private final LibraryEligibilityService eligibilityService;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final LibraryAccessPolicy accessPolicy;

    public LibraryReservationCommandEligibilityService(LibraryReservationRepository reservationRepository,
            LibraryPatronRepository patronRepository, LibraryEligibilityService eligibilityService,
            BookRepository bookRepository, BookCopyRepository copyRepository,
            LibraryAccessPolicy accessPolicy) {
        this.reservationRepository = reservationRepository;
        this.patronRepository = patronRepository;
        this.eligibilityService = eligibilityService;
        this.bookRepository = bookRepository;
        this.copyRepository = copyRepository;
        this.accessPolicy = accessPolicy;
    }

    public Long validateCreation(Long bookId, Long actorUserId) {
        LibraryEligibilityService.BorrowingEligibility eligibility =
                eligibilityService.assertCanBorrow(actorUserId, false);
        Book book = lockReservableBook(bookId);
        LibraryPatron patron = lockEligiblePatron(eligibility.patronId());
        if (!copyRepository.findAvailableCopiesForUpdate(book.getId(), BookCopyStatus.AVAILABLE).isEmpty()) {
            throw error(HttpStatus.CONFLICT, "RESERVATION_NOT_NEEDED", "Có bản sao đang sẵn sàng để mượn");
        }
        if (!reservationRepository.findActiveByPatronAndBookForUpdate(
                patron.getId(), book.getId(), ACTIVE_STATES).isEmpty()) {
            throw error(HttpStatus.CONFLICT, "RESERVATION_ALREADY_EXISTS",
                    "Bạn đã có yêu cầu đặt giữ đang hoạt động");
        }
        return patron.getId();
    }

    public void validateCancellationOwner(Long patronId) {
        LibraryPatron patron = patronRepository.findById(patronId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, PATRON_NOT_FOUND, "Không tìm thấy hồ sơ bạn đọc"));
        if (!accessPolicy.isManagerOrOwner(patron.getUserId())) {
            throw error(HttpStatus.FORBIDDEN, "LIBRARY_RESOURCE_FORBIDDEN",
                    "Không có quyền hủy yêu cầu đặt giữ");
        }
    }

    private Book lockReservableBook(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Không tìm thấy đầu sách"));
        if (book.isArchived()) {
            throw error(HttpStatus.CONFLICT, "BOOK_NOT_AVAILABLE", "Đầu sách đã được lưu trữ");
        }
        return bookRepository.findByIdForUpdate(book.getId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Không tìm thấy đầu sách"));
    }

    private LibraryPatron lockEligiblePatron(Long patronId) {
        LibraryPatron patron = patronRepository.findByIdForUpdate(patronId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, PATRON_NOT_FOUND, "Không tìm thấy hồ sơ bạn đọc"));
        if (patron.getStatus() == LibraryPatronStatus.BORROWING_SUSPENDED) {
            throw error(HttpStatus.FORBIDDEN, "PATRON_BORROWING_SUSPENDED", "Bạn đọc đang bị đình chỉ mượn");
        }
        if (patron.getStatus() != LibraryPatronStatus.ACTIVE) {
            throw error(HttpStatus.FORBIDDEN, "PATRON_NOT_ELIGIBLE", "Hồ sơ bạn đọc đã đóng");
        }
        return patron;
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}

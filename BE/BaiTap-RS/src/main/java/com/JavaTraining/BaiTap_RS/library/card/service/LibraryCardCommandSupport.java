package com.JavaTraining.BaiTap_RS.library.card.service;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LibraryCardCommandSupport {

    private static final String CARD_NOT_FOUND = "LIBRARY_CARD_NOT_FOUND";
    private static final String CARD_NOT_FOUND_MESSAGE = "Không tìm thấy thẻ thư viện";
    private static final String PATRON_NOT_FOUND = "PATRON_NOT_FOUND";
    private static final String PATRON_NOT_FOUND_MESSAGE = "Không tìm thấy hồ sơ bạn đọc";

    private final LibraryCardRepository cardRepository;
    private final LibraryPatronRepository patronRepository;

    public LibraryCardCommandSupport(LibraryCardRepository cardRepository,
            LibraryPatronRepository patronRepository) {
        this.cardRepository = cardRepository;
        this.patronRepository = patronRepository;
    }

    public LibraryCard findCard(String cardNo) {
        return cardRepository.findByCardNo(cardNo).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, CARD_NOT_FOUND, CARD_NOT_FOUND_MESSAGE));
    }

    public LibraryCard findCardForUpdate(String cardNo) {
        return cardRepository.findByCardNoForUpdate(cardNo).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, CARD_NOT_FOUND, CARD_NOT_FOUND_MESSAGE));
    }

    public LibraryPatron patronForUpdate(Long patronId) {
        return patronRepository.findByIdForUpdate(patronId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, PATRON_NOT_FOUND, PATRON_NOT_FOUND_MESSAGE));
    }

    public void requireActivePatron(LibraryPatron patron) {
        if (patron.getStatus() == LibraryPatronStatus.BORROWING_SUSPENDED) {
            throw error(HttpStatus.CONFLICT, "PATRON_BORROWING_SUSPENDED", "Hồ sơ bạn đọc không hoạt động");
        }
        if (patron.getStatus() != LibraryPatronStatus.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "PATRON_NOT_ELIGIBLE", "Hồ sơ bạn đọc đã đóng");
        }
    }

    public void requireFutureOrToday(LocalDate expiresAt, LocalDate today) {
        if (expiresAt.isBefore(today)) {
            throw error(HttpStatus.BAD_REQUEST, "CARD_EXPIRY_DATE_INVALID",
                    "Ngày hết hạn phải là hôm nay hoặc sau hôm nay");
        }
    }

    private LibraryPatronException error(HttpStatus status, String code, String message) {
        return new LibraryPatronException(status, code, message);
    }
}

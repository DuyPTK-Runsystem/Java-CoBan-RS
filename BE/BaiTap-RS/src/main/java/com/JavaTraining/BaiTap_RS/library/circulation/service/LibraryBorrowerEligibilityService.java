package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDate;
import java.time.ZoneId;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.RoleCode;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryBorrowerEligibilityService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String PATRON_NOT_FOUND = "PATRON_NOT_FOUND";
    private static final String CARD_REVOKED = "CARD_REVOKED";
    private final LibraryCardRepository cardRepository;
    private final UserRepository userRepository;

    public LibraryCard validateBorrower(LibraryPatron patron, String cardNo) {
        validatePatronStatus(patron);
        validateBorrowerRole(patron);
        if (cardNo == null || cardNo.isBlank()) {
            throw error(HttpStatus.FORBIDDEN, CARD_REVOKED, "Thẻ thư viện không hợp lệ");
        }
        LibraryCard card = cardRepository.findByCardNoForUpdate(cardNo).orElseThrow(() ->
                error(HttpStatus.FORBIDDEN, CARD_REVOKED, "Thẻ thư viện không hợp lệ"));
        if (!card.getPatronId().equals(patron.getId()) || card.getStatus() != LibraryCardStatus.ACTIVE) {
            throw error(HttpStatus.FORBIDDEN, CARD_REVOKED, "Thẻ thư viện không còn hiệu lực");
        }
        if (card.getExpiresAt().isBefore(LocalDate.now(LIBRARY_ZONE))) {
            throw error(HttpStatus.FORBIDDEN, "CARD_EXPIRED", "Thẻ thư viện đã hết hạn");
        }
        return card;
    }

    public void validateRenewal(LibraryPatron patron, boolean staff) {
        validatePatronStatus(patron);
        LibraryCard card = cardRepository.findFirstByPatronIdAndStatusOrderByIssuedAtDesc(
                patron.getId(), LibraryCardStatus.ACTIVE).orElseThrow(() ->
                        error(HttpStatus.FORBIDDEN, CARD_REVOKED, "Không tìm thấy thẻ hợp lệ"));
        if (card.getExpiresAt().isBefore(LocalDate.now(LIBRARY_ZONE))) {
            throw error(HttpStatus.FORBIDDEN, "CARD_EXPIRED", "Thẻ thư viện đã hết hạn");
        }
        if (!staff) {
            validateBorrowerRole(patron);
        }
    }

    private void validatePatronStatus(LibraryPatron patron) {
        if (patron.getStatus() == LibraryPatronStatus.BORROWING_SUSPENDED) {
            throw error(HttpStatus.FORBIDDEN, "PATRON_BORROWING_SUSPENDED", "Bạn đọc đang bị đình chỉ mượn");
        }
        if (patron.getStatus() != LibraryPatronStatus.ACTIVE) {
            throw error(HttpStatus.FORBIDDEN, "PATRON_NOT_ELIGIBLE", "Hồ sơ bạn đọc đã đóng");
        }
    }

    private void validateBorrowerRole(LibraryPatron patron) {
        User user = userRepository.findById(patron.getUserId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, PATRON_NOT_FOUND, "Không tìm thấy tài khoản bạn đọc"));
        boolean excluded = user.getRoles().stream().map(Role::getCode).anyMatch(code ->
                RoleCode.ADMIN.code().equals(code) || RoleCode.LIBRARIAN.code().equals(code));
        if (excluded) {
            throw error(HttpStatus.FORBIDDEN, "LIBRARY_RESOURCE_FORBIDDEN", "Tài khoản không đủ điều kiện mượn sách");
        }
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}

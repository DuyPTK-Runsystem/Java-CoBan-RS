package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDate;
import java.time.ZoneId;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.RoleCode;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shared borrower gate for Plan 098 circulation use cases. */
@Service
@RequiredArgsConstructor
public class LibraryEligibilityService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository userRepository;
    private final LibraryPatronRepository patronRepository;
    private final LibraryCardRepository cardRepository;

    @Transactional(readOnly = true)
    public BorrowingEligibility assertCanBorrow(Long userId, boolean cardRequired) {
        User user = userRepository.findById(userId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "PATRON_USER_NOT_FOUND", "Không tìm thấy tài khoản"));
        boolean excludedRole = user.getRoles().stream().map(Role::getCode)
                .anyMatch(code -> RoleCode.ADMIN.code().equals(code) || RoleCode.LIBRARIAN.code().equals(code));
        if (excludedRole) {
            throw error(HttpStatus.FORBIDDEN, "LIBRARY_RESOURCE_FORBIDDEN",
                    "Tài khoản ADMIN hoặc LIBRARIAN không đủ điều kiện mượn sách");
        }
        LibraryPatron patron = patronRepository.findByUserId(userId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc"));
        if (patron.getStatus() == LibraryPatronStatus.BORROWING_SUSPENDED) {
            throw error(HttpStatus.FORBIDDEN, "PATRON_BORROWING_SUSPENDED", "Bạn đọc đang bị đình chỉ mượn");
        }
        if (patron.getStatus() != LibraryPatronStatus.ACTIVE) {
            throw error(HttpStatus.FORBIDDEN, "PATRON_NOT_ELIGIBLE", "Hồ sơ bạn đọc đã đóng");
        }
        String cardNo = null;
        if (cardRequired) {
            LibraryCard card = cardRepository.findFirstByPatronIdAndStatusOrderByIssuedAtDesc(
                    patron.getId(), LibraryCardStatus.ACTIVE).filter(candidate -> !candidate.getExpiresAt()
                            .isBefore(LocalDate.now(LIBRARY_ZONE))).orElseThrow(() -> error(
                                    HttpStatus.FORBIDDEN, "CARD_NOT_VALID", "Bạn đọc chưa có thẻ hợp lệ"));
            cardNo = card.getCardNo();
        }
        return new BorrowingEligibility(userId, patron.getId(), cardNo);
    }

    public record BorrowingEligibility(Long userId, Long patronId, String cardNo) { }

    private LibraryPatronException error(HttpStatus status, String code, String message) {
        return new LibraryPatronException(status, code, message);
    }
}

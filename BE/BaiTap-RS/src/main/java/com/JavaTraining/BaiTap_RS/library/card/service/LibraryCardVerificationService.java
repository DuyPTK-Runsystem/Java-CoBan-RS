package com.JavaTraining.BaiTap_RS.library.card.service;

import java.time.LocalDate;
import java.time.ZoneId;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqVerifyLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardVerificationDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryCardVerificationService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String CARD_NOT_FOUND = "LIBRARY_CARD_NOT_FOUND";
    private static final String CARD_NOT_FOUND_MESSAGE = "Không tìm thấy thẻ thư viện";
    private static final String PATRON_NOT_FOUND = "PATRON_NOT_FOUND";
    private static final String PATRON_NOT_FOUND_MESSAGE = "Không tìm thấy hồ sơ bạn đọc";
    private static final String CARD_EXPIRED = "CARD_EXPIRED";
    private static final String CARD_EXPIRED_MESSAGE = "Thẻ đã hết hạn";
    private static final String CARD_REVOKED = "CARD_REVOKED";
    private static final String CARD_REVOKED_MESSAGE = "Thẻ đã bị thu hồi";

    private final LibraryCardRepository cardRepository;
    private final LibraryPatronRepository patronRepository;
    private final LibraryCardQrService qrService;

    public LibraryCardVerificationService(LibraryCardRepository cardRepository,
            LibraryPatronRepository patronRepository, LibraryCardQrService qrService) {
        this.cardRepository = cardRepository;
        this.patronRepository = patronRepository;
        this.qrService = qrService;
    }

    @Transactional
    public ResLibraryCardVerificationDTO verify(ReqVerifyLibraryCardDTO request) {
        LibraryCardQrService.ParsedPayload payload = qrService.parseAndVerify(request);
        if (payload.expiresAt().isBefore(LocalDate.now(LIBRARY_ZONE))) {
            throw error(HttpStatus.BAD_REQUEST, CARD_EXPIRED, CARD_EXPIRED_MESSAGE);
        }
        LibraryCard card = cardRepository.findByCardNo(payload.cardNo()).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, CARD_NOT_FOUND, CARD_NOT_FOUND_MESSAGE));
        if (!card.getPatronId().equals(payload.patronId()) || !card.getExpiresAt().equals(payload.expiresAt())) {
            throw error(HttpStatus.BAD_REQUEST, "CARD_SIGNATURE_INVALID", "Dữ liệu thẻ không khớp");
        }
        validateCardStatus(card);
        LibraryPatron patron = patronRepository.findById(payload.patronId()).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, PATRON_NOT_FOUND, PATRON_NOT_FOUND_MESSAGE));
        return new ResLibraryCardVerificationDTO(true, LibraryCardMapper.toResponse(card), patron.getStatus());
    }

    private void validateCardStatus(LibraryCard card) {
        if (card.getStatus() == LibraryCardStatus.REVOKED) {
            throw error(HttpStatus.BAD_REQUEST, CARD_REVOKED, CARD_REVOKED_MESSAGE);
        }
        if (card.getStatus() == LibraryCardStatus.EXPIRED) {
            throw error(HttpStatus.BAD_REQUEST, CARD_EXPIRED, CARD_EXPIRED_MESSAGE);
        }
    }

    private LibraryPatronException error(HttpStatus status, String code, String message) {
        return new LibraryPatronException(status, code, message);
    }
}

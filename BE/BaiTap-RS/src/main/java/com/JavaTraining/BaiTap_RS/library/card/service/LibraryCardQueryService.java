package com.JavaTraining.BaiTap_RS.library.card.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryCardQueryService {

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
    private final LibraryAccessPolicy accessPolicy;

    public LibraryCardQueryService(LibraryCardRepository cardRepository, LibraryPatronRepository patronRepository,
            LibraryCardQrService qrService, LibraryAccessPolicy accessPolicy) {
        this.cardRepository = cardRepository;
        this.patronRepository = patronRepository;
        this.qrService = qrService;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public ResLibraryCardDTO getMe() {
        LibraryPatron patron = myPatron();
        LibraryCard card = cardRepository.findFirstByPatronIdAndStatusOrderByIssuedAtDesc(
                patron.getId(), LibraryCardStatus.ACTIVE).orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, CARD_NOT_FOUND, "Chưa có thẻ thư viện"));
        return LibraryCardMapper.toResponse(card);
    }

    @Transactional(readOnly = true)
    public List<ResLibraryCardDTO> historyForPatron(Long patronId) {
        requirePatron(patronId);
        return cardRepository.findAllByPatronIdOrderByIssuedAtDesc(patronId).stream()
                .map(LibraryCardMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ResLibraryCardDTO> myHistory() {
        return historyForPatron(myPatron().getId());
    }

    @Transactional(readOnly = true)
    public byte[] qrPng(String cardNo) {
        LibraryCard card = findCard(cardNo);
        LibraryPatron patron = requirePatron(card.getPatronId());
        if (!accessPolicy.isManagerOrOwner(patron.getUserId())) {
            throw error(HttpStatus.FORBIDDEN, "LIBRARY_RESOURCE_FORBIDDEN", "Không có quyền xem mã QR");
        }
        ensureCardCurrent(card);
        return qrService.png(qrService.payload(card));
    }

    private void ensureCardCurrent(LibraryCard card) {
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        if (card.getStatus() == LibraryCardStatus.REVOKED) {
            throw error(HttpStatus.BAD_REQUEST, CARD_REVOKED, CARD_REVOKED_MESSAGE);
        }
        if (card.getStatus() != LibraryCardStatus.ACTIVE || card.getExpiresAt().isBefore(today)) {
            throw error(HttpStatus.BAD_REQUEST, CARD_EXPIRED, CARD_EXPIRED_MESSAGE);
        }
        qrService.requireConfigured();
    }

    private LibraryCard findCard(String cardNo) {
        return cardRepository.findByCardNo(cardNo).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, CARD_NOT_FOUND, CARD_NOT_FOUND_MESSAGE));
    }

    private LibraryPatron myPatron() {
        Long userId = accessPolicy.currentUserId();
        if (userId == null) {
            throw error(HttpStatus.UNAUTHORIZED, "LIBRARY_RESOURCE_FORBIDDEN", "Chưa xác thực tài khoản");
        }
        return patronRepository.findByUserId(userId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, PATRON_NOT_FOUND, PATRON_NOT_FOUND_MESSAGE));
    }

    private LibraryPatron requirePatron(Long patronId) {
        return patronRepository.findById(patronId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, PATRON_NOT_FOUND, PATRON_NOT_FOUND_MESSAGE));
    }

    private LibraryPatronException error(HttpStatus status, String code, String message) {
        return new LibraryPatronException(status, code, message);
    }
}

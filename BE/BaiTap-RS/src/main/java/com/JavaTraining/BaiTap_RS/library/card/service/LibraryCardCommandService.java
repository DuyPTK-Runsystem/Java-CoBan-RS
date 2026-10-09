package com.JavaTraining.BaiTap_RS.library.card.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqIssueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqReissueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqRevokeLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryCardCommandService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ENTITY_TYPE = "library_card";
    private static final String CARD_EXPIRED = "CARD_EXPIRED";
    private static final String CARD_EXPIRED_MESSAGE = "Thẻ đã hết hạn";

    private final LibraryCardRepository cardRepository;
    private final LibraryCardQrService qrService;
    private final LibraryOperationAuditService auditService;
    private final LibraryCardCommandSupport support;
    private final LibraryCardLifecycleService lifecycleService;

    @Transactional
    public ResLibraryCardDTO issue(ReqIssueLibraryCardDTO request) {
        qrService.requireConfigured();
        LibraryPatron patron = support.patronForUpdate(request.patronId());
        support.requireActivePatron(patron);
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        support.requireFutureOrToday(request.expiresAt(), today);
        lifecycleService.expireStaleActiveCards(patron.getId(), today);
        if (!cardRepository.findByPatronIdAndStatusForUpdate(patron.getId(), LibraryCardStatus.ACTIVE).isEmpty()) {
            throw error(HttpStatus.CONFLICT, "CARD_ALREADY_ACTIVE", "Bạn đọc đã có thẻ đang hoạt động");
        }
        return LibraryCardMapper.toResponse(
                lifecycleService.createCard(patron, request.expiresAt(), "LIBRARY_CARD_ISSUED"));
    }

    @Transactional
    public ResLibraryCardDTO reissue(String cardNo, ReqReissueLibraryCardDTO request) {
        qrService.requireConfigured();
        LibraryCard known = support.findCard(cardNo);
        LibraryPatron patron = support.patronForUpdate(known.getPatronId());
        LibraryCard oldCard = support.findCardForUpdate(cardNo);
        support.requireActivePatron(patron);
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        support.requireFutureOrToday(request.expiresAt(), today);
        if (oldCard.getStatus() != LibraryCardStatus.ACTIVE || oldCard.getExpiresAt().isBefore(today)) {
            throw error(HttpStatus.CONFLICT, "CARD_REISSUE_NOT_ALLOWED", "Chỉ có thể cấp lại thẻ đang còn hiệu lực");
        }
        Map<String, Object> before = LibraryCardMapper.auditSnapshot(oldCard);
        oldCard.revoke(request.reason().trim(), LocalDateTime.now());
        cardRepository.flush();
        auditService.record("LIBRARY_CARD_REISSUED_OLD", ENTITY_TYPE, oldCard.getId(), before,
                LibraryCardMapper.auditSnapshot(oldCard));
        LibraryCard replacement = lifecycleService.createCard(patron, request.expiresAt(), "LIBRARY_CARD_REISSUED_NEW");
        return LibraryCardMapper.toResponse(replacement);
    }

    @Transactional
    public ResLibraryCardDTO revoke(String cardNo, ReqRevokeLibraryCardDTO request) {
        LibraryCard known = support.findCard(cardNo);
        support.patronForUpdate(known.getPatronId());
        LibraryCard card = support.findCardForUpdate(cardNo);
        if (card.getStatus() == LibraryCardStatus.REVOKED) {
            return LibraryCardMapper.toResponse(card);
        }
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        if (card.getStatus() == LibraryCardStatus.EXPIRED || card.getExpiresAt().isBefore(today)) {
            lifecycleService.expireIfStale(card, today);
            throw error(HttpStatus.CONFLICT, CARD_EXPIRED, CARD_EXPIRED_MESSAGE);
        }
        Map<String, Object> before = LibraryCardMapper.auditSnapshot(card);
        card.revoke(request.reason().trim(), LocalDateTime.now());
        cardRepository.flush();
        auditService.record("LIBRARY_CARD_REVOKED", ENTITY_TYPE, card.getId(), before,
                LibraryCardMapper.auditSnapshot(card));
        return LibraryCardMapper.toResponse(card);
    }

    private LibraryPatronException error(HttpStatus status, String code, String message) {
        return new LibraryPatronException(status, code, message);
    }
}

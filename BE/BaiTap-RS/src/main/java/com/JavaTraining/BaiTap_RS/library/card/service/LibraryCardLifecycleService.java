package com.JavaTraining.BaiTap_RS.library.card.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LibraryCardLifecycleService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String POLICY_VERSION = "EXPLICIT_EXPIRY_V1";
    private static final String ENTITY_TYPE = "library_card";

    private final LibraryCardRepository cardRepository;
    private final LibraryCardNumberService numberService;
    private final LibraryOperationAuditService auditService;

    public LibraryCardLifecycleService(LibraryCardRepository cardRepository,
            LibraryCardNumberService numberService, LibraryOperationAuditService auditService) {
        this.cardRepository = cardRepository;
        this.numberService = numberService;
        this.auditService = auditService;
    }

    public LibraryCard createCard(LibraryPatron patron, LocalDate expiresAt, String auditAction) {
        String cardNo = numberService.nextCardNo(LocalDate.now(LIBRARY_ZONE).getYear());
        LibraryCard card;
        try {
            card = cardRepository.saveAndFlush(new LibraryCard(patron.getId(), cardNo, LocalDateTime.now(),
                    expiresAt, "v1", POLICY_VERSION));
        } catch (DataIntegrityViolationException exception) {
            throw new LibraryPatronException(HttpStatus.CONFLICT, "CARD_ALREADY_ACTIVE",
                    "Bạn đọc đã có thẻ đang hoạt động", exception);
        }
        auditService.record(auditAction, ENTITY_TYPE, card.getId(), null, LibraryCardMapper.auditSnapshot(card));
        return card;
    }

    public void expireStaleActiveCards(Long patronId, LocalDate today) {
        List<LibraryCard> cards = cardRepository.findByPatronIdAndStatusForUpdate(patronId, LibraryCardStatus.ACTIVE);
        for (LibraryCard card : cards) {
            expireIfStale(card, today);
        }
        cardRepository.flush();
    }

    public void expireIfStale(LibraryCard card, LocalDate today) {
        if (card.getStatus() == LibraryCardStatus.ACTIVE && card.getExpiresAt().isBefore(today)) {
            Map<String, Object> before = LibraryCardMapper.auditSnapshot(card);
            card.expire(LocalDateTime.now());
            auditService.record("LIBRARY_CARD_EXPIRED", ENTITY_TYPE, card.getId(), before,
                    LibraryCardMapper.auditSnapshot(card));
        }
    }
}

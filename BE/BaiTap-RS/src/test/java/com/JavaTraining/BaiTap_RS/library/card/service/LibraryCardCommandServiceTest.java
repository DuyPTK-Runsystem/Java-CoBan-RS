package com.JavaTraining.BaiTap_RS.library.card.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqIssueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqReissueLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqRevokeLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryCardCommandServiceTest {

    private static final String CARD_NO = "LC-2026-000001";
    private static final Long PATRON_ID = 4L;
    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private LibraryCardRepository cardRepository;

    @Mock
    private LibraryCardQrService qrService;

    @Mock
    private LibraryOperationAuditService auditService;

    @Mock
    private LibraryCardCommandSupport support;

    @Mock
    private LibraryCardLifecycleService lifecycleService;

    @InjectMocks
    private LibraryCardCommandService service;

    private LibraryPatron patron;

    @BeforeEach
    void setUp() {
        patron = new LibraryPatron(15L, LocalDateTime.of(2026, 10, 9, 10, 0));
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
        when(support.patronForUpdate(PATRON_ID)).thenReturn(patron);
    }

    @Test
    void issuesOneActiveCardWithCallerSuppliedInclusiveExpiryDate() {
        LocalDate expiresAt = LocalDate.now(LIBRARY_ZONE);
        when(cardRepository.findByPatronIdAndStatusForUpdate(PATRON_ID, LibraryCardStatus.ACTIVE))
                .thenReturn(List.of());
        when(lifecycleService.createCard(patron, expiresAt, "LIBRARY_CARD_ISSUED"))
                .thenReturn(card(expiresAt));

        var result = service.issue(new ReqIssueLibraryCardDTO(PATRON_ID, expiresAt));

        assertEquals(CARD_NO, result.cardNo());
        assertEquals(expiresAt, result.expiresAt());
        assertEquals(LibraryCardStatus.ACTIVE, result.status());
        verify(qrService).requireConfigured();
        verify(lifecycleService).expireStaleActiveCards(PATRON_ID, LocalDate.now(LIBRARY_ZONE));
    }

    @Test
    void refusesIssuanceWhenAnActiveCardStillExists() {
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        LibraryCard existing = card(today);
        when(cardRepository.findByPatronIdAndStatusForUpdate(PATRON_ID, LibraryCardStatus.ACTIVE))
                .thenReturn(List.of(existing));

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.issue(new ReqIssueLibraryCardDTO(PATRON_ID, today)));

        assertEquals("CARD_ALREADY_ACTIVE", exception.getCode());
        verify(lifecycleService, never()).createCard(any(), any(), any());
    }

    @Test
    void atomicallyReissuesActiveCardAndPreservesTheOldCardAsRevokedHistory() {
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        LibraryCard oldCard = card(today);
        ReflectionTestUtils.setField(oldCard, "id", 8L);
        when(support.findCard(CARD_NO)).thenReturn(oldCard);
        when(support.findCardForUpdate(CARD_NO)).thenReturn(oldCard);
        when(lifecycleService.createCard(patron, today, "LIBRARY_CARD_REISSUED_NEW"))
                .thenReturn(card(today));

        var replacement = service.reissue(CARD_NO, new ReqReissueLibraryCardDTO(today, "Damaged"));

        assertEquals(LibraryCardStatus.REVOKED, oldCard.getStatus());
        assertEquals("Damaged", oldCard.getRevokedReason());
        assertEquals(CARD_NO, replacement.cardNo());
        assertEquals(today, replacement.expiresAt());
        verify(auditService).record(eq("LIBRARY_CARD_REISSUED_OLD"), eq("library_card"), eq(8L), any(), any());
        verify(lifecycleService).createCard(patron, today, "LIBRARY_CARD_REISSUED_NEW");
    }

    @Test
    void revokesAnActiveCardAndKeepsTheReason() {
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        LibraryCard active = card(today);
        ReflectionTestUtils.setField(active, "id", 8L);
        when(support.findCard(CARD_NO)).thenReturn(active);
        when(support.findCardForUpdate(CARD_NO)).thenReturn(active);

        var revoked = service.revoke(CARD_NO, new ReqRevokeLibraryCardDTO("Lost"));

        assertEquals(LibraryCardStatus.REVOKED, revoked.status());
        assertEquals("Lost", revoked.revokedReason());
        verify(auditService).record(eq("LIBRARY_CARD_REVOKED"), eq("library_card"), eq(8L), any(), any());
    }

    @Test
    void rejectsReissueWhenCardIsNotActive() {
        LibraryCard expired = card(LocalDate.now(LIBRARY_ZONE));
        expired.revoke("already revoked", LocalDateTime.now());
        when(support.findCard(CARD_NO)).thenReturn(expired);
        when(support.findCardForUpdate(CARD_NO)).thenReturn(expired);

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.reissue(CARD_NO,
                        new ReqReissueLibraryCardDTO(LocalDate.now(LIBRARY_ZONE), "Replacement")));

        assertEquals("CARD_REISSUE_NOT_ALLOWED", exception.getCode());
        verify(lifecycleService, never()).createCard(any(), any(), any());
    }

    private LibraryCard card(LocalDate expiresAt) {
        LibraryCard card = new LibraryCard(PATRON_ID, CARD_NO, LocalDateTime.now(), expiresAt, "v1", "test");
        ReflectionTestUtils.setField(card, "id", 9L);
        return card;
    }
}

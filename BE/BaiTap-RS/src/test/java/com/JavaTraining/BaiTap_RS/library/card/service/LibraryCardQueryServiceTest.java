package com.JavaTraining.BaiTap_RS.library.card.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryCardQueryServiceTest {

    private static final String SECRET = "plan097-test-secret-at-least-32-bytes";
    private static final String CARD_NO = "LC-2026-000001";
    private static final Long PATRON_ID = 4L;
    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private LibraryCardRepository cardRepository;

    @Mock
    private LibraryPatronRepository patronRepository;

    @Mock
    private LibraryAccessPolicy accessPolicy;

    private final LibraryCardQrService qrService = new LibraryCardQrService();

    private LibraryCardQueryService service;

    private LibraryPatron patron;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(qrService, "signingSecret", SECRET);
        service = new LibraryCardQueryService(cardRepository, patronRepository, qrService, accessPolicy);
        patron = new LibraryPatron(15L, LocalDateTime.of(2026, 10, 9, 10, 0));
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
    }

    @Test
    void deniesQrAccessWhenCallerIsNeitherManagerNorOwner() {
        when(cardRepository.findByCardNo(CARD_NO)).thenReturn(Optional.of(card()));
        when(patronRepository.findById(PATRON_ID)).thenReturn(Optional.of(patron));
        when(accessPolicy.isManagerOrOwner(15L)).thenReturn(false);

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.qrPng(CARD_NO));

        assertEquals("LIBRARY_RESOURCE_FORBIDDEN", exception.getCode());
    }

    @Test
    void generatesQrPngForTheCardOwner() {
        when(cardRepository.findByCardNo(CARD_NO)).thenReturn(Optional.of(card()));
        when(patronRepository.findById(PATRON_ID)).thenReturn(Optional.of(patron));
        when(accessPolicy.isManagerOrOwner(15L)).thenReturn(true);

        byte[] png = service.qrPng(CARD_NO);

        assertTrue(png.length > 8);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 0x50, png[1]);
    }

    @Test
    void refusesQrWhenCardHasBeenRevoked() {
        LibraryCard revoked = card();
        revoked.revoke("Lost", LocalDateTime.now());
        when(cardRepository.findByCardNo(CARD_NO)).thenReturn(Optional.of(revoked));
        when(patronRepository.findById(PATRON_ID)).thenReturn(Optional.of(patron));
        when(accessPolicy.isManagerOrOwner(15L)).thenReturn(true);

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.qrPng(CARD_NO));

        assertEquals("CARD_REVOKED", exception.getCode());
    }

    @Test
    void returnsCurrentCardForAuthenticatedOwner() {
        when(accessPolicy.currentUserId()).thenReturn(15L);
        when(patronRepository.findByUserId(15L)).thenReturn(Optional.of(patron));
        when(cardRepository.findFirstByPatronIdAndStatusOrderByIssuedAtDesc(PATRON_ID, LibraryCardStatus.ACTIVE))
                .thenReturn(Optional.of(card()));

        var result = service.getMe();

        assertEquals(CARD_NO, result.cardNo());
        verify(accessPolicy).currentUserId();
    }

    @Test
    void returnsCardHistoryForPatron() {
        when(patronRepository.findById(PATRON_ID)).thenReturn(Optional.of(patron));
        when(cardRepository.findAllByPatronIdOrderByIssuedAtDesc(PATRON_ID)).thenReturn(List.of(card()));

        var history = service.historyForPatron(PATRON_ID);

        assertEquals(1, history.size());
        assertEquals(CARD_NO, history.getFirst().cardNo());
    }

    private LibraryCard card() {
        return new LibraryCard(PATRON_ID, CARD_NO, LocalDateTime.now(), LocalDate.now(LIBRARY_ZONE), "v1", "test");
    }
}

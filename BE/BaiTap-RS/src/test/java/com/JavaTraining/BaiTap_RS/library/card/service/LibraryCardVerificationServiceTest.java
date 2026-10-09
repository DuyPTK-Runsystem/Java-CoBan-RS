package com.JavaTraining.BaiTap_RS.library.card.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.requests.ReqVerifyLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryCardVerificationServiceTest {

    private static final String SECRET = "plan097-test-secret-at-least-32-bytes";
    private static final String CARD_NO = "LC-2026-000001";
    private static final Long PATRON_ID = 4L;
    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private LibraryCardRepository cardRepository;

    @Mock
    private LibraryPatronRepository patronRepository;

    private final LibraryCardQrService qrService = new LibraryCardQrService();

    private LibraryCardVerificationService service;

    private LibraryPatron patron;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(qrService, "signingSecret", SECRET);
        service = new LibraryCardVerificationService(cardRepository, patronRepository, qrService);
        patron = new LibraryPatron(15L, LocalDateTime.of(2026, 10, 9, 10, 0));
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
    }

    @Test
    void verifiesCardOnInclusiveExpiryDateAndRejectsOnFollowingDay() throws Exception {
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        when(cardRepository.findByCardNo(CARD_NO)).thenReturn(Optional.of(card(today)));
        when(patronRepository.findById(PATRON_ID)).thenReturn(Optional.of(patron));

        assertTrue(service.verify(new ReqVerifyLibraryCardDTO(payload(today))).valid());

        LocalDate yesterday = today.minusDays(1);
        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.verify(new ReqVerifyLibraryCardDTO(payload(yesterday))));

        assertEquals("CARD_EXPIRED", exception.getCode());
    }

    @Test
    void rejectsTamperedSignatureBeforeDatabaseLookup() throws Exception {
        String[] fields = payload(LocalDate.now(LIBRARY_ZONE)).split("\\|", -1);
        fields[4] = "0".repeat(64);

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.verify(new ReqVerifyLibraryCardDTO(String.join("|", fields))));

        assertEquals("CARD_SIGNATURE_INVALID", exception.getCode());
        verify(cardRepository, never()).findByCardNo(anyString());
    }

    @Test
    void rejectsRevokedCardEvenWhenItsSignatureIsValid() throws Exception {
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        LibraryCard card = card(today);
        card.revoke("replaced", LocalDateTime.now());
        when(cardRepository.findByCardNo(CARD_NO)).thenReturn(Optional.of(card));

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.verify(new ReqVerifyLibraryCardDTO(payload(today))));

        assertEquals("CARD_REVOKED", exception.getCode());
        verify(patronRepository, never()).findById(PATRON_ID);
    }

    @Test
    void rejectsMalformedPayloadBeforeCheckingItsSignature() {
        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.verify(new ReqVerifyLibraryCardDTO("v2|broken")));

        assertEquals("CARD_PAYLOAD_MALFORMED", exception.getCode());
        verify(cardRepository, never()).findByCardNo(anyString());
    }

    private LibraryCard card(LocalDate expiresAt) {
        return new LibraryCard(PATRON_ID, CARD_NO, LocalDateTime.now(), expiresAt, "v1", "test");
    }

    private String payload(LocalDate expiresAt) throws Exception {
        String content = "v1|" + CARD_NO + "|" + PATRON_ID + "|" + expiresAt.toEpochDay();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return content + "|" + HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }
}

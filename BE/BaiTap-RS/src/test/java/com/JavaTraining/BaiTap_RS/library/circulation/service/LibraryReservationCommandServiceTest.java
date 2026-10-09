package com.JavaTraining.BaiTap_RS.library.circulation.service;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryReservationCommandServiceTest {

    private static final Long BOOK_ID = 101L;
    private static final Long PATRON_ID = 12L;
    private static final Long USER_ID = 17L;

    @Mock private LibraryReservationRepository reservationRepository;
    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository copyRepository;
    @Mock private LibraryPolicyService policyService;
    @Mock private LibraryCirculationMapper mapper;
    @Mock private LibraryOperationAuditService auditService;
    @Mock private LibraryReservationExpirationService expirationService;
    @Mock private LibraryReservationCommandEligibilityService eligibilityService;

    @Test
    void expiresBeforeCheckingAvailabilityAndDuplicateThenCreatesWaitingReservation() {
        LibraryReservationCommandService service = service();
        LibraryCirculationPolicy policy = Mockito.mock(LibraryCirculationPolicy.class);
        LibraryReservation persisted = Mockito.mock(LibraryReservation.class);
        LibraryReservationDTO expected = new LibraryReservationDTO(1L, BOOK_ID, "Title", PATRON_ID,
                ReservationStatus.WAITING, null, null, null, null, null, null, "LIB-POL-1");
        when(eligibilityService.lockAndValidateCreationEligibility(BOOK_ID, USER_ID)).thenReturn(PATRON_ID);
        when(policyService.resolveCurrent(any())).thenReturn(policy);
        when(policy.getPolicyVersion()).thenReturn("LIB-POL-1");
        when(policy.getReservationPickupDays()).thenReturn(3);
        when(persisted.getId()).thenReturn(1L);
        when(reservationRepository.save(any(LibraryReservation.class))).thenReturn(persisted);
        when(mapper.reservation(persisted)).thenReturn(expected);

        try (MockedStatic<AuditContext> auditContext = Mockito.mockStatic(AuditContext.class)) {
            auditContext.when(AuditContext::currentUserId).thenReturn(USER_ID);
            assertSame(expected, service.create(new ReqReservationDTO(BOOK_ID)));
        }

        InOrder order = inOrder(eligibilityService, expirationService, reservationRepository);
        order.verify(eligibilityService).lockAndValidateCreationEligibility(BOOK_ID, USER_ID);
        order.verify(expirationService).expireForBook(Mockito.eq(BOOK_ID), any(), Mockito.eq(USER_ID));
        order.verify(eligibilityService).validateCreationAvailabilityAndDuplicate(BOOK_ID, PATRON_ID);
        order.verify(reservationRepository).save(any(LibraryReservation.class));
        verify(auditService).record("RESERVATION_CREATED", "library_reservation", 1L, null,
                Map.of("bookId", BOOK_ID, "patronId", PATRON_ID));
    }

    @Test
    void doesNotCreateOrAuditWhenPostExpiryAvailabilityCheckRejectsRequest() {
        LibraryReservationCommandService service = service();
        when(eligibilityService.lockAndValidateCreationEligibility(BOOK_ID, USER_ID)).thenReturn(PATRON_ID);
        Mockito.doThrow(new LibraryCirculationException(org.springframework.http.HttpStatus.CONFLICT,
                "RESERVATION_NOT_NEEDED", "Có bản sao đang sẵn sàng để mượn"))
                .when(eligibilityService).validateCreationAvailabilityAndDuplicate(BOOK_ID, PATRON_ID);

        try (MockedStatic<AuditContext> auditContext = Mockito.mockStatic(AuditContext.class)) {
            auditContext.when(AuditContext::currentUserId).thenReturn(USER_ID);
            assertThrows(LibraryCirculationException.class, () -> service.create(new ReqReservationDTO(BOOK_ID)));
        }

        verify(expirationService).expireForBook(Mockito.eq(BOOK_ID), any(), Mockito.eq(USER_ID));
        verify(reservationRepository, never()).save(any());
        verify(auditService, never()).record(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                Mockito.any(), Mockito.any());
    }

    private LibraryReservationCommandService service() {
        return new LibraryReservationCommandService(reservationRepository, bookRepository, copyRepository,
                policyService, mapper, auditService, expirationService, eligibilityService);
    }
}

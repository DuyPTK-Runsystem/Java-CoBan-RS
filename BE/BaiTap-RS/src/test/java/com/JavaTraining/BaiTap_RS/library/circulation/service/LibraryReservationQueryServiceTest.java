package com.JavaTraining.BaiTap_RS.library.circulation.service;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryReservationRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryReservationQueryServiceTest {

    private static final Long USER_ID = 71L;
    private static final Long SELF_PATRON_ID = 12L;
    private static final Long BOOK_ID = 101L;

    @Mock private LibraryReservationRepository reservationRepository;
    @Mock private LibraryPatronRepository patronRepository;
    @Mock private LibraryCirculationMapper mapper;
    @Mock private LibraryAccessPolicy accessPolicy;

    @Test
    void staffQueryCombinesBookStatusAndPatronFilters() {
        LibraryReservationQueryService service = service();
        when(accessPolicy.isManager()).thenReturn(true);
        when(reservationRepository.pageForStaff(BOOK_ID, SELF_PATRON_ID, ReservationStatus.WAITING,
                PageRequest.of(1, 10))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 11));

        var result = service.page(BOOK_ID, SELF_PATRON_ID, ReservationStatus.WAITING, 1, 10);

        verify(reservationRepository).pageForStaff(BOOK_ID, SELF_PATRON_ID, ReservationStatus.WAITING,
                PageRequest.of(1, 10));
        assertEquals(2, result.meta().totalPages());
        assertEquals(11, result.meta().totalItems());
    }

    @Test
    void patronQueryFiltersBookWithinDerivedSelfScope() {
        LibraryReservationQueryService service = service();
        LibraryPatron self = Mockito.mock(LibraryPatron.class);
        when(accessPolicy.isManager()).thenReturn(false);
        when(patronRepository.findByUserId(USER_ID)).thenReturn(Optional.of(self));
        when(self.getId()).thenReturn(SELF_PATRON_ID);
        when(reservationRepository.pageForPatron(BOOK_ID, SELF_PATRON_ID, ReservationStatus.READY,
                PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of()));

        try (MockedStatic<AuditContext> auditContext = Mockito.mockStatic(AuditContext.class)) {
            auditContext.when(AuditContext::currentUserId).thenReturn(USER_ID);
            var result = service.page(BOOK_ID, null, ReservationStatus.READY, 0, 20);
            assertEquals(0, result.meta().totalItems());
        }

        verify(reservationRepository).pageForPatron(BOOK_ID, SELF_PATRON_ID, ReservationStatus.READY,
                PageRequest.of(0, 20));
    }

    @Test
    void patronCannotUseFilterToReadAnotherPatronsReservations() {
        LibraryReservationQueryService service = service();
        LibraryPatron self = Mockito.mock(LibraryPatron.class);
        when(accessPolicy.isManager()).thenReturn(false);
        when(patronRepository.findByUserId(USER_ID)).thenReturn(Optional.of(self));
        when(self.getId()).thenReturn(SELF_PATRON_ID);

        try (MockedStatic<AuditContext> auditContext = Mockito.mockStatic(AuditContext.class)) {
            auditContext.when(AuditContext::currentUserId).thenReturn(USER_ID);
            LibraryCirculationException error = assertThrows(LibraryCirculationException.class,
                    () -> service.page(BOOK_ID, 99L, null, 0, 20));
            assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        }

        verify(reservationRepository, Mockito.never()).pageForPatron(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any());
    }

    private LibraryReservationQueryService service() {
        return new LibraryReservationQueryService(reservationRepository, patronRepository, mapper, accessPolicy);
    }
}

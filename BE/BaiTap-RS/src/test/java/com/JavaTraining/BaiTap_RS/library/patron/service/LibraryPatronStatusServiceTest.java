package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests.ReqUpdateLibraryPatronStatusDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryPatronStatusServiceTest {

    private static final Long USER_ID = 15L;
    private static final Long PATRON_ID = 4L;

    @Mock
    private LibraryPatronRepository patronRepository;

    @Mock
    private LibraryPatronReadModelService readModelService;

    @Mock
    private LibraryPatronSuspensionService suspensionService;

    @Mock
    private LibraryOperationAuditService auditService;

    @InjectMocks
    private LibraryPatronStatusService service;

    @Test
    void suspendsPatronWithRequiredReasonAndRecordsAudit() {
        LibraryPatron patron = patron(LibraryPatronStatus.ACTIVE);
        ReqUpdateLibraryPatronStatusDTO request =
                new ReqUpdateLibraryPatronStatusDTO(LibraryPatronStatus.BORROWING_SUSPENDED, " Manual review ");
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));
        when(readModelService.suspensionReasons(PATRON_ID)).thenReturn(List.of());
        when(readModelService.patron(patron)).thenReturn(response(patron, LibraryPatronStatus.BORROWING_SUSPENDED));

        ResLibraryPatronDTO result = service.updateStatus(PATRON_ID, request);

        assertEquals(LibraryPatronStatus.BORROWING_SUSPENDED, result.status());
        verify(suspensionService).suspendManually(eq(PATRON_ID), eq("Manual review"),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
        verify(auditService).record(eq("LIBRARY_PATRON_STATUS_CHANGED"), eq("library_patron"), eq(PATRON_ID),
                anyMap(), anyMap());
    }

    @Test
    void requiresAReasonForSuspension() {
        LibraryPatron patron = patron(LibraryPatronStatus.ACTIVE);
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.updateStatus(PATRON_ID,
                        new ReqUpdateLibraryPatronStatusDTO(LibraryPatronStatus.BORROWING_SUSPENDED, " ")));

        assertEquals("PATRON_STATUS_REASON_REQUIRED", exception.getCode());
        verify(suspensionService, never()).suspendManually(eq(PATRON_ID), eq(" "),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void closedPatronCannotBeReopened() {
        LibraryPatron patron = patron(LibraryPatronStatus.CLOSED);
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.updateStatus(PATRON_ID,
                        new ReqUpdateLibraryPatronStatusDTO(LibraryPatronStatus.ACTIVE, null)));

        assertEquals("PATRON_STATUS_CONFLICT", exception.getCode());
        verify(suspensionService, never()).resolveManual(eq(PATRON_ID), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void activeRequestResolvesManualSuspensionAndActivatesWhenNoOtherReasonRemains() {
        LibraryPatron patron = patron(LibraryPatronStatus.BORROWING_SUSPENDED);
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));
        when(readModelService.suspensionReasons(PATRON_ID)).thenReturn(List.of("MANUAL"), List.of());
        when(suspensionService.resolveManual(eq(PATRON_ID), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(true);
        when(suspensionService.hasUnresolved(PATRON_ID)).thenReturn(false);
        when(readModelService.patron(patron)).thenReturn(response(patron, LibraryPatronStatus.ACTIVE));

        ResLibraryPatronDTO result = service.updateStatus(PATRON_ID,
                new ReqUpdateLibraryPatronStatusDTO(LibraryPatronStatus.ACTIVE, null));

        assertEquals(LibraryPatronStatus.ACTIVE, result.status());
        assertEquals(LibraryPatronStatus.ACTIVE, patron.getStatus());
        verify(suspensionService).resolveManual(eq(PATRON_ID), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
        verify(auditService).record(eq("LIBRARY_PATRON_STATUS_CHANGED"), eq("library_patron"), eq(PATRON_ID),
                anyMap(), anyMap());
    }

    @Test
    void activeRequestKeepsFineSuspensionAndEligibilityStatus() {
        LibraryPatron patron = patron(LibraryPatronStatus.BORROWING_SUSPENDED);
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));
        when(readModelService.suspensionReasons(PATRON_ID)).thenReturn(List.of("FINE"));
        when(suspensionService.resolveManual(eq(PATRON_ID), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(false);
        when(suspensionService.hasUnresolved(PATRON_ID)).thenReturn(true);
        when(readModelService.patron(patron)).thenReturn(response(patron, LibraryPatronStatus.BORROWING_SUSPENDED));

        ResLibraryPatronDTO result = service.updateStatus(PATRON_ID,
                new ReqUpdateLibraryPatronStatusDTO(LibraryPatronStatus.ACTIVE, null));

        assertEquals(LibraryPatronStatus.BORROWING_SUSPENDED, result.status());
        assertEquals(LibraryPatronStatus.BORROWING_SUSPENDED, patron.getStatus());
        verify(suspensionService, never()).suspendManually(eq(PATRON_ID), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
        verify(auditService, never()).record(eq("LIBRARY_PATRON_STATUS_CHANGED"), eq("library_patron"),
                eq(PATRON_ID), anyMap(), anyMap());
    }

    @Test
    void activeRequestResolvesManualButRetainsSuspendedStatusForFineReason() {
        LibraryPatron patron = patron(LibraryPatronStatus.BORROWING_SUSPENDED);
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));
        when(readModelService.suspensionReasons(PATRON_ID)).thenReturn(List.of("MANUAL", "FINE"), List.of("FINE"));
        when(suspensionService.resolveManual(eq(PATRON_ID), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(true);
        when(suspensionService.hasUnresolved(PATRON_ID)).thenReturn(true);
        when(readModelService.patron(patron)).thenReturn(response(patron, LibraryPatronStatus.BORROWING_SUSPENDED));

        ResLibraryPatronDTO result = service.updateStatus(PATRON_ID,
                new ReqUpdateLibraryPatronStatusDTO(LibraryPatronStatus.ACTIVE, null));

        assertEquals(LibraryPatronStatus.BORROWING_SUSPENDED, result.status());
        assertEquals(LibraryPatronStatus.BORROWING_SUSPENDED, patron.getStatus());
        verify(auditService).record(eq("LIBRARY_PATRON_STATUS_CHANGED"), eq("library_patron"), eq(PATRON_ID),
                anyMap(), anyMap());
    }

    private LibraryPatron patron(LibraryPatronStatus status) {
        LibraryPatron patron = new LibraryPatron(USER_ID, LocalDateTime.of(2026, 10, 9, 10, 0));
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
        ReflectionTestUtils.setField(patron, "status", status);
        return patron;
    }

    private ResLibraryPatronDTO response(LibraryPatron patron, LibraryPatronStatus status) {
        return new ResLibraryPatronDTO(patron.getId(), patron.getUserId(), "User", status,
                patron.getJoinedAt(), List.of(), null);
    }
}

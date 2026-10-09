package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronSuspension;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronSuspensionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryPatronSuspensionServiceTest {

    private static final Long PATRON_ID = 4L;
    private static final Long ACTOR_ID = 15L;

    @Mock
    private LibraryPatronSuspensionRepository repository;

    @InjectMocks
    private LibraryPatronSuspensionService service;

    @Test
    void resolvesOnlyManualSuspensions() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 9, 10, 0);
        LibraryPatronSuspension manual = new LibraryPatronSuspension(PATRON_ID, "Manual review", "MANUAL",
                now.minusDays(1));
        when(repository.findAllByPatronIdAndResolvedAtIsNullAndSourceOrderBySuspendedAtAsc(PATRON_ID, "MANUAL"))
                .thenReturn(List.of(manual));

        assertTrue(service.resolveManual(PATRON_ID, ACTOR_ID, now));

        assertNotNull(manual.getResolvedAt());
        assertEquals(ACTOR_ID, manual.getResolvedBy());
        verify(repository).findAllByPatronIdAndResolvedAtIsNullAndSourceOrderBySuspendedAtAsc(PATRON_ID, "MANUAL");
        verify(repository).flush();
    }
}

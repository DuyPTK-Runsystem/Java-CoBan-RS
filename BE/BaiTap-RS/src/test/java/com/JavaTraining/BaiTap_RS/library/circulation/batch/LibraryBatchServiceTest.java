package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryBatchRun;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryBatchRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.ExitStatus;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryBatchServiceTest {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job libraryOverdueFineJob;

    @Mock
    private LibraryBatchRunCreationService runCreationService;

    @Mock
    private LibraryBatchRunRepository runRepository;

    @Mock
    private LibraryBatchRunStatusService runStatusService;

    @InjectMocks
    private LibraryBatchService service;

    @Test
    void acceptsPastDateAndLaunchesJob() throws Exception {
        LocalDate pastDate = LocalDate.now(LIBRARY_ZONE).minusDays(1);
        LibraryBatchRun run = createRun(pastDate, 10L);
        when(runCreationService.createOrRestart(eq(pastDate), any())).thenReturn(run);
        JobExecution execution = org.mockito.Mockito.mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.COMPLETED);
        when(jobLauncher.run(eq(libraryOverdueFineJob), any(JobParameters.class))).thenReturn(execution);
        when(runRepository.findById(10L)).thenReturn(Optional.of(run));

        var dto = service.run(pastDate);

        assertEquals(pastDate, dto.runDate());
        verify(runCreationService).createOrRestart(eq(pastDate), any());
        verify(jobLauncher).run(eq(libraryOverdueFineJob), any(JobParameters.class));
    }

    @Test
    void acceptsCurrentDateAndLaunchesJob() throws Exception {
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        LibraryBatchRun run = createRun(today, 11L);
        when(runCreationService.createOrRestart(eq(today), any())).thenReturn(run);
        JobExecution execution = org.mockito.Mockito.mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.COMPLETED);
        when(jobLauncher.run(eq(libraryOverdueFineJob), any(JobParameters.class))).thenReturn(execution);
        when(runRepository.findById(11L)).thenReturn(Optional.of(run));

        var dto = service.run(today);

        assertEquals(today, dto.runDate());
        verify(runCreationService).createOrRestart(eq(today), any());
        verify(jobLauncher).run(eq(libraryOverdueFineJob), any(JobParameters.class));
    }

    @Test
    void rejectsFutureDateWithoutLaunchingJobOrMutatingData() throws Exception {
        LocalDate futureDate = LocalDate.now(LIBRARY_ZONE).plusDays(1);

        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.run(futureDate));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("INVALID_BATCH_RUN_DATE", exception.getCode());
        verify(runCreationService, never()).createOrRestart(any(), any());
        verify(jobLauncher, never()).run(any(), any());
        verify(runStatusService, never()).fail(any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    private LibraryBatchRun createRun(LocalDate runDate, Long id) {
        LibraryBatchRun run = new LibraryBatchRun("libraryOverdueFineJob", runDate, 1L, LocalDateTime.now(LIBRARY_ZONE));
        org.springframework.test.util.ReflectionTestUtils.setField(run, "id", id);
        return run;
    }
}

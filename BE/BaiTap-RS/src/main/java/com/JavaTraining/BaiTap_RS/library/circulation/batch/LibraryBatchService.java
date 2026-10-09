package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryBatchRunDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryBatchRun;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryBatchRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryBatchService {

    private final JobLauncher jobLauncher;
    @Qualifier("libraryOverdueFineJob")
    private final Job libraryOverdueFineJob;
    private final LibraryBatchRunCreationService runCreationService;
    private final LibraryBatchRunRepository runRepository;
    private final LibraryBatchRunStatusService runStatusService;

    public LibraryBatchService(JobLauncher jobLauncher,
            @Qualifier("libraryOverdueFineJob") Job libraryOverdueFineJob,
            LibraryBatchRunCreationService runCreationService, LibraryBatchRunRepository runRepository,
            LibraryBatchRunStatusService runStatusService) {
        this.jobLauncher = jobLauncher;
        this.libraryOverdueFineJob = libraryOverdueFineJob;
        this.runCreationService = runCreationService;
        this.runRepository = runRepository;
        this.runStatusService = runStatusService;
    }

    public LibraryBatchRunDTO run(LocalDate runDate) {
        Long actorId = AuditContext.currentUserId();
        LibraryBatchRun run = runCreationService.createOrRestart(runDate, actorId);
        JobParameters parameters = new JobParametersBuilder()
                .addString("runDate", runDate.toString())
                .addLong("runId", run.getId())
                .addLong("actorId", actorId)
                .toJobParameters();
        try {
            JobExecution execution = jobLauncher.run(libraryOverdueFineJob, parameters);
            if (execution.getStatus().isUnsuccessful()) {
                throw new LibraryCirculationException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "LIBRARY_BATCH_FAILED", "Không thể hoàn tất đợt tính phí trễ hạn");
            }
        } catch (JobExecutionAlreadyRunningException exception) {
            throw new LibraryCirculationException(HttpStatus.CONFLICT,
                    "LIBRARY_BATCH_ALREADY_RUNNING", "Đợt tính phí trễ hạn đang được xử lý", exception);
        } catch (JobInstanceAlreadyCompleteException | JobRestartException
                | InvalidJobParametersException | org.springframework.core.task.TaskRejectedException exception) {
            runStatusService.fail(run.getId(), 0, 0);
            throw new LibraryCirculationException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "LIBRARY_BATCH_FAILED", "Không thể chạy đợt tính phí trễ hạn", exception);
        }
        return dto(runRepository.findById(run.getId()).orElse(run));
    }

    @Transactional(readOnly = true)
    public List<LibraryBatchRunDTO> history() {
        return runRepository.findOrderedByStartedAt(PageRequest.of(0, 50)).stream().map(this::dto).toList();
    }

    public LibraryBatchRunDTO dto(LibraryBatchRun run) {
        return new LibraryBatchRunDTO(run.getId(), run.getJobName(), run.getRunDate(), run.getStatus(),
                run.getProcessedCount(), run.getSkippedCount(), run.getStartedAt(), run.getCompletedAt());
    }
}

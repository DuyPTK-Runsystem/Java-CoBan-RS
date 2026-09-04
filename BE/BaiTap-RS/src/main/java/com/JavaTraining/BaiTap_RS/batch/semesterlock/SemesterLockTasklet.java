package com.JavaTraining.BaiTap_RS.batch.semesterlock;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.LockSource;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterLockRun;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterLockRunStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterLockRunRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.service.SemesterCompletenessService;
import com.JavaTraining.BaiTap_RS.academic.service.SemesterLockService;
import com.JavaTraining.BaiTap_RS.academic.service.SemesterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class SemesterLockTasklet implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemesterLockTasklet.class);
    private static final Set<Integer> CHECKPOINTS = Set.of(-45, -30, -14, -7, -3, -1, 0, 1, 3, 7, 14);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SemesterRepository semesterRepository;
    private final SemesterLockRunRepository lockRunRepository;
    private final SemesterLockService lockService;
    private final SemesterCompletenessService completenessService;
    private final SemesterService semesterService;

    public SemesterLockTasklet(
            SemesterRepository semesterRepository,
            SemesterLockRunRepository lockRunRepository,
            SemesterLockService lockService,
            SemesterCompletenessService completenessService,
            SemesterService semesterService) {
        this.semesterRepository = semesterRepository;
        this.lockRunRepository = lockRunRepository;
        this.lockService = lockService;
        this.completenessService = completenessService;
        this.semesterService = semesterService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String dateStr = (String) chunkContext.getStepContext().getJobParameters().get("businessDate");
        LocalDate businessDate = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now(VIETNAM_ZONE);
        Long batchExecutionId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting SemesterLockTasklet for business date: {}", businessDate);
        }

        SemesterLockRun run = new SemesterLockRun(businessDate, batchExecutionId, SemesterLockRunStatus.RUNNING);
        run = lockRunRepository.save(run);

        try {
            List<Semester> semesters = semesterRepository.findAllByStatusIn(
                    List.of(SemesterStatus.ACTIVE, SemesterStatus.LOCKED));

            for (Semester semester : semesters) {
                processSemester(run, semester, businessDate);
            }

            run.markSucceeded();
            lockRunRepository.save(run);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("SemesterLockTasklet completed successfully for run ID: {}", run.getId());
            }
        } catch (Exception exception) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("SemesterLockTasklet failed for run ID {}: {}", run.getId(), exception.getMessage());
            }
            run.markFailed(exception.getMessage());
            lockRunRepository.save(run);
            throw exception;
        }

        return RepeatStatus.FINISHED;
    }

    private void processSemester(SemesterLockRun run, Semester semester, LocalDate businessDate) {
        LocalDate effectiveLockDate = semesterService.calculateEffectiveLockDate(semester);
        int offset = (int) ChronoUnit.DAYS.between(effectiveLockDate, businessDate);

        if (CHECKPOINTS.contains(offset)) {
            String checkpointCode;
            if (offset == 0) {
                checkpointCode = "t";
            } else if (offset > 0) {
                checkpointCode = "t+" + offset + "d";
            } else {
                checkpointCode = "t" + offset + "d";
            }
            String correlationId = "BATCH-RUN-" + run.getId() + "-SEM-" + semester.getId() + "-" + checkpointCode;

            if (offset == 0 && semester.getStatus() == SemesterStatus.ACTIVE) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Locking semester {} at checkpoint t", semester.getId());
                }
                lockService.lockSemester(
                        semester.getId(),
                        LockSource.AUTOMATIC,
                        null,
                        "Tự động khóa học kỳ theo lịch",
                        correlationId);
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Evaluating completeness for semester {} at checkpoint {}",
                        semester.getId(), checkpointCode);
            }
            completenessService.evaluateAndSaveReport(
                    run.getId(),
                    semester.getId(),
                    checkpointCode,
                    correlationId);
        }
    }
}

package com.JavaTraining.BaiTap_RS.batch.semesterlock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
                "PMD.UnitTestContainsTooManyAsserts",
                "PMD.ExcessiveImports"
})
class SemesterLockSchedulerAndBatchTest {

        @Mock
        private SemesterRepository semesterRepository;

        @Mock
        private SemesterLockRunRepository lockRunRepository;

        @Mock
        private SemesterLockService lockService;

        @Mock
        private SemesterCompletenessService completenessService;

        @Mock
        private SemesterService semesterService;

        @Mock
        private JobLauncher jobLauncher;

        @Mock
        private Job semesterLockJob;

        private SemesterLockTasklet tasklet;
        private SemesterLockScheduler scheduler;

        @BeforeEach
        void setUp() {
                tasklet = new SemesterLockTasklet(
                                semesterRepository,
                                lockRunRepository,
                                lockService,
                                completenessService,
                                semesterService);
                scheduler = new SemesterLockScheduler(jobLauncher, semesterLockJob);
        }

        @Test
        void taskletExecutesAutoLockAtCheckpointTAndEvaluatesCompleteness() {
                SemesterLockRun run = new SemesterLockRun(LocalDate.of(2027, 2, 14), 999L,
                                SemesterLockRunStatus.RUNNING);
                ReflectionTestUtils.setField(run, "id", 10L);

                Mockito.when(lockRunRepository.save(Mockito.any(SemesterLockRun.class)))
                                .thenAnswer(inv -> {
                                        SemesterLockRun r = inv.getArgument(0);
                                        if (r.getId() == null) {
                                                ReflectionTestUtils.setField(r, "id", 10L);
                                        }
                                        return r;
                                });

                Semester sem = new Semester(
                                10L,
                                "HK1",
                                "Học kỳ 1",
                                1,
                                LocalDate.of(2026, 8, 15),
                                LocalDate.of(2026, 12, 31),
                                LocalDateTime.of(2027, 2, 14, 0, 0),
                                SemesterStatus.ACTIVE);
                ReflectionTestUtils.setField(sem, "id", 1L);

                Mockito.when(semesterRepository.findAllByStatusIn(Mockito.anyList()))
                                .thenReturn(List.of(sem));
                Mockito.when(semesterService.calculateEffectiveLockDate(sem))
                                .thenReturn(LocalDate.of(2027, 2, 14));

                StepExecution stepExecution = Mockito.mock(StepExecution.class);
                Mockito.when(stepExecution.getJobExecutionId()).thenReturn(999L);

                StepContext stepContext = Mockito.mock(StepContext.class);
                Mockito.when(stepContext.getJobParameters()).thenReturn(Map.of("businessDate", "2027-02-14"));
                Mockito.when(stepContext.getStepExecution()).thenReturn(stepExecution);

                ChunkContext chunkContext = Mockito.mock(ChunkContext.class);
                Mockito.when(chunkContext.getStepContext()).thenReturn(stepContext);

                StepContribution contribution = Mockito.mock(StepContribution.class);

                RepeatStatus status = tasklet.execute(contribution, chunkContext);

                Assertions.assertEquals(RepeatStatus.FINISHED, status, "tasklet status should be FINISHED");

                Mockito.verify(lockService).lockSemester(
                                Mockito.eq(1L),
                                Mockito.eq(LockSource.AUTOMATIC),
                                Mockito.isNull(),
                                Mockito.anyString(),
                                Mockito.anyString());

                Mockito.verify(completenessService).evaluateAndSaveReport(
                                Mockito.eq(10L),
                                Mockito.eq(1L),
                                Mockito.eq("t"),
                                Mockito.anyString());
        }

        @Test
        void schedulerLaunchesJobSuccessfully() throws Exception {
                JobExecution expectedExecution = Mockito.mock(JobExecution.class);
                Mockito.when(jobLauncher.run(Mockito.eq(semesterLockJob), Mockito.any(JobParameters.class)))
                                .thenReturn(expectedExecution);

                JobExecution execution = scheduler.runJobForDate(LocalDate.of(2027, 2, 14));
                Assertions.assertEquals(expectedExecution, execution, "job execution should match expected");
        }
}

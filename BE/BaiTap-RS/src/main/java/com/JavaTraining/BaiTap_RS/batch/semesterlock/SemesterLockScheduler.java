package com.JavaTraining.BaiTap_RS.batch.semesterlock;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SemesterLockScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemesterLockScheduler.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JobLauncher jobLauncher;
    private final Job semesterLockJob;

    public SemesterLockScheduler(JobLauncher jobLauncher, Job semesterLockJob) {
        this.jobLauncher = jobLauncher;
        this.semesterLockJob = semesterLockJob;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void runDailySemesterLock() {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scheduled daily semester lock job triggering for date: {}", today);
        }
        runJobForDate(today);
    }

    public JobExecution runJobForDate(LocalDate businessDate) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("businessDate", businessDate.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            return jobLauncher.run(semesterLockJob, params);
        } catch (JobExecutionAlreadyRunningException
                | JobInstanceAlreadyCompleteException
                | JobRestartException
                | InvalidJobParametersException exception) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to launch semesterLockJob for date {}: {}", businessDate, exception.getMessage());
            }
            throw new IllegalStateException("Failed to launch semesterLockJob", exception);
        }
    }
}

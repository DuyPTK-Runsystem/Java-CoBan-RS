package com.JavaTraining.BaiTap_RS.batch.semesterlock;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SemesterLockBatchConfiguration {

    @Bean
    public Job semesterLockJob(JobRepository jobRepository, Step semesterLockStep) {
        return new JobBuilder("semesterLockJob", jobRepository)
                .start(semesterLockStep)
                .build();
    }

    @Bean
    public Step semesterLockStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SemesterLockTasklet semesterLockTasklet) {
        return new StepBuilder("semesterLockStep", jobRepository)
                .tasklet(semesterLockTasklet, transactionManager)
                .build();
    }
}

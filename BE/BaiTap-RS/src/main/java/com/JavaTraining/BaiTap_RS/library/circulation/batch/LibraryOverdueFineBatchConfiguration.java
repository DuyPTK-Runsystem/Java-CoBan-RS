package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import java.time.LocalDate;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class LibraryOverdueFineBatchConfiguration {

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job libraryOverdueFineJob(JobRepository jobRepository, Step libraryOverdueFineExpiryStep,
            Step libraryOverdueFineStep, LibraryOverdueFineJobListener listener) {
        return new JobBuilder("libraryOverdueFineJob", jobRepository)
                .start(libraryOverdueFineExpiryStep)
                .next(libraryOverdueFineStep)
                .listener(listener)
                .build();
    }

    @Bean
    public Step libraryOverdueFineExpiryStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager, LibraryOverdueFineTasklet tasklet) {
        return new StepBuilder("libraryOverdueFineExpiryStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step libraryOverdueFineStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            LibraryOverdueFineItemReader reader, ItemProcessor<Long, Long> processor,
            ItemWriter<Long> writer) {
        return new StepBuilder("libraryOverdueFineStep", jobRepository)
                .<Long, Long>chunk(CHUNK_SIZE)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(LibraryOverdueFineItemException.class)
                .skipLimit(25)
                .build();
    }

    @Bean
    @StepScope
    public LibraryOverdueFineItemReader libraryOverdueFineItemReader(
            com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository loanRepository,
            @Value("#{jobParameters['runDate']}") String runDate) {
        return new LibraryOverdueFineItemReader(loanRepository, LocalDate.parse(runDate));
    }

    @Bean
    @StepScope
    public LibraryOverdueFineItemProcessor libraryOverdueFineItemProcessor(LibraryOverdueFineItemService itemService,
            @Value("#{jobParameters['runDate']}") String runDate,
            @Value("#{jobParameters['actorId']}") Long actorId) {
        return new LibraryOverdueFineItemProcessor(itemService, LocalDate.parse(runDate), actorId);
    }
}

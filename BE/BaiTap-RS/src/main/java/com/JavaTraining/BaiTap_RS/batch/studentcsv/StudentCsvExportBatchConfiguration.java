package com.JavaTraining.BaiTap_RS.batch.studentcsv;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class StudentCsvExportBatchConfiguration {

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job studentCsvExportJob(JobRepository jobRepository, Step studentCsvExportStep) {
        return new JobBuilder("studentCsvExportJob", jobRepository)
                .start(studentCsvExportStep)
                .build();
    }

    @Bean
    public Step studentCsvExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaPagingItemReader<StudentCsvExportRow> studentCsvExportReader,
            StudentCsvExportItemProcessor studentCsvExportProcessor,
            StudentCsvExportWriter studentCsvExportWriter) {
        return new StepBuilder("studentCsvExportStep", jobRepository)
                .<StudentCsvExportRow, StudentCsvExportRow>chunk(CHUNK_SIZE, transactionManager)
                .reader(studentCsvExportReader)
                .processor(studentCsvExportProcessor)
                .writer(studentCsvExportWriter)
                .faultTolerant()
                .skip(StudentCsvExportItemException.class)
                .skipLimit(Integer.MAX_VALUE)
                .listener(studentCsvExportWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<StudentCsvExportRow> studentCsvExportReader(EntityManagerFactory entityManagerFactory) {
        JpaPagingItemReader<StudentCsvExportRow> reader = new JpaPagingItemReader<>(entityManagerFactory);
        reader.setName("studentCsvExportReader");
        reader.setPageSize(CHUNK_SIZE);
        reader.setQueryString("""
                select new com.JavaTraining.BaiTap_RS.batch.studentcsv.StudentCsvExportRow(
                    student.id,
                    student.studentName,
                    student.studentCode,
                    studentInfo.address,
                    studentInfo.averageScore,
                    studentInfo.dateOfBirth)
                from Student student
                join student.studentInfo studentInfo
                order by student.id
                """);
        return reader;
    }

    @Bean
    @StepScope
    public StudentCsvExportWriter studentCsvExportWriter(StudentCsvExportResultStore resultStore) {
        return new StudentCsvExportWriter(resultStore);
    }
}

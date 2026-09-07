package com.JavaTraining.BaiTap_RS.batch.studentcsv;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class StudentCsvExportService {

    private final JobLauncher jobLauncher;
    private final Job studentCsvExportJob;
    private final StudentCsvExportResultStore resultStore;

    public StudentCsvExportService(
            JobLauncher jobLauncher,
            @Qualifier("studentCsvExportJob") Job studentCsvExportJob,
            StudentCsvExportResultStore resultStore) {
        this.jobLauncher = jobLauncher;
        this.studentCsvExportJob = studentCsvExportJob;
        this.resultStore = resultStore;
    }

    public byte[] exportStudents() {
        JobExecution execution = runExportJob();
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            resultStore.discard(execution.getId());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xuất CSV sinh viên");
        }
        byte[] content = resultStore.take(execution.getId());
        if (content == null) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy nội dung CSV đã xuất");
        }
        return content;
    }

    private JobExecution runExportJob() {
        JobParameters parameters = new JobParametersBuilder()
                .addLong("requestedAt", System.nanoTime())
                .toJobParameters();
        try {
            return jobLauncher.run(studentCsvExportJob, parameters);
        } catch (JobExecutionAlreadyRunningException
                | JobInstanceAlreadyCompleteException
                | JobRestartException
                | InvalidJobParametersException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể khởi chạy export CSV", exception);
        }
    }
}

package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class LibraryOverdueFineJobListener implements JobExecutionListener {

    private final LibraryBatchRunStatusService runStatusService;

    public LibraryOverdueFineJobListener(LibraryBatchRunStatusService runStatusService) {
        this.runStatusService = runStatusService;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Long rawRunId = jobExecution.getJobParameters().getLong("runId");
        if (rawRunId == null) {
            return;
        }
        long processed = jobExecution.getStepExecutions().stream()
                .mapToLong(org.springframework.batch.core.step.StepExecution::getWriteCount).sum();
        long skipped = jobExecution.getStepExecutions().stream()
                .mapToLong(org.springframework.batch.core.step.StepExecution::getSkipCount).sum();
        Long runId = rawRunId;
        if (jobExecution.getStatus().isUnsuccessful()) {
            runStatusService.fail(runId, processed, skipped);
        } else {
            runStatusService.complete(runId, processed, skipped);
        }
    }
}

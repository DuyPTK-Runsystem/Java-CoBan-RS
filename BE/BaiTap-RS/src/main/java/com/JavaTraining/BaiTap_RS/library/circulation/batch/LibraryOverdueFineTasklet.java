package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class LibraryOverdueFineTasklet implements Tasklet {

    private final LibraryOverdueFineRunService runService;

    public LibraryOverdueFineTasklet(LibraryOverdueFineRunService runService) {
        this.runService = runService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Long actorId = Long.valueOf(chunkContext.getStepContext().getJobParameters().get("actorId").toString());
        runService.expireReservations(actorId);
        return RepeatStatus.FINISHED;
    }
}

package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.UUID;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTask;
import com.JavaTraining.BaiTap_RS.scorebook.repository.CalculationTaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Background poller for durable calculation tasks. */
@Component
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class CalculationTaskWorker {

    private final CalculationTaskService taskService;
    private final TranscriptRecalculationService recalculationService;
    private final CalculationTaskRepository taskRepository;
    private final String workerId = "scorebook-worker-" + UUID.randomUUID();

    public CalculationTaskWorker(
            CalculationTaskService taskService,
            TranscriptRecalculationService recalculationService,
            CalculationTaskRepository taskRepository) {
        this.taskService = taskService;
        this.recalculationService = recalculationService;
        this.taskRepository = taskRepository;
    }

    @Scheduled(fixedDelayString = "${app.calculation.worker-interval-ms:5000}")
    public void processNextTask() {
        Long taskId = taskService.claimNextTask(workerId);
        if (taskId == null) {
            return;
        }
        CalculationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        try {
            recalculationService.recalculate(
                    task.getStudentId(),
                    task.getAcademicYearId(),
                    task.getRequestedVersion(),
                    task.getId());
            taskService.markSucceeded(taskId);
        } catch (RuntimeException failure) {
            taskService.markFailed(taskId, failure);
        }
    }
}

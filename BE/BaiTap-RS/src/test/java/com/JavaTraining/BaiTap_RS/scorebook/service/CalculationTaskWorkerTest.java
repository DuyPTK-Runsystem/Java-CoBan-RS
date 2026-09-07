package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTask;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskType;
import com.JavaTraining.BaiTap_RS.scorebook.repository.CalculationTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class CalculationTaskWorkerTest {

    @Mock
    private CalculationTaskService taskService;
    @Mock
    private TranscriptRecalculationService recalculationService;
    @Mock
    private CalculationTaskRepository taskRepository;

    @Test
    void claimsAndCompletesAvailableTask() {
        CalculationTask task = task(101L);
        Mockito.when(taskService.claimNextTask(Mockito.anyString())).thenReturn(101L);
        Mockito.when(taskRepository.findById(101L)).thenReturn(Optional.of(task));
        CalculationTaskWorker worker = new CalculationTaskWorker(taskService, recalculationService, taskRepository);

        worker.processNextTask();

        Mockito.verify(recalculationService).recalculate(200L, 10L, 3L, 101L);
        Mockito.verify(taskService).markSucceeded(101L);
        Mockito.verify(taskService, Mockito.never()).markFailed(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void recordsFailureWhenRecalculationThrows() {
        CalculationTask task = task(102L);
        Mockito.when(taskService.claimNextTask(Mockito.anyString())).thenReturn(102L);
        Mockito.when(taskRepository.findById(102L)).thenReturn(Optional.of(task));
        Mockito.doThrow(new IllegalStateException("calculation failed"))
                .when(recalculationService)
                .recalculate(200L, 10L, 3L, 102L);
        CalculationTaskWorker worker = new CalculationTaskWorker(taskService, recalculationService, taskRepository);

        worker.processNextTask();

        Mockito.verify(taskService).markFailed(Mockito.eq(102L), Mockito.any(IllegalStateException.class));
        Mockito.verify(taskService, Mockito.never()).markSucceeded(102L);
    }

    @Test
    void doesNothingWhenNoTaskIsAvailable() {
        Mockito.when(taskService.claimNextTask(Mockito.anyString())).thenReturn(null);
        CalculationTaskWorker worker = new CalculationTaskWorker(taskService, recalculationService, taskRepository);

        worker.processNextTask();

        Mockito.verifyNoInteractions(recalculationService, taskRepository);
    }

    @Test
    void doesNotExecuteWhenClaimedTaskCannotBeLoaded() {
        Mockito.when(taskService.claimNextTask(Mockito.anyString())).thenReturn(103L);
        Mockito.when(taskRepository.findById(103L)).thenReturn(Optional.empty());
        CalculationTaskWorker worker = new CalculationTaskWorker(taskService, recalculationService, taskRepository);

        worker.processNextTask();

        Mockito.verifyNoInteractions(recalculationService);
        Mockito.verify(taskService, Mockito.never()).markSucceeded(Mockito.anyLong());
        Mockito.verify(taskService, Mockito.never()).markFailed(Mockito.anyLong(), Mockito.any());
    }

    private static CalculationTask task(Long id) {
        CalculationTask task = new CalculationTask(
                200L,
                10L,
                CalculationTaskType.STUDENT_YEAR_RECALC,
                3L,
                "RECALC:200:10");
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "createdAt", LocalDateTime.now());
        return task;
    }
}

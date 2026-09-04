package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResCalculationTaskDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTask;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.CalculationTaskRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals",
        "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.UnitTestContainsTooManyAsserts"
})
class CalculationTaskServiceTest {

    private static final Long TASK_ID = 101L;
    private static final Long STUDENT_ID = 200L;
    private static final Long ACADEMIC_YEAR_ID = 10L;

    @Mock
    private CalculationTaskRepository taskRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentLookupService studentLookupService;
    @Mock
    private TranscriptStateService transcriptStateService;
    @Mock
    private StudentAnnualTranscriptRepository annualTranscriptRepository;
    @Mock
    private ScorebookAuditService auditService;

    private CalculationTaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new CalculationTaskService(
                taskRepository,
                studentRepository,
                studentLookupService,
                transcriptStateService,
                annualTranscriptRepository,
                auditService);
    }

    @Test
    void claimsPendingTaskAndIncrementsAttempt() {
        CalculationTask task = task();
        Mockito.when(taskRepository.findAvailableForUpdate(
                Mockito.eq(CalculationTaskStatus.PENDING),
                Mockito.any(),
                Mockito.any(PageRequest.class)))
                .thenReturn(List.of(task));

        Long claimedId = taskService.claimNextTask("worker-1");

        Assertions.assertEquals(TASK_ID, claimedId);
        Assertions.assertEquals(CalculationTaskStatus.RUNNING, task.getStatus());
        Assertions.assertEquals(1, task.getAttemptCount());
        Assertions.assertEquals("worker-1", task.getWorkerId());
    }

    @Test
    void retriesFailureUntilMaxAttemptsThenMarksTaskFailed() {
        CalculationTask task = task();
        Mockito.when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

        task.claim("worker-1", java.time.LocalDateTime.now());
        taskService.markFailed(TASK_ID, new IllegalStateException("first failure"));
        Assertions.assertEquals(CalculationTaskStatus.PENDING, task.getStatus());
        Assertions.assertEquals("first failure", task.getLastError());

        task.claim("worker-1", java.time.LocalDateTime.now());
        taskService.markFailed(TASK_ID, new IllegalStateException("second failure"));
        task.claim("worker-1", java.time.LocalDateTime.now());
        taskService.markFailed(TASK_ID, new IllegalStateException("final failure"));

        Assertions.assertEquals(CalculationTaskStatus.FAILED, task.getStatus());
        Assertions.assertEquals("final failure", task.getLastError());
        Assertions.assertNotNull(task.getCompletedAt());
    }

    @Test
    void requeuesTaskWhenSourceVersionAdvancedBeforeSuccess() {
        CalculationTask task = task();
        StudentAnnualTranscript annual = annualTranscript(5L);
        Mockito.when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.of(annual));

        taskService.markSucceeded(TASK_ID);

        Assertions.assertEquals(CalculationTaskStatus.PENDING, task.getStatus());
        Assertions.assertEquals(5L, task.getRequestedVersion());
        Assertions.assertEquals(0, task.getAttemptCount());
    }

    @Test
    void marksTaskSucceededWhenSourceVersionMatches() {
        CalculationTask task = task();
        StudentAnnualTranscript annual = annualTranscript(3L);
        Mockito.when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.of(annual));

        taskService.markSucceeded(TASK_ID);

        Assertions.assertEquals(CalculationTaskStatus.SUCCEEDED, task.getStatus());
        Assertions.assertNotNull(task.getCompletedAt());
    }

    @Test
    void mergesNewVersionIntoExistingIdempotentTask() {
        CalculationTask task = task();
        Mockito.when(taskRepository.findByIdempotencyKey("RECALC:200:10"))
                .thenReturn(Optional.of(task));

        CalculationTask result = taskService.ensureRecalcTask(STUDENT_ID, ACADEMIC_YEAR_ID, 4L);

        Assertions.assertSame(task, result);
        Assertions.assertEquals(4L, task.getRequestedVersion());
        Assertions.assertEquals(CalculationTaskStatus.PENDING, task.getStatus());
        Assertions.assertEquals(0, task.getAttemptCount());
        Mockito.verify(taskRepository, Mockito.never()).save(Mockito.any(CalculationTask.class));
    }

    @Test
    void retriesFailedTaskWithCurrentSourceVersionAndStudentCode() {
        CalculationTask task = task();
        task.setStatus(CalculationTaskStatus.FAILED);
        StudentAnnualTranscript annual = annualTranscript(6L);
        Student student = new Student("Học sinh 200", "HS200");
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);
        Mockito.when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.of(annual));
        Mockito.when(studentRepository.findAllById(List.of(STUDENT_ID))).thenReturn(List.of(student));

        ResCalculationTaskDTO response = taskService.retryTask(TASK_ID);

        Assertions.assertEquals(CalculationTaskStatus.PENDING, task.getStatus());
        Assertions.assertEquals(6L, response.requestedVersion());
        Assertions.assertEquals("HS200", response.studentCode());
        Mockito.verify(auditService).writeAudit(
                Mockito.eq("CALCULATION_TASK_RETRIED"),
                Mockito.eq("calculation_task"),
                Mockito.eq(TASK_ID),
                Mockito.any(),
                Mockito.any());
    }

    @Test
    void rejectsRetryForNonFailedTask() {
        CalculationTask task = task();
        Mockito.when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

        com.JavaTraining.BaiTap_RS.common.error.AppException exception = Assertions.assertThrows(
                com.JavaTraining.BaiTap_RS.common.error.AppException.class,
                () -> taskService.retryTask(TASK_ID));

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void retriesAllFailedTasksAndLeavesNoFailedTaskBehind() {
        CalculationTask first = task();
        CalculationTask second = new CalculationTask(
                STUDENT_ID + 1,
                ACADEMIC_YEAR_ID,
                CalculationTaskType.STUDENT_YEAR_RECALC,
                3L,
                "RECALC:201:10");
        ReflectionTestUtils.setField(second, "id", TASK_ID + 1);
        first.setStatus(CalculationTaskStatus.FAILED);
        second.setStatus(CalculationTaskStatus.FAILED);
        Mockito.when(taskRepository.findAllByStatusOrderByCreatedAtAsc(CalculationTaskStatus.FAILED))
                .thenReturn(List.of(first, second));
        Mockito.when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(first));
        Mockito.when(taskRepository.findById(TASK_ID + 1)).thenReturn(Optional.of(second));
        Mockito.when(studentRepository.findAllById(Mockito.anyList())).thenReturn(List.of());
        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(Mockito.anyLong(),
                Mockito.eq(ACADEMIC_YEAR_ID))).thenReturn(Optional.empty());

        List<ResCalculationTaskDTO> responses = taskService.retryAllFailedTasks();

        Assertions.assertEquals(2, responses.size());
        Assertions.assertEquals(CalculationTaskStatus.PENDING, first.getStatus());
        Assertions.assertEquals(CalculationTaskStatus.PENDING, second.getStatus());
        Mockito.verify(auditService, Mockito.times(2)).writeAudit(
                Mockito.eq("CALCULATION_TASK_RETRIED"),
                Mockito.eq("calculation_task"),
                Mockito.anyLong(),
                Mockito.any(),
                Mockito.any());
    }

    private static CalculationTask task() {
        CalculationTask task = new CalculationTask(
                STUDENT_ID,
                ACADEMIC_YEAR_ID,
                CalculationTaskType.STUDENT_YEAR_RECALC,
                3L,
                "RECALC:200:10");
        ReflectionTestUtils.setField(task, "id", TASK_ID);
        return task;
    }

    private static StudentAnnualTranscript annualTranscript(long sourceVersion) {
        StudentAnnualTranscript annual = new StudentAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
        annual.setSourceVersion(sourceVersion);
        annual.setCalculationStatus(CalculationStatus.IN_PROGRESS);
        return annual;
    }
}

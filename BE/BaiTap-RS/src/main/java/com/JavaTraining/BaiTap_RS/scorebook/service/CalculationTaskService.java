package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterCalculationTaskDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResCalculationTaskDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTask;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskType;
import com.JavaTraining.BaiTap_RS.scorebook.repository.CalculationTaskRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * NFR-CALC-005: Tạo hoặc gộp calculation task theo idempotency key.
 */
@Service
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GuardLogStatement"})
public class CalculationTaskService {

    private final CalculationTaskRepository taskRepository;
    private final StudentRepository studentRepository;
    private final StudentLookupService studentLookupService;
    private final TranscriptStateService transcriptStateService;
    private final StudentAnnualTranscriptRepository annualTranscriptRepository;
    private final ScorebookAuditService auditService;

    public CalculationTaskService(
            CalculationTaskRepository taskRepository,
            StudentRepository studentRepository,
            StudentLookupService studentLookupService,
            TranscriptStateService transcriptStateService,
            StudentAnnualTranscriptRepository annualTranscriptRepository,
            ScorebookAuditService auditService) {
        this.taskRepository = taskRepository;
        this.studentRepository = studentRepository;
        this.studentLookupService = studentLookupService;
        this.transcriptStateService = transcriptStateService;
        this.annualTranscriptRepository = annualTranscriptRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CalculationTask ensureRecalcTask(Long studentId, Long academicYearId, long sourceVersion) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.ensureRecalcTask");
        String key = "RECALC:" + studentId + ":" + academicYearId;

        return taskRepository.findByIdempotencyKey(key)
                .map(existing -> mergeTask(existing, sourceVersion))
                .orElseGet(() -> createTask(studentId, academicYearId, sourceVersion, key));
    }

    @Transactional
    public Long claimNextTask(String workerId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.claimNextTask");
        List<CalculationTask> tasks = taskRepository.findAvailableForUpdate(
                CalculationTaskStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, 1));
        if (tasks.isEmpty()) {
            return null;
        }
        CalculationTask task = tasks.get(0);
        task.claim(workerId, LocalDateTime.now());
        return task.getId();
    }

    @Transactional
    public void markSucceeded(Long taskId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.markSucceeded");
        CalculationTask task = findTask(taskId);
        long currentVersion = annualTranscriptRepository
                .findByStudentIdAndAcademicYearId(task.getStudentId(), task.getAcademicYearId())
                .map(com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript::getSourceVersion)
                .orElse(task.getRequestedVersion());
        if (currentVersion > task.getRequestedVersion()) {
            task.updateRequestedVersion(currentVersion);
        } else {
            task.markSucceeded(LocalDateTime.now());
        }
    }

    @Transactional
    public void markFailed(Long taskId, Throwable failure) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.markFailed");
        CalculationTask task = findTask(taskId);
        String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
        String error = message.length() > 2000 ? message.substring(0, 2000) : message;
        if (task.getAttemptCount() < task.getMaxAttempts()) {
            task.scheduleRetry(error, LocalDateTime.now());
        } else {
            task.markFailed(error, LocalDateTime.now());
        }
    }

    @Transactional
    public ResCalculationTaskDTO retryTask(Long taskId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.retryTask");
        CalculationTask task = findTask(taskId);
        if (task.getStatus() != CalculationTaskStatus.FAILED) {
            throw new AppException(HttpStatus.CONFLICT, "Chỉ calculation task FAILED mới được retry");
        }
        Map<String, Object> before = taskAuditData(task);
        long sourceVersion = annualTranscriptRepository
                .findByStudentIdAndAcademicYearId(task.getStudentId(), task.getAcademicYearId())
                .map(com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript::getSourceVersion)
                .orElse(task.getRequestedVersion());
        task.updateRequestedVersion(sourceVersion);
        auditService.writeAudit(
                "CALCULATION_TASK_RETRIED",
                "calculation_task",
                task.getId(),
                before,
                taskAuditData(task));
        return toResponse(task, findStudents(List.of(task.getStudentId())));
    }

    @Transactional(readOnly = true)
    public Page<ResCalculationTaskDTO> findTasks(ReqFilterCalculationTaskDTO filter) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.findTasks");
        Long studentId = filter.getStudentId();
        if (filter.getStudentCode() != null && !filter.getStudentCode().isBlank()) {
            Student student = studentLookupService.resolveStudent(studentId, filter.getStudentCode());
            studentId = student.getId();
        }
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CalculationTask> page = taskRepository.findAll(
                CalculationTaskSpecifications.from(filter, studentId), pageable);
        Map<Long, Student> students = findStudents(page.getContent().stream()
                .map(CalculationTask::getStudentId)
                .distinct()
                .toList());
        return page.map(task -> toResponse(task, students));
    }

    @Transactional(readOnly = true)
    public Page<ResCalculationTaskDTO> findFailedTasks(ReqFilterCalculationTaskDTO filter) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.findFailedTasks");
        filter.setStatus(CalculationTaskStatus.FAILED);
        return findTasks(filter);
    }

    @Transactional
    public List<ResCalculationTaskDTO> retryAllFailedTasks() {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.retryAllFailedTasks");
        return taskRepository.findAllByStatusOrderByCreatedAtAsc(CalculationTaskStatus.FAILED).stream()
                .map(task -> retryTask(task.getId()))
                .toList();
    }

    @Transactional
    public ResCalculationTaskDTO requestRecalculation(String studentCode, Long academicYearId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.requestRecalculation");
        Student student = studentLookupService.resolveStudent(null, studentCode);
        return requestRecalculation(student, academicYearId);
    }

    @Transactional
    public ResCalculationTaskDTO requestRecalculation(Long studentId, Long academicYearId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskService.class,
                "CalculationTaskService.requestRecalculation");
        Student student = studentLookupService.resolveStudent(studentId, null);
        return requestRecalculation(student, academicYearId);
    }

    private ResCalculationTaskDTO requestRecalculation(Student student, Long academicYearId) {
        long sourceVersion = transcriptStateService.touchAnnualTranscript(student.getId(), academicYearId);
        CalculationTask task = ensureRecalcTask(student.getId(), academicYearId, sourceVersion);
        auditService.writeAudit(
                "CALCULATION_TASK_RECALCULATE_REQUESTED",
                "calculation_task",
                task.getId(),
                null,
                taskAuditData(task));
        return toResponse(task, Map.of(student.getId(), student));
    }

    private CalculationTask mergeTask(CalculationTask existing, long sourceVersion) {
        if (existing.getStatus() == CalculationTaskStatus.PENDING
                || existing.getStatus() == CalculationTaskStatus.FAILED
                || existing.getStatus() == CalculationTaskStatus.SUCCEEDED) {
            existing.updateRequestedVersion(sourceVersion);
        }
        return existing;
    }

    private CalculationTask createTask(Long studentId, Long academicYearId, long sourceVersion, String key) {
        CalculationTask task = new CalculationTask(
                studentId,
                academicYearId,
                CalculationTaskType.STUDENT_YEAR_RECALC,
                sourceVersion,
                key);
        return taskRepository.save(task);
    }

    private CalculationTask findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy calculation task"));
    }

    private Map<Long, Student> findStudents(List<Long> studentIds) {
        return studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
    }

    private ResCalculationTaskDTO toResponse(CalculationTask task, Map<Long, Student> students) {
        Student student = students.get(task.getStudentId());
        return new ResCalculationTaskDTO(
                task.getId(),
                task.getStudentId(),
                student == null ? null : student.getStudentCode(),
                task.getAcademicYearId(),
                task.getTaskType(),
                task.getRequestedVersion(),
                task.getStatus(),
                task.getAttemptCount(),
                task.getMaxAttempts(),
                task.getAvailableAt(),
                task.getLockedAt(),
                task.getWorkerId(),
                task.getLastError(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getCompletedAt());
    }

    private Map<String, Object> taskAuditData(CalculationTask task) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", task.getId());
        data.put("studentId", task.getStudentId());
        data.put("academicYearId", task.getAcademicYearId());
        data.put("requestedVersion", task.getRequestedVersion());
        data.put("status", task.getStatus());
        data.put("attemptCount", task.getAttemptCount());
        return data;
    }
}

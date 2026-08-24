package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTask;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskType;
import com.JavaTraining.BaiTap_RS.scorebook.repository.CalculationTaskRepository;
import org.springframework.stereotype.Service;

/**
 * NFR-CALC-005: Tạo hoặc gộp calculation task theo idempotency key.
 */
@Service
public class CalculationTaskService {

    private final CalculationTaskRepository taskRepository;

    public CalculationTaskService(CalculationTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void ensureRecalcTask(Long studentId, Long academicYearId, long sourceVersion) {
        String key = "RECALC:" + studentId + ":" + academicYearId;

        taskRepository.findByIdempotencyKey(key).ifPresentOrElse(
                existing -> mergeTask(existing, sourceVersion),
                () -> createTask(studentId, academicYearId, sourceVersion, key));
    }

    private void mergeTask(CalculationTask existing, long sourceVersion) {
        if (existing.getStatus() == CalculationTaskStatus.PENDING
                || existing.getStatus() == CalculationTaskStatus.FAILED) {
            existing.updateRequestedVersion(sourceVersion);
            taskRepository.save(existing);
        }
    }

    private void createTask(Long studentId, Long academicYearId, long sourceVersion, String key) {
        CalculationTask task = new CalculationTask(
                studentId,
                academicYearId,
                CalculationTaskType.STUDENT_YEAR_RECALC,
                sourceVersion,
                key);
        taskRepository.save(task);
    }
}

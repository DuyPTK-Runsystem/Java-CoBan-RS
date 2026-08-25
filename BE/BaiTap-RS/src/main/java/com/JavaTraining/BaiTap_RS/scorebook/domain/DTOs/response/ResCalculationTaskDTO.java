package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskType;

public record ResCalculationTaskDTO(
        Long taskId,
        Long studentId,
        String studentCode,
        Long academicYearId,
        CalculationTaskType taskType,
        Long requestedVersion,
        CalculationTaskStatus status,
        Integer attemptCount,
        Integer maxAttempts,
        LocalDateTime availableAt,
        LocalDateTime lockedAt,
        String workerId,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {
}

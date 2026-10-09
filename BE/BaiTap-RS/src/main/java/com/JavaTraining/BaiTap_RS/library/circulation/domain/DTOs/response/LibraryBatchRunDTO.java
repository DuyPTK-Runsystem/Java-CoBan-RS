package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LibraryBatchRunDTO(Long runId, String jobName, LocalDate runDate, String status,
        long processedCount, long skippedCount, LocalDateTime startedAt, LocalDateTime completedAt) {
}

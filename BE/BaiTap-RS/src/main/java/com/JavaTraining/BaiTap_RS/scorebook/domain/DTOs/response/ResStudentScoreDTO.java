package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;

public record ResStudentScoreDTO(
                Long scoreId,
                Long assessmentColumnId,
                Long studentId,
                String studentCode,
                String studentName,
                ScoreStatus scoreStatus,
                BigDecimal scoreValue,
                String note,
                Long enteredBy,
                LocalDateTime enteredAt,
                Long updatedBy,
                LocalDateTime updatedAt,
                Long version) {
}

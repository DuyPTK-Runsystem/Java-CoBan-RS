package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
public class ReqFilterScoreAuditLogDTO {

    private String entityType;
    private String entityId;
    private Long studentId;
    private String studentCode;
    private String action;
    private Long actorUserId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromOccurredAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toOccurredAt;

    @PositiveOrZero
    private int page;

    @Positive
    @Max(50)
    private int size = 10;
}

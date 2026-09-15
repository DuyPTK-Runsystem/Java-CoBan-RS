package com.JavaTraining.BaiTap_RS.lessonlog.domain.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class LessonLogEntryStateFields extends LessonLogEntryContentFields {
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LessonLogStatus status = LessonLogStatus.DRAFT;
    private LocalDateTime submittedAt;
    private Long submittedBy;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
    private String reviewComment;
    @Version private Long version = 0L;
    @Column(nullable = false) private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

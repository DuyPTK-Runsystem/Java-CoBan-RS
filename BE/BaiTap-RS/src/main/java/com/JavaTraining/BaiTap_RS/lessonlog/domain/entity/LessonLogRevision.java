package com.JavaTraining.BaiTap_RS.lessonlog.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lesson_log_revision")
public class LessonLogRevision {

    public LessonLogRevision(Long entryId, String action, Long actorId, String reason,
            String beforeStateJson, String afterStateJson) {
        this.entryId = entryId;
        this.action = action;
        this.actorId = actorId;
        this.reason = reason;
        this.beforeStateJson = beforeStateJson;
        this.afterStateJson = afterStateJson;
        this.createdAt = LocalDateTime.now();
    }

    public LessonLogRevision(Long entryId, Long weeklyReviewId, Long policyId, String action,
            Long actorId, String reason, String beforeStateJson, String afterStateJson) {
        this.entryId = entryId;
        this.weeklyReviewId = weeklyReviewId;
        this.policyId = policyId;
        this.action = action;
        this.actorId = actorId;
        this.reason = reason;
        this.beforeStateJson = beforeStateJson;
        this.afterStateJson = afterStateJson;
        this.createdAt = LocalDateTime.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revision_id")
    private Long id;

    @Column(name = "entry_id")
    private Long entryId;

    @Column(name = "weekly_review_id")
    private Long weeklyReviewId;

    @Column(name = "policy_id")
    private Long policyId;

    @Column(nullable = false)
    private String action;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    private String reason;

    @Column(name = "before_state_json", columnDefinition = "json")
    private String beforeStateJson;

    @Column(name = "after_state_json", nullable = false, columnDefinition = "json")
    private String afterStateJson;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

package com.JavaTraining.BaiTap_RS.scorebook.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(
        name = "scorebook",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_scorebook_class_subject",
                columnNames = "class_subject_id"))
public class Scorebook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scorebook_id", nullable = false)
    private Long id;

    @Column(name = "class_subject_id", nullable = false)
    private Long classSubjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScorebookStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Scorebook(Long classSubjectId, ScorebookStatus status) {
        this.classSubjectId = classSubjectId;
        this.status = status;
    }

    public void open() {
        status = ScorebookStatus.OPEN;
    }

    public void publish(Long actorId, LocalDateTime publishedAt) {
        status = ScorebookStatus.PUBLISHED;
        publishedBy = actorId;
        this.publishedAt = publishedAt;
    }

    @PrePersist
    /* default */ void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    /* default */ void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

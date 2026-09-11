package com.JavaTraining.BaiTap_RS.placement.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "placement_session")
public class PlacementSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placement_session_id", nullable = false)
    private Long id;
    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;
    @Column(name = "target_grade_id", nullable = false)
    private Long targetGradeId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PlacementSessionStatus status;
    @Column(name = "rule_version", nullable = false, length = 50)
    private String ruleVersion;
    @Column(name = "scope_snapshot", nullable = false, columnDefinition = "JSON")
    private String scopeSnapshot;
    @Version
    @Column(name = "expected_version", nullable = false)
    private Long version = 0L;
    @Column(name = "confirm_idempotency_key", length = 100)
    private String confirmIdempotencyKey;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PlacementSession(Long academicYearId, Long targetGradeId, String ruleVersion,
            String scopeSnapshot, Long createdBy) {
        this.academicYearId = academicYearId;
        this.targetGradeId = targetGradeId;
        this.ruleVersion = ruleVersion;
        this.scopeSnapshot = scopeSnapshot;
        this.createdBy = createdBy;
        this.status = PlacementSessionStatus.DRAFT;
    }
    /* default */
    @jakarta.persistence.PrePersist
    void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    /* default */
    @jakarta.persistence.PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}

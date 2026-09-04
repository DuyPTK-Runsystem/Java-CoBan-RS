package com.JavaTraining.BaiTap_RS.scorebook.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "skill_weight_config",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_skill_weight_scorebook",
                columnNames = "scorebook_id"))
public class SkillWeightConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_weight_config_id", nullable = false)
    private Long id;

    @Column(name = "scorebook_id", nullable = false)
    private Long scorebookId;

    @Column(name = "kttt_weight_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ktttWeightPercent;

    @Column(name = "ktdk_weight_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ktdkWeightPercent;

    @Column(name = "ktck_weight_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ktckWeightPercent;

    @Column(name = "configured_by", nullable = false)
    private Long configuredBy;

    @Column(name = "configured_at", nullable = false)
    private LocalDateTime configuredAt;

    @Column(name = "locked_by")
    private Long lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    public SkillWeightConfig(
            Long scorebookId,
            BigDecimal ktttWeightPercent,
            BigDecimal ktdkWeightPercent,
            BigDecimal ktckWeightPercent,
            Long configuredBy,
            LocalDateTime configuredAt) {
        this.scorebookId = scorebookId;
        this.ktttWeightPercent = ktttWeightPercent;
        this.ktdkWeightPercent = ktdkWeightPercent;
        this.ktckWeightPercent = ktckWeightPercent;
        this.configuredBy = configuredBy;
        this.configuredAt = configuredAt;
    }

    public void update(
            BigDecimal ktttWeightPercent,
            BigDecimal ktdkWeightPercent,
            BigDecimal ktckWeightPercent,
            Long configuredBy,
            LocalDateTime configuredAt) {
        this.ktttWeightPercent = ktttWeightPercent;
        this.ktdkWeightPercent = ktdkWeightPercent;
        this.ktckWeightPercent = ktckWeightPercent;
        this.configuredBy = configuredBy;
        this.configuredAt = configuredAt;
    }

    public void lock(Long lockedBy, LocalDateTime lockedAt) {
        this.lockedBy = lockedBy;
        this.lockedAt = lockedAt;
    }

    @PrePersist
    /* default */ void onCreate() {
        if (configuredAt == null) {
            configuredAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    /* default */ void onUpdate() {
        if (configuredAt == null) {
            configuredAt = LocalDateTime.now();
        }
    }
}

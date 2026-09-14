package com.JavaTraining.BaiTap_RS.timetable.domain.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "teacher_load_rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_tlr_policy_code", columnNames = {"policy_id", "rule_code"}))
public class TeacherLoadRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id", nullable = false)
    private Long id;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "rule_code", nullable = false, length = 80)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private TeacherLoadRuleTriggerType triggerType;

    @Column(name = "reduction_periods", nullable = false)
    private Integer reductionPeriods;

    @Column(name = "source", nullable = false, length = 255)
    private String source;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TeacherLoadRule(Long policyId, String ruleCode, String ruleName,
            TeacherLoadRuleTriggerType triggerType, Integer reductionPeriods, String source) {
        this.policyId = policyId;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.triggerType = triggerType;
        this.reductionPeriods = reductionPeriods;
        this.source = source;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

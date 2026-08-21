package com.JavaTraining.BaiTap_RS.enrollment.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// BR-ENROLL-002: transfer là append-only và không ghi đè history trước đó.
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "class_transfer_history")
public class ClassTransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id", nullable = false)
    private Long id;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "from_class_id")
    private Long fromClassId;

    @Column(name = "to_class_id", nullable = false)
    private Long toClassId;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ClassTransferHistory(
            Long enrollmentId,
            Long fromClassId,
            Long toClassId,
            LocalDateTime effectiveAt,
            String reason,
            Long approvedBy) {
        this.enrollmentId = enrollmentId;
        this.fromClassId = fromClassId;
        this.toClassId = toClassId;
        this.effectiveAt = effectiveAt;
        this.reason = reason;
        this.approvedBy = approvedBy;
    }

    @jakarta.persistence.PrePersist
    /* default */ void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

package com.JavaTraining.BaiTap_RS.placement.domain.entity;

import java.math.BigDecimal;

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

@Getter @Setter @NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity @Table(name = "placement_result")
public class PlacementResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placement_result_id", nullable = false) private Long id;
    @Column(name = "placement_session_id", nullable = false) private Long sessionId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "target_class_id") private Long targetClassId;
    @Enumerated(EnumType.STRING) @Column(name = "result_status", nullable = false, length = 30)
    private PlacementResultStatus resultStatus;
    @Column(name = "score", precision = 8, scale = 3) private BigDecimal score;
    @Column(name = "issue_code", length = 60) private String issueCode;
    @Enumerated(EnumType.STRING) @Column(name = "issue_severity", length = 20) private PlacementIssueSeverity issueSeverity;
    @Column(name = "explanation", nullable = false, length = 1000) private String explanation;
    @Version @Column(name = "result_version", nullable = false) private Long version = 0L;
    public PlacementResult(Long sessionId, Long studentId, Long targetClassId, PlacementResultStatus resultStatus,
            BigDecimal score, String issueCode, PlacementIssueSeverity issueSeverity, String explanation) {
        this.sessionId = sessionId; this.studentId = studentId; this.targetClassId = targetClassId;
        this.resultStatus = resultStatus; this.score = score; this.issueCode = issueCode;
        this.issueSeverity = issueSeverity; this.explanation = explanation;
    }
}

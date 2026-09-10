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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity @Table(name = "placement_candidate")
public class PlacementCandidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placement_candidate_id", nullable = false) private Long id;
    @Column(name = "placement_session_id", nullable = false) private Long sessionId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "target_grade_id", nullable = false) private Long targetGradeId;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 30)
    private PlacementSourceType sourceType;
    @Column(name = "score", precision = 8, scale = 3) private BigDecimal score;
    @Column(name = "score_source_reference", length = 255) private String scoreSourceReference;
    @Column(name = "gender_snapshot", length = 50) private String genderSnapshot;
    @Column(name = "eligibility_evidence", columnDefinition = "JSON") private String eligibilityEvidence;
    @Column(name = "approval_reference", length = 255) private String approvalReference;
    public PlacementCandidate(Long sessionId, Long studentId, Long targetGradeId, PlacementSourceType sourceType,
            BigDecimal score, String scoreSourceReference, String genderSnapshot, String eligibilityEvidence,
            String approvalReference) {
        this.sessionId = sessionId; this.studentId = studentId; this.targetGradeId = targetGradeId;
        this.sourceType = sourceType; this.score = score; this.scoreSourceReference = scoreSourceReference;
        this.genderSnapshot = genderSnapshot; this.eligibilityEvidence = eligibilityEvidence;
        this.approvalReference = approvalReference;
    }
}

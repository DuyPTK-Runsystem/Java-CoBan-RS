package com.JavaTraining.BaiTap_RS.timetableagent.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "timetable_agent_approval")
public class TimetableAgentApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id", nullable = false)
    private Long id;

    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;

    @Column(name = "proposal_version", nullable = false)
    private Long proposalVersion;

    @Column(name = "proposal_hash", nullable = false, length = 64)
    private String proposalHash;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "approved_at", nullable = false, updatable = false)
    private Instant approvedAt;

    public TimetableAgentApproval(Long proposalId, Long proposalVersion, String proposalHash, Long actorId) {
        this.proposalId = proposalId;
        this.proposalVersion = proposalVersion;
        this.proposalHash = proposalHash;
        this.actorId = actorId;
    }

    @PrePersist
    protected void onCreate() {
        if (approvedAt == null) {
            approvedAt = Instant.now();
        }
    }
}

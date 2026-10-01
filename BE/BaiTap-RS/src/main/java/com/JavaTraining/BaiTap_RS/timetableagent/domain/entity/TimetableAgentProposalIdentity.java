package com.JavaTraining.BaiTap_RS.timetableagent.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimetableAgentProposalIdentity {

    @Column(name = "actor_id", nullable = false)
    private Long actorId;
    @Column(name = "target_revision_id", nullable = false)
    private Long targetRevisionId;
    @Column(name = "target_semester_id", nullable = false)
    private Long targetSemesterId;
    @Column(name = "expected_version", nullable = false)
    private Long expectedVersion;
    @Column(name = "proposal_version", nullable = false)
    private Long proposalVersion;
    @Column(name = "snapshot_id", nullable = false, length = 64)
    private String snapshotId;
    @Column(name = "snapshot_fingerprint", nullable = false, length = 64)
    private String snapshotFingerprint;
    @Column(name = "proposal_hash", nullable = false, length = 64)
    private String proposalHash;
}

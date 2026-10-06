package com.JavaTraining.BaiTap_RS.timetableagent.domain.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "timetable_agent_action")
public class TimetableAgentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id", nullable = false)
    private Long id;

    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "lease_token", nullable = false, length = 36)
    private String leaseToken;

    @Column(name = "lease_expires_at", nullable = false)
    private Instant leaseExpiresAt;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "receipt_json", columnDefinition = "LONGTEXT")
    private String receiptJson;

    @Column(name = "committed_at")
    private Instant committedAt;

    public TimetableAgentAction(Long proposalId, Long actorId, String idempotencyKey,
            String requestHash, String receiptJson) {
        this.proposalId = proposalId;
        this.actorId = actorId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.status = "PENDING";
        this.leaseToken = UUID.randomUUID().toString();
        this.receiptJson = receiptJson;
    }

}

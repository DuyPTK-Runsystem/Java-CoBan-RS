package com.JavaTraining.BaiTap_RS.library.card.domain.entity;

import java.time.LocalDate;
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

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "library_card")
public class LibraryCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id", nullable = false)
    private Long id;

    @Column(name = "patron_id", nullable = false)
    private Long patronId;

    @Column(name = "card_no", nullable = false, unique = true, length = 32)
    private String cardNo;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LibraryCardStatus status;

    @Column(name = "payload_version", nullable = false, length = 8)
    private String payloadVersion;

    @Column(name = "policy_version", nullable = false, length = 64)
    private String policyVersion;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LibraryCard(Long patronId, String cardNo, LocalDateTime now, LocalDate expiresAt,
            String payloadVersion, String policyVersion) {
        this.patronId = patronId;
        this.cardNo = cardNo;
        this.issuedAt = now;
        this.expiresAt = expiresAt;
        this.status = LibraryCardStatus.ACTIVE;
        this.payloadVersion = payloadVersion;
        this.policyVersion = policyVersion;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void revoke(String reason, LocalDateTime now) {
        this.status = LibraryCardStatus.REVOKED;
        this.revokedAt = now;
        this.revokedReason = reason;
        this.updatedAt = now;
    }

    public void expire(LocalDateTime now) {
        this.status = LibraryCardStatus.EXPIRED;
        this.updatedAt = now;
    }

    protected void setPatronId(Long patronId) {
        this.patronId = patronId;
    }

    protected void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    protected void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    protected void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    protected void setPayloadVersion(String payloadVersion) {
        this.payloadVersion = payloadVersion;
    }

    protected void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    protected void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

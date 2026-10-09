package com.JavaTraining.BaiTap_RS.library.patron.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "library_patron_suspension")
public class LibraryPatronSuspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suspension_id", nullable = false)
    private Long id;

    @Column(name = "patron_id", nullable = false)
    private Long patronId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "suspended_at", nullable = false)
    private LocalDateTime suspendedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    public LibraryPatronSuspension(Long patronId, String reason, String source, LocalDateTime now) {
        this.patronId = patronId;
        this.reason = reason;
        this.source = source;
        this.suspendedAt = now;
    }

    public void resolve(Long actorId, LocalDateTime now) {
        this.resolvedAt = now;
        this.resolvedBy = actorId;
    }

    protected void setPatronId(Long patronId) {
        this.patronId = patronId;
    }

    protected void setReason(String reason) {
        this.reason = reason;
    }

    protected void setSource(String source) {
        this.source = source;
    }

    protected void setSuspendedAt(LocalDateTime suspendedAt) {
        this.suspendedAt = suspendedAt;
    }
}

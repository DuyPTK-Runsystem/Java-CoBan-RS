package com.JavaTraining.BaiTap_RS.library.patron.domain.entity;

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
@Table(name = "library_patron")
public class LibraryPatron {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patron_id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LibraryPatronStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LibraryPatron(Long userId, LocalDateTime now) {
        this.userId = userId;
        this.status = LibraryPatronStatus.ACTIVE;
        this.joinedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setStatus(LibraryPatronStatus status, LocalDateTime now) {
        this.status = status;
        this.updatedAt = now;
    }

    protected void setUserId(Long userId) {
        this.userId = userId;
    }

    protected void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    protected void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

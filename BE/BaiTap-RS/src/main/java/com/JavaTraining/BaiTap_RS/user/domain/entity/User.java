package com.JavaTraining.BaiTap_RS.user.domain.entity;

import java.time.Instant;

import com.JavaTraining.BaiTap_RS.common.util.AuditUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(
        name = "user",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_user_name", columnNames = "user_name"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long id;

    @Column(name = "user_name", nullable = false, length = 20)
    @Setter(lombok.AccessLevel.PACKAGE)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    @Setter(lombok.AccessLevel.PACKAGE)
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @PrePersist
    /* default */ void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        createdBy = AuditUtil.currentUsername();
        updatedBy = createdBy;
    }

    @PreUpdate
    /* default */ void onUpdate() {
        updatedAt = Instant.now();
        updatedBy = AuditUtil.currentUsername();
    }
}

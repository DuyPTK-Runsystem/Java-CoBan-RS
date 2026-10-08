package com.JavaTraining.BaiTap_RS.library.catalog.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "book_copy_batch_request", uniqueConstraints = @UniqueConstraint(
        name = "uk_book_copy_batch_actor_book_key",
        columnNames = { "actor_user_id", "book_id", "idempotency_key" }))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookCopyBatchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_request_id", nullable = false)
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "payload_fingerprint", nullable = false, length = 64)
    private String payloadFingerprint;

    @Column(name = "response_json", nullable = false, columnDefinition = "JSON")
    private String responseJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BookCopyBatchRequest(Long actorUserId, Long bookId, String idempotencyKey,
            String payloadFingerprint, String responseJson) {
        this.actorUserId = actorUserId;
        this.bookId = bookId;
        this.idempotencyKey = idempotencyKey;
        this.payloadFingerprint = payloadFingerprint;
        this.responseJson = responseJson;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

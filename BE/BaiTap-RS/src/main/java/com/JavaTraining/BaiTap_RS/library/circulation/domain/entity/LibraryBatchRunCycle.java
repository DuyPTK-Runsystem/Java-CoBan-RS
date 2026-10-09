package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class LibraryBatchRunCycle {

    @Column(name = "processed_count", nullable = false)
    private long processedCount;

    @Column(name = "skipped_count", nullable = false)
    private long skippedCount;

    @Transient
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public LibraryBatchRunCycle(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    @Column(name = "started_at", nullable = false)
    @Access(AccessType.PROPERTY)
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    protected void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void complete(long processedCount, long skippedCount, LocalDateTime now) {
        this.processedCount = processedCount;
        this.skippedCount = skippedCount;
        this.completedAt = now;
    }

    public void fail(long processedCount, long skippedCount, LocalDateTime now) {
        this.processedCount = processedCount;
        this.skippedCount = skippedCount;
        this.completedAt = now;
    }

    public void ensureCompletionTime(LocalDateTime now) {
        if (completedAt == null) {
            completedAt = now;
        }
    }
}

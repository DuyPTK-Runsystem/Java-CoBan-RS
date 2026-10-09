package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library_batch_run_summary")
@Getter
@NoArgsConstructor
public class LibraryBatchRun {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long id;

    // Creation metadata remains fixed after insertion; JPA field access requires non-final persistent fields.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "job_name", nullable = false, length = 120)
    private String jobName;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Embedded
    private LibraryBatchRunCycle cycle;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "actor_user_id")
    private Long actorUserId;

    public LibraryBatchRun(String jobName, LocalDate runDate, Long actorUserId, LocalDateTime startedAt) {
        this.jobName = jobName;
        this.runDate = runDate;
        this.actorUserId = actorUserId;
        this.cycle = new LibraryBatchRunCycle(startedAt);
        this.status = "STARTED";
    }

    public void complete(long processedCount, long skippedCount, LocalDateTime now) {
        this.status = "COMPLETED";
        cycle.complete(processedCount, skippedCount, now);
    }

    public void restart(LocalDateTime now) {
        status = "STARTED";
        cycle = new LibraryBatchRunCycle(now);
    }

    public void fail(long processedCount, long skippedCount, LocalDateTime now) {
        this.status = "FAILED";
        cycle.fail(processedCount, skippedCount, now);
    }

    @PreUpdate
    protected void onUpdate() {
        if (cycle.getCompletedAt() == null && !"STARTED".equals(status)) {
            cycle.ensureCompletionTime(LocalDateTime.now());
        }
    }

    public long getProcessedCount() {
        return cycle.getProcessedCount();
    }

    public long getSkippedCount() {
        return cycle.getSkippedCount();
    }

    public LocalDateTime getStartedAt() {
        return cycle.getStartedAt();
    }

    public LocalDateTime getCompletedAt() {
        return cycle.getCompletedAt();
    }
}

package com.JavaTraining.BaiTap_RS.lessonlog.domain.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lesson_log_entry", uniqueConstraints = @UniqueConstraint(
        columnNames = {"class_id", "lesson_date", "session", "period_index"}))
public class LessonLogEntry extends LessonLogEntryStateFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long id;

    public static LessonLogEntry create(LessonLogEntrySourceData source) {
        LessonLogEntry entry = new LessonLogEntry();
        entry.setTimetableEntryId(source.timetableEntryId());
        entry.setTimetableRevisionId(source.revisionId());
        entry.setAssignmentId(source.assignmentId());
        entry.setSemesterId(source.semesterId());
        entry.setClassId(source.classId());
        entry.setSubjectId(source.subjectId());
        entry.setAssignedTeacherId(source.teacherId());
        entry.setPolicyId(source.policyId());
        entry.setLessonDate(source.date());
        entry.setSession(source.session());
        entry.setPeriodIndex(source.periodIndex());
        entry.setSourceSnapshotJson(source.snapshot());
        entry.setLessonEndsAt(source.ends());
        entry.setEditWindowExpiresAt(source.expires());
        entry.setCreatedBy(source.actor());
        return entry;
    }

    @PrePersist
    /* default */
    void create() {
        LocalDateTime now = LocalDateTime.now();
        setCreatedAt(now);
        setUpdatedAt(now);
    }

    @PreUpdate
    /* default */
    void update() {
        setUpdatedAt(LocalDateTime.now());
    }
}

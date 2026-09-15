package com.JavaTraining.BaiTap_RS.lessonlog.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class LessonLogEntrySourceFields {
    @Column(name = "timetable_entry_id", nullable = false) private Long timetableEntryId;
    @Column(name = "timetable_revision_id", nullable = false) private Long timetableRevisionId;
    @Column(name = "assignment_id", nullable = false) private Long assignmentId;
    @Column(name = "semester_id", nullable = false) private Long semesterId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "subject_id", nullable = false) private Long subjectId;
    @Column(name = "assigned_teacher_id", nullable = false) private Long assignedTeacherId;
    @Column(name = "policy_id", nullable = false) private Long policyId;
    @Column(name = "lesson_date", nullable = false) private LocalDate lessonDate;
    @Column(nullable = false) private String session;
    @Column(name = "period_index", nullable = false) private Integer periodIndex;
    @Column(name = "source_snapshot_json", nullable = false, columnDefinition = "json") private String sourceSnapshotJson;
    private Integer rosterCountSnapshot;
    @Column(nullable = false) private LocalDateTime lessonEndsAt;
    @Column(nullable = false) private LocalDateTime editWindowExpiresAt;
}

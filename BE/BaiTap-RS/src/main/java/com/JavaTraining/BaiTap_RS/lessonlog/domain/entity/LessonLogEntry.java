package com.JavaTraining.BaiTap_RS.lessonlog.domain.entity;
import java.time.*;
import jakarta.persistence.*;
import lombok.AccessLevel; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
@Entity @Getter @Setter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(name="lesson_log_entry", uniqueConstraints=@UniqueConstraint(columnNames={"class_id","lesson_date","session","period_index"}))
public class LessonLogEntry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="entry_id") private Long id;
 @Column(name="timetable_entry_id",nullable=false) private Long timetableEntryId; @Column(name="timetable_revision_id",nullable=false) private Long timetableRevisionId;
 @Column(name="assignment_id",nullable=false) private Long assignmentId; @Column(name="semester_id",nullable=false) private Long semesterId; @Column(name="class_id",nullable=false) private Long classId; @Column(name="subject_id",nullable=false) private Long subjectId; @Column(name="assigned_teacher_id",nullable=false) private Long assignedTeacherId; @Column(name="policy_id",nullable=false) private Long policyId;
 @Column(name="lesson_date",nullable=false) private LocalDate lessonDate; @Column(nullable=false) private String session; @Column(name="period_index",nullable=false) private Integer periodIndex;
 @Column(name="source_snapshot_json",nullable=false,columnDefinition="json") private String sourceSnapshotJson; private Integer rosterCountSnapshot; @Column(nullable=false) private LocalDateTime lessonEndsAt; @Column(nullable=false) private LocalDateTime editWindowExpiresAt;
 private String title; @Column(columnDefinition="TEXT") private String content; private String completionStatus; private String grade; private Integer presentCount; private Integer absentCount; private String comments; private String absentStudentNotes; private String homework;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private LessonLogStatus status=LessonLogStatus.DRAFT; private LocalDateTime submittedAt; private Long submittedBy; private LocalDateTime reviewedAt; private Long reviewedBy; private String reviewComment; @Version private Long version=0L; @Column(nullable=false) private Long createdBy; private LocalDateTime createdAt; private LocalDateTime updatedAt;
 public LessonLogEntry(Long timetableEntryId,Long revisionId,Long assignmentId,Long semesterId,Long classId,Long subjectId,Long teacherId,Long policyId,LocalDate date,String session,Integer periodIndex,String snapshot,LocalDateTime ends,LocalDateTime expires,Long actor){this.timetableEntryId=timetableEntryId;this.timetableRevisionId=revisionId;this.assignmentId=assignmentId;this.semesterId=semesterId;this.classId=classId;this.subjectId=subjectId;this.assignedTeacherId=teacherId;this.policyId=policyId;this.lessonDate=date;this.session=session;this.periodIndex=periodIndex;this.sourceSnapshotJson=snapshot;this.lessonEndsAt=ends;this.editWindowExpiresAt=expires;this.createdBy=actor;}
 @PrePersist void create(){createdAt=LocalDateTime.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=LocalDateTime.now();}
}

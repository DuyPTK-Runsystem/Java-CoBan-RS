package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;

public record LessonLogEntryResponse(
        Long entryId,
        Long timetableEntryId,
        Long timetableRevisionId,
        Long assignmentId,
        Long semesterId,
        LocalDate lessonDate,
        Long classId,
        Long subjectId,
        Long assignedTeacherId,
        String className,
        String subjectName,
        String teacherName,
        String functionalRoomName,
        String session,
        Integer periodIndex,
        LocalDateTime lessonEndsAt,
        LocalDateTime editWindowExpiresAt,
        String title,
        String content,
        String completionStatus,
        String grade,
        Integer presentCount,
        Integer absentCount,
        String comments,
        String absentStudentNotes,
        String homework,
        LessonLogStatus status,
        Long version,
        Long policyId,
        List<Object> rubric,
        Integer rosterCountSnapshot,
        boolean canTeacherEdit,
        boolean canSubmit,
        boolean canReview,
        boolean canAmend,
        boolean canLateRecord,
        String blockedReason,
        LocalDateTime submittedAt,
        Long submittedBy,
        LocalDateTime reviewedAt,
        Long reviewedBy,
        String reviewComment) { }

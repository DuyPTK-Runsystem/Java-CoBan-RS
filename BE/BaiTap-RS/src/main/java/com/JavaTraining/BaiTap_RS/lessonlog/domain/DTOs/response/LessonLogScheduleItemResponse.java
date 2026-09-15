package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;

/** A timetable occurrence; entryId is zero when no lesson log exists yet. */
public record LessonLogScheduleItemResponse(
        Long entryId,
        Long timetableEntryId,
        Long timetableRevisionId,
        Long assignmentId,
        Long classId,
        Long subjectId,
        Long assignedTeacherId,
        Long semesterId,
        String className,
        String subjectName,
        String teacherName,
        String functionalRoomName,
        LocalDate lessonDate,
        String session,
        Integer periodIndex,
        LocalDateTime lessonEndsAt,
        LocalDateTime editWindowExpiresAt,
        LessonLogStatus status,
        Long version,
        Long policyId,
        Integer rosterCountSnapshot,
        boolean canCreate,
        boolean canLateRecord,
        String blockedReason,
        LessonLogEntryResponse entry) { }

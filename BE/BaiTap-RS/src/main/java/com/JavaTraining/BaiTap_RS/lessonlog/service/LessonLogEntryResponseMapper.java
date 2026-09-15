package com.JavaTraining.BaiTap_RS.lessonlog.service;

import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;

import lombok.experimental.UtilityClass;

/** Builds the transport response from values resolved by the application services. */
@UtilityClass
public class LessonLogEntryResponseMapper {
    public LessonLogEntryResponse map(LessonLogEntry entry, DisplayValues display, ResponsePermissions permissions) {
        return new LessonLogEntryResponse(entry.getId(), entry.getTimetableEntryId(), entry.getTimetableRevisionId(),
                entry.getAssignmentId(), entry.getSemesterId(), entry.getLessonDate(), entry.getClassId(),
                entry.getSubjectId(), entry.getAssignedTeacherId(), display.className(), display.subjectName(),
                display.teacherName(), display.functionalRoomName(), entry.getSession(), entry.getPeriodIndex(),
                entry.getLessonEndsAt(), entry.getEditWindowExpiresAt(), entry.getTitle(), entry.getContent(),
                entry.getCompletionStatus(), entry.getGrade(), entry.getPresentCount(), entry.getAbsentCount(),
                entry.getComments(), entry.getAbsentStudentNotes(), entry.getHomework(), entry.getStatus(),
                entry.getVersion(), entry.getPolicyId(), permissions.rubric(),
                entry.getRosterCountSnapshot(), permissions.canEdit(), permissions.canSubmit(), permissions.canReview(),
                permissions.canAmend(), false, permissions.blockedReason(), entry.getSubmittedAt(), entry.getSubmittedBy(),
                entry.getReviewedAt(), entry.getReviewedBy(), entry.getReviewComment());
    }

    public record DisplayValues(String className, String subjectName, String teacherName, String functionalRoomName) { }

    public record ResponsePermissions(boolean canEdit, boolean canSubmit, boolean canReview, boolean canAmend,
            String blockedReason, java.util.List<Object> rubric) { }
}

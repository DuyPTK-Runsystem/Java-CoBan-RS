package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

/** Determines which lifecycle actions are available for a lesson-log entry. */
public final class LessonLogEntryAuthorization {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private LessonLogEntryAuthorization() {
    }

    public static boolean canEdit(LessonLogEntry entry, TeacherRepository teachers) {
        boolean assignedTeacher = Objects.equals(LessonLogActorAuthorization.teacherIdForActor(teachers),
                entry.getAssignedTeacherId());
        return (LessonLogActorAuthorization.isManager() || assignedTeacher)
                && entry.getStatus() == LessonLogStatus.DRAFT && isOpen(entry);
    }

    public static boolean canReview(LessonLogEntry entry) {
        return LessonLogActorAuthorization.isManager()
                && (entry.getStatus() == LessonLogStatus.SUBMITTED || entry.getStatus() == LessonLogStatus.AMENDED);
    }

    public static boolean canAmend(LessonLogEntry entry) {
        return LessonLogActorAuthorization.isManager() && entry.getStatus() != LessonLogStatus.DRAFT && isOpen(entry);
    }

    public static boolean isOpen(LessonLogEntry entry) {
        boolean finalized = entry.getStatus() == LessonLogStatus.REVIEWED
                || entry.getStatus() == LessonLogStatus.AMENDED;
        return !finalized && LocalDateTime.now(BUSINESS_ZONE).isBefore(entry.getEditWindowExpiresAt());
    }

    public static String blockedReason(LessonLogEntry entry, TeacherRepository teachers) {
        if (entry.getStatus() == LessonLogStatus.REVIEWED || entry.getStatus() == LessonLogStatus.AMENDED) {
            return "Sổ đã được duyệt/điều chỉnh";
        }
        if (!isOpen(entry)) {
            return "Đã hết hạn chỉnh sửa";
        }
        boolean assignedTeacher = Objects.equals(LessonLogActorAuthorization.teacherIdForActor(teachers),
                entry.getAssignedTeacherId());
        if (entry.getStatus() == LessonLogStatus.DRAFT && !assignedTeacher) {
            return "Giáo viên không được phân công tiết này";
        }
        return null;
    }
}

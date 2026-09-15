package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

/** Protects access to entries and weekly class scope. */
public final class LessonLogScopeAuthorization {
    private LessonLogScopeAuthorization() {
    }

    public static void assertEntryReadScope(LessonLogEntry entry, TeacherRepository teachers,
            HomeroomAssignmentRepository homerooms) {
        if (LessonLogActorAuthorization.isManager()) {
            return;
        }
        Long teacherId = LessonLogActorAuthorization.teacherIdForUser(teachers,
                com.JavaTraining.BaiTap_RS.common.audit.AuditContext.currentUserId());
        boolean assignedTeacher = Objects.equals(teacherId, entry.getAssignedTeacherId());
        boolean homeroomTeacher = teacherId != null && homerooms.existsActiveHomeroomAt(entry.getClassId(), teacherId,
                AssignmentStatus.ACTIVE, entry.getLessonDate());
        if (!assignedTeacher && !homeroomTeacher) {
            throw new AppException(HttpStatus.FORBIDDEN, "Không có quyền xem sổ này");
        }
    }

    public static void assertClassScope(Long classId, LocalDate effectiveDate, TeacherRepository teachers,
            HomeroomAssignmentRepository homerooms) {
        if (LessonLogActorAuthorization.isManager()) {
            return;
        }
        Long teacherId = LessonLogActorAuthorization.teacherIdForUser(teachers,
                com.JavaTraining.BaiTap_RS.common.audit.AuditContext.currentUserId());
        boolean homeroomTeacher = teacherId != null && homerooms.existsActiveHomeroomAt(classId, teacherId,
                AssignmentStatus.ACTIVE, effectiveDate);
        if (!homeroomTeacher) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ giáo viên chủ nhiệm của lớp được xem hoặc ký tuần");
        }
    }
}

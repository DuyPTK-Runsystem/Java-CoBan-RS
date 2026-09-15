package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

/** Resolves the authenticated actor and protects teacher-assigned operations. */
public final class LessonLogActorAuthorization {
    private LessonLogActorAuthorization() {
    }

    public static boolean isManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_ACADEMIC_OFFICE".equals(authority.getAuthority()));
    }

    public static Long teacherIdForActor(TeacherRepository teachers) {
        Long userId = AuditContext.currentUserId();
        Teacher teacher = userId == null || teachers == null ? null : findTeacher(teachers, userId);
        return teacher == null ? null : teacher.getId();
    }

    public static void assertTeacherAssignment(Long teacherId, TeacherRepository teachers) {
        if (isManager()) {
            return;
        }
        Long userId = AuditContext.currentUserId();
        if (userId == null) {
            return;
        }
        Long actualTeacherId = teacherIdForUser(teachers, userId);
        if (!Objects.equals(actualTeacherId, teacherId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Giáo viên không được phân công tiết này");
        }
    }

    public static Long teacherIdForUser(TeacherRepository teachers, Long userId) {
        return userId == null ? null : teachers.findByUserId(userId).map(Teacher::getId).orElse(null);
    }

    private static Teacher findTeacher(TeacherRepository teachers, Long userId) {
        return teachers.findByUserId(userId).orElse(null);
    }
}

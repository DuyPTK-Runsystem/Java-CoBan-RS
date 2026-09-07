package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDate;
import java.time.ZoneId;

import com.JavaTraining.BaiTap_RS.assignment.service.SubjectTeachingAssignmentAccessService;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ScorebookGuard {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TeacherRepository teacherRepository;
    private final SubjectTeachingAssignmentAccessService assignmentAccessService;

    public ScorebookGuard(
            TeacherRepository teacherRepository,
            SubjectTeachingAssignmentAccessService assignmentAccessService) {
        this.teacherRepository = teacherRepository;
        this.assignmentAccessService = assignmentAccessService;
    }

    public void assertCanManage(Scorebook scorebook) {
        if (hasOfficeRole()) {
            return;
        }
        Teacher teacher = currentTeacher();
        assignmentAccessService.assertActiveAssignment(
                teacher.getId(), scorebook.getClassSubjectId(), LocalDate.now(BUSINESS_ZONE));
    }

    public void assertCanRead(Scorebook scorebook) {
        assertCanManage(scorebook);
    }

    public void assertCanReadClassSubject(Long classSubjectId) {
        if (hasOfficeRole()) {
            return;
        }
        Teacher teacher = currentTeacher();
        assignmentAccessService.assertActiveAssignment(
                teacher.getId(), classSubjectId, LocalDate.now(BUSINESS_ZONE));
    }

    private Teacher currentTeacher() {
        Long userId = AuditContext.currentUserId();
        if (userId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                throw new AppException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản chưa có ngữ cảnh giáo viên hợp lệ");
            }
            throw new AppException(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để thao tác sổ điểm");
        }
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản chưa có hồ sơ giáo viên"));
    }

    private boolean hasOfficeRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_ACADEMIC_OFFICE".equals(authority.getAuthority()));
    }
}

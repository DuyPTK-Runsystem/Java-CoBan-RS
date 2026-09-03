package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class SubjectTeachingAssignmentAccessService {

    private final SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;
    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;

    public SubjectTeachingAssignmentAccessService(
            SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository,
            TeacherRepository teacherRepository,
            HomeroomAssignmentRepository homeroomAssignmentRepository) {
        this.subjectTeachingAssignmentRepository = subjectTeachingAssignmentRepository;
        this.teacherRepository = teacherRepository;
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public void assertCanViewClass(Long classId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isOffice(authentication)) {
            return;
        }
        Long currentUserId = AuditContext.currentUserId();
        Long currentTeacherId = currentUserId == null ? null
                : teacherRepository.findByUserId(currentUserId).map(Teacher::getId).orElse(null);
        boolean homeroom = currentTeacherId != null && homeroomAssignmentRepository
                .existsByClassIdAndTeacherIdAndStatus(classId, currentTeacherId, AssignmentStatus.ACTIVE);
        boolean subjectTeacher = currentTeacherId != null && subjectTeachingAssignmentRepository
                .existsByClassIdAndTeacherIdAndStatus(classId, currentTeacherId, AssignmentStatus.ACTIVE);
        if (!homeroom && !subjectTeacher) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Giáo viên chỉ được xem lớp được phân công");
        }
    }

    private boolean isOffice(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_ACADEMIC_OFFICE".equals(authority.getAuthority()));
    }

    @Transactional(readOnly = true)
    public void assertCanViewAssignments(Long teacherId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectTeachingAssignmentAccessService.class,
                "SubjectTeachingAssignmentAccessService.assertCanViewAssignments");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean officeUser = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_ACADEMIC_OFFICE".equals(authority.getAuthority()));
        if (officeUser) {
            return;
        }
        Long currentUserId = AuditContext.currentUserId();
        Long currentTeacherId = currentUserId == null ? null
                : teacherRepository.findByUserId(currentUserId).map(Teacher::getId).orElse(null);
        if (!teacherId.equals(currentTeacherId)) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Giáo viên chỉ được xem phân công giảng dạy của chính mình");
        }
    }

    @Transactional(readOnly = true)
    public boolean hasActiveAssignment(Long teacherId, Long classSubjectId, LocalDate effectiveDate) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectTeachingAssignmentAccessService.class,
                "SubjectTeachingAssignmentAccessService.hasActiveAssignment");
        return subjectTeachingAssignmentRepository.hasActiveAssignment(teacherId, classSubjectId, effectiveDate);
    }

    @Transactional(readOnly = true)
    public void assertActiveAssignment(Long teacherId, Long classSubjectId, LocalDate effectiveDate) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectTeachingAssignmentAccessService.class,
                "SubjectTeachingAssignmentAccessService.assertActiveAssignment");
        if (!hasActiveAssignment(teacherId, classSubjectId, effectiveDate)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Giáo viên không có phân công GVBM ACTIVE");
        }
    }
}

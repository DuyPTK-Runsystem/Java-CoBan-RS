package com.JavaTraining.BaiTap_RS.student.service;

import java.util.LinkedHashSet;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentV2AccessService {

    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;
    private final ClassSubjectRepository classSubjectRepository;

    public StudentV2AccessService(
            TeacherRepository teacherRepository,
            HomeroomAssignmentRepository homeroomAssignmentRepository,
            SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository,
            ClassSubjectRepository classSubjectRepository) {
        this.teacherRepository = teacherRepository;
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        this.subjectTeachingAssignmentRepository = subjectTeachingAssignmentRepository;
        this.classSubjectRepository = classSubjectRepository;
    }

    @Transactional(readOnly = true)
    public AccessScope accessScope() {
        if (hasOfficeRole()) {
            return new AccessScope(true, Set.of());
        }
        Long userId = AuditContext.currentUserId();
        Teacher teacher = userId == null ? null : teacherRepository.findByUserId(userId).orElse(null);
        if (teacher == null) {
            return new AccessScope(false, Set.of());
        }
        Set<Long> classIds = new LinkedHashSet<>();
        classIds.addAll(homeroomAssignmentRepository.findClassIdsByTeacherIdAndStatus(
                teacher.getId(), AssignmentStatus.ACTIVE));
        subjectTeachingAssignmentRepository.findAllByTeacherIdOrderByValidFromDesc(teacher.getId()).stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
                .map(assignment -> classSubjectRepository.findById(assignment.getClassSubjectId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .forEach(classSubject -> classIds.add(classSubject.getClassId()));
        return new AccessScope(false, Set.copyOf(classIds));
    }

    public void assertCanReadStudent(Long currentClassId) {
        AccessScope scope = accessScope();
        if (scope.office()) {
            return;
        }
        if (currentClassId == null || !scope.classIds().contains(currentClassId)) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Giáo viên chỉ được xem học sinh thuộc lớp được phân công");
        }
    }

    private boolean hasOfficeRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_ACADEMIC_OFFICE".equals(authority.getAuthority()));
    }

    public record AccessScope(boolean office, Set<Long> classIds) {
    }
}

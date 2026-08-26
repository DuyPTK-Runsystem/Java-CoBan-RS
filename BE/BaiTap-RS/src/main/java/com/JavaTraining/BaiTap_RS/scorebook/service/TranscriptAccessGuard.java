package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TranscriptAccessGuard {

    private final TeacherRepository teacherRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;

    public TranscriptAccessGuard(
            TeacherRepository teacherRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            ClassSubjectRepository classSubjectRepository,
            HomeroomAssignmentRepository homeroomAssignmentRepository,
            SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository) {
        this.teacherRepository = teacherRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        this.subjectTeachingAssignmentRepository = subjectTeachingAssignmentRepository;
    }

    public void assertCanRead(Long studentId, Long academicYearId, List<Semester> semesters,
            Collection<ClassSubject> transcriptClassSubjects) {
        if (hasAnyRole("ADMIN", "ACADEMIC_OFFICE")) {
            return;
        }

        if (!hasAnyRole("TEACHER")) {
            throw new AccessDeniedException("Không có quyền xem bảng điểm của học sinh");
        }

        Long currentUserId = com.JavaTraining.BaiTap_RS.common.audit.AuditContext.currentUserId();
        Teacher teacher = teacherRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy hồ sơ giáo viên"));

        List<ClassSubject> scopedSubjects = transcriptClassSubjects.isEmpty()
                ? resolveCurrentClassSubjects(studentId, academicYearId, semesters)
                : List.copyOf(transcriptClassSubjects);

        boolean permitted = scopedSubjects.stream().anyMatch(classSubject -> semesters.stream()
                .filter(semester -> semester.getId().equals(classSubject.getSemesterId()))
                .anyMatch(semester -> hasAssignmentForSemester(teacher.getId(), classSubject, semester)));
        if (!permitted) {
            throw new AccessDeniedException("Giáo viên không được phân công trong phạm vi lớp của học sinh");
        }
    }

    private List<ClassSubject> resolveCurrentClassSubjects(
            Long studentId, Long academicYearId, List<Semester> semesters) {
        StudentYearEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy hồ sơ xếp lớp của học sinh"));
        return classSubjectRepository.findAllByClassIdInAndSemesterIdIn(
                List.of(enrollment.getCurrentClassId()),
                semesters.stream().map(Semester::getId).toList());
    }

    private boolean hasAssignmentForSemester(Long teacherId, ClassSubject classSubject, Semester semester) {
        LocalDate from = semester.getStartDate();
        LocalDate to = semester.getEndDate();
        return homeroomAssignmentRepository.existsActiveHomeroomBetween(
                classSubject.getClassId(), teacherId, AssignmentStatus.ACTIVE, from, to)
                || subjectTeachingAssignmentRepository.existsActiveAssignmentBetween(
                        teacherId, classSubject.getId(), AssignmentStatus.ACTIVE, from, to);
    }

    private boolean hasAnyRole(String... roleCodes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .anyMatch(authority -> java.util.Arrays.stream(roleCodes)
                        .anyMatch(roleCode -> ("ROLE_" + roleCode).equals(authority)));
    }
}

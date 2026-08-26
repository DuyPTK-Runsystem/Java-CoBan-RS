package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TranscriptAccessGuardTest {

    private static final Long TEACHER_USER_ID = 11L;
    private static final Long TEACHER_ID = 12L;
    private static final Long CLASS_SUBJECT_ID = 13L;
    private static final Long CLASS_ID = 14L;

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentYearEnrollmentRepository enrollmentRepository;
    @Mock
    private ClassSubjectRepository classSubjectRepository;
    @Mock
    private HomeroomAssignmentRepository homeroomAssignmentRepository;
    @Mock
    private SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;

    private TranscriptAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new TranscriptAccessGuard(teacherRepository, enrollmentRepository, classSubjectRepository,
                homeroomAssignmentRepository, subjectTeachingAssignmentRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void academicOfficeCanReadWithoutTeacherLookup() {
        authenticate("ACADEMIC_OFFICE", null);

        Assertions.assertDoesNotThrow(() -> guard.assertCanRead(100L, 200L, List.of(), List.of()));
        Mockito.verifyNoInteractions(teacherRepository);
    }

    @Test
    void assignedSubjectTeacherCanReadFullTranscriptForAssignedClass() {
        authenticate("TEACHER", TEACHER_USER_ID);
        Teacher teacher = Mockito.mock(Teacher.class);
        Mockito.when(teacher.getId()).thenReturn(TEACHER_ID);
        Mockito.when(teacherRepository.findByUserId(TEACHER_USER_ID)).thenReturn(Optional.of(teacher));
        Mockito.when(subjectTeachingAssignmentRepository.existsActiveAssignmentBetween(
                TEACHER_ID, CLASS_SUBJECT_ID, AssignmentStatus.ACTIVE, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31))).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> guard.assertCanRead(100L, 200L, List.of(semester()), List.of(classSubject())));
    }

    @Test
    void teacherOutsideClassScopeIsForbidden() {
        authenticate("TEACHER", TEACHER_USER_ID);
        Teacher teacher = Mockito.mock(Teacher.class);
        Mockito.when(teacher.getId()).thenReturn(TEACHER_ID);
        Mockito.when(teacherRepository.findByUserId(TEACHER_USER_ID)).thenReturn(Optional.of(teacher));
        Mockito.when(homeroomAssignmentRepository.existsActiveHomeroomBetween(
                CLASS_ID, TEACHER_ID, AssignmentStatus.ACTIVE, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31))).thenReturn(false);
        Mockito.when(subjectTeachingAssignmentRepository.existsActiveAssignmentBetween(
                TEACHER_ID, CLASS_SUBJECT_ID, AssignmentStatus.ACTIVE, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31))).thenReturn(false);

        Assertions.assertThrows(AccessDeniedException.class,
                () -> guard.assertCanRead(100L, 200L, List.of(semester()), List.of(classSubject())));
    }

    private static Semester semester() {
        Semester semester = Mockito.mock(Semester.class);
        Mockito.when(semester.getId()).thenReturn(15L);
        Mockito.when(semester.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
        Mockito.when(semester.getEndDate()).thenReturn(LocalDate.of(2026, 12, 31));
        return semester;
    }

    private static ClassSubject classSubject() {
        ClassSubject classSubject = Mockito.mock(ClassSubject.class);
        Mockito.when(classSubject.getId()).thenReturn(CLASS_SUBJECT_ID);
        Mockito.when(classSubject.getClassId()).thenReturn(CLASS_ID);
        Mockito.when(classSubject.getSemesterId()).thenReturn(15L);
        return classSubject;
    }

    private static void authenticate(String role, Long userId) {
        UserPrincipal principal = Mockito.mock(UserPrincipal.class);
        if (userId != null) {
            Mockito.when(principal.getId()).thenReturn(userId);
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}

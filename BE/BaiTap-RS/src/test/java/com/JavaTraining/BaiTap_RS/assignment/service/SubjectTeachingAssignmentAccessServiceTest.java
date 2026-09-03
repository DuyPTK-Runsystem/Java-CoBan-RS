package com.JavaTraining.BaiTap_RS.assignment.service;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubjectTeachingAssignmentAccessServiceTest {

    private static final String TEACHER_ROLE = "TEACHER";

    @Mock
    private SubjectTeachingAssignmentRepository assignmentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private HomeroomAssignmentRepository homeroomAssignmentRepository;

    private SubjectTeachingAssignmentAccessService service;

    @BeforeEach
    void setUp() {
        service = new SubjectTeachingAssignmentAccessService(
                assignmentRepository, teacherRepository, homeroomAssignmentRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teacherCanViewOwnAssignments() {
        authenticate(100L, TEACHER_ROLE);
        Teacher teacher = teacher(200L, 100L);
        Mockito.when(teacherRepository.findByUserId(100L)).thenReturn(Optional.of(teacher));

        Assertions.assertDoesNotThrow(
                () -> service.assertCanViewAssignments(200L),
                "teacher should view own assignments");
    }

    @Test
    void teacherCannotViewAnotherTeachersAssignments() {
        authenticate(100L, TEACHER_ROLE);
        Mockito.when(teacherRepository.findByUserId(100L)).thenReturn(Optional.of(teacher(200L, 100L)));

        Assertions.assertThrows(
                AppException.class,
                () -> service.assertCanViewAssignments(201L),
                "teacher should not view another teacher's assignments");
    }

    @Test
    void officeCanViewAnyTeachersAssignments() {
        authenticate(10L, "ACADEMIC_OFFICE");

        Assertions.assertDoesNotThrow(
                () -> service.assertCanViewAssignments(201L),
                "office should view any teacher assignments");
    }

    @Test
    void teacherCanViewAssignedClass() {
        authenticate(100L, TEACHER_ROLE);
        Mockito.when(teacherRepository.findByUserId(100L)).thenReturn(Optional.of(teacher(200L, 100L)));
        Mockito.when(homeroomAssignmentRepository.existsByClassIdAndTeacherIdAndStatus(
                300L, 200L, AssignmentStatus.ACTIVE)).thenReturn(true);

        Assertions.assertDoesNotThrow(
                () -> service.assertCanViewClass(300L),
                "teacher should view an assigned class");
    }

    @Test
    void teacherCannotViewUnassignedClass() {
        authenticate(100L, TEACHER_ROLE);
        Mockito.when(teacherRepository.findByUserId(100L)).thenReturn(Optional.of(teacher(200L, 100L)));

        Assertions.assertThrows(
                AppException.class,
                () -> service.assertCanViewClass(301L),
                "teacher should not view an unassigned class");
    }

    private void authenticate(Long userId, String role) {
        User user = new User("user-" + userId, "password");
        ReflectionTestUtils.setField(user, "id", userId);
        com.JavaTraining.BaiTap_RS.user.domain.entity.Role roleEntity =
                new com.JavaTraining.BaiTap_RS.user.domain.entity.Role(role, role, role);
        user.getRoles().add(roleEntity);
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Teacher teacher(Long teacherId, Long userId) {
        Teacher teacher = new Teacher(
                userId, "GV" + teacherId, "Teacher", null, null, null, null, null, null, TeacherStatus.ACTIVE);
        ReflectionTestUtils.setField(teacher, "id", teacherId);
        return teacher;
    }
}

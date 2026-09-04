package com.JavaTraining.BaiTap_RS.teacher.service;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.service.AcademicCatalogAuditService;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.requests.ReqCreateTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.response.ResTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.RoleRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    private static final Long USER_ID = 10L;
    private static final String TEACHER_CODE = "T-001";
    private static final String ROLE_TEACHER = "TEACHER";

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private HomeroomAssignmentRepository homeroomAssignmentRepository;

    @Mock
    private SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;

    @Mock
    private AcademicCatalogAuditService auditService;

    private TeacherService teacherService;

    @BeforeEach
    void setUp() {
        teacherService = new TeacherService(
                teacherRepository,
                userRepository,
                roleRepository,
                homeroomAssignmentRepository,
                subjectTeachingAssignmentRepository,
                auditService);
    }

    @Test
    void createTeacherAssignsTeacherRoleToLinkedUser() {
        User user = user();
        Role teacherRole = new Role(ROLE_TEACHER, "Teacher", "Giáo viên");
        prepareCreate(user, teacherRole);

        ResTeacherDTO response = teacherService.createTeacher(request());

        Assertions.assertTrue(
                USER_ID.equals(response.userId()) && user.getRoles().contains(teacherRole),
                "user ID and TEACHER role should be assigned");
    }

    @Test
    void createTeacherDoesNotDuplicateExistingTeacherRole() {
        User user = user();
        Role existingTeacherRole = new Role(ROLE_TEACHER, "Teacher", "Giáo viên");
        user.addRole(existingTeacherRole);
        Role anotherTeacherRoleInstance = new Role(ROLE_TEACHER, "Teacher", "Giáo viên");
        prepareCreate(user, anotherTeacherRoleInstance);

        teacherService.createTeacher(request());

        Assertions.assertTrue(
                user.getRoles().size() == 1 && user.getRoles().contains(existingTeacherRole),
                "existing role instance preserved and not duplicated");
    }

    @Test
    void createTeacherWithoutLinkedUserDoesNotLookupOrAssignRole() {
        ReqCreateTeacherDTO request = new ReqCreateTeacherDTO(
                null,
                TEACHER_CODE,
                "Nguyen Van A",
                null,
                null,
                null,
                null,
                null,
                null,
                TeacherStatus.ACTIVE);
        Mockito.when(teacherRepository.existsByTeacherCode(TEACHER_CODE)).thenReturn(false);
        Mockito.when(teacherRepository.save(Mockito.any(Teacher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResTeacherDTO response = teacherService.createTeacher(request);

        Assertions.assertNull(response.userId(), "user ID should be null for unlinked teacher");
    }

    @Test
    void createTeacherRejectsMissingTeacherRole() {
        User user = user();
        Mockito.when(userRepository.existsById(USER_ID)).thenReturn(true);
        Mockito.when(teacherRepository.existsByUserId(USER_ID)).thenReturn(false);
        Mockito.when(teacherRepository.existsByTeacherCode(TEACHER_CODE)).thenReturn(false);
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(roleRepository.findByCode(ROLE_TEACHER)).thenReturn(Optional.empty());

        assertInternalServerError(() -> teacherService.createTeacher(request()));
    }

    private void prepareCreate(User user, Role teacherRole) {
        Mockito.when(userRepository.existsById(USER_ID)).thenReturn(true);
        Mockito.when(teacherRepository.existsByUserId(USER_ID)).thenReturn(false);
        Mockito.when(teacherRepository.existsByTeacherCode(TEACHER_CODE)).thenReturn(false);
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(roleRepository.findByCode(ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        Mockito.when(teacherRepository.save(Mockito.any(Teacher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ReqCreateTeacherDTO request() {
        return new ReqCreateTeacherDTO(
                USER_ID,
                TEACHER_CODE,
                "Nguyen Van A",
                null,
                null,
                null,
                null,
                null,
                null,
                TeacherStatus.ACTIVE);
    }

    private User user() {
        User user = new User("teacher01", "hashed");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private void assertInternalServerError(Runnable action) {
        AppException exception = Assertions.assertThrows(
                AppException.class,
                action::run,
                "expected AppException");
        Assertions.assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatus(),
                "missing system role should trigger 500");
    }
}

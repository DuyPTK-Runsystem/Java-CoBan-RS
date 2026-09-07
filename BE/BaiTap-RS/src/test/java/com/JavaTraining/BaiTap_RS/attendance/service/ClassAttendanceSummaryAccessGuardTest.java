package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.calendar.service.CalendarValidityService;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClassAttendanceSummaryAccessGuardTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private HomeroomAssignmentRepository homeroomAssignmentRepository;

    @Mock
    private AttendanceEnrollmentRepository enrollmentRepository;

    @Mock
    private CalendarValidityService calendarValidityService;

    private AttendanceGuard guard;

    @BeforeEach
    void setUp() {
        guard = new AttendanceGuard(
                schoolClassRepository,
                semesterRepository,
                teacherRepository,
                homeroomAssignmentRepository,
                enrollmentRepository,
                calendarValidityService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACADEMIC_OFFICE", "ADMIN"})
    void officeRolesCanViewAnyClassSummaryWithoutTeacherProfile(String roleCode) {
        authenticate(100L, roleCode);

        guard.validateCurrentUserCanViewClassSummary(
                20L,
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 30));

        Mockito.verifyNoInteractions(teacherRepository, homeroomAssignmentRepository);
    }

    @Test
    void teacherStillRequiresHomeroomAssignmentForClassSummary() {
        authenticate(100L, "TEACHER");
        Mockito.when(teacherRepository.findByUserId(100L)).thenReturn(Optional.of(teacher()));
        Mockito.when(homeroomAssignmentRepository.existsActiveHomeroomBetween(
                20L,
                30L,
                AssignmentStatus.ACTIVE,
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 30))).thenReturn(false);

        AppException exception = captureAppException(() -> guard.validateCurrentUserCanViewClassSummary(
                20L,
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 30)));

        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception.getStatus(),
                "teacher must remain within the assigned homeroom scope");
    }

    private void authenticate(Long userId, String roleCode) {
        User user = new User("user" + userId, "password");
        ReflectionTestUtils.setField(user, "id", userId);
        user.addRole(new Role(roleCode, roleCode, roleCode));
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private AppException captureAppException(Runnable action) {
        try {
            action.run();
            return new AppException(HttpStatus.OK, "unexpected success");
        } catch (AppException exception) {
            return exception;
        }
    }

    private Teacher teacher() {
        Teacher teacher = new Teacher(
                100L,
                "T001",
                "Nguyen Van A",
                null,
                null,
                null,
                null,
                null,
                null,
                TeacherStatus.ACTIVE);
        ReflectionTestUtils.setField(teacher, "id", 30L);
        return teacher;
    }
}

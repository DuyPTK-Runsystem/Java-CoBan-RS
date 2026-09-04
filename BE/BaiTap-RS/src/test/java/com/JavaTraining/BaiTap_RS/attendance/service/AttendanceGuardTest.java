package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.calendar.service.CalendarValidityService;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AttendanceGuardTest {

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

    @Test
    void validateClassSemesterAndDateRejectsDateOutsideSemester() {
        AppException exception = captureAppException(() -> guard
                .validateClassSemesterAndDate(
                        schoolClass(), semester(), LocalDate.of(2027, 1, 1),
                        com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod.MORNING));

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus(), "invalid date should be conflict");
    }

    @Test
    void validateClassSemesterAndDateForOfficeEnsuresCalendar() {
        validateForOffice();

        Mockito.verify(calendarValidityService).ensureScheduled(
                70L,
                LocalDate.of(2026, 9, 5),
                com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod.MORNING);
    }

    private void validateForOffice() {
        guard.validateClassSemesterAndDateForOffice(
                schoolClass(), semester(), LocalDate.of(2026, 9, 5),
                com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod.MORNING);
    }

    @Test
    void assertCurrentUserHomeroomRejectsTeacherWithoutAssignment() {
        authenticate(100L);
        Mockito.when(teacherRepository.findByUserId(100L)).thenReturn(Optional.of(teacher()));
        Mockito.when(homeroomAssignmentRepository.existsActiveHomeroomBetween(
                20L,
                30L,
                com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus.ACTIVE,
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 5))).thenReturn(false);

        AppException exception = captureAppException(() -> guard
                .assertCurrentUserHomeroom(20L, LocalDate.of(2026, 9, 5)));

        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception.getStatus(), "GVCN assignment is required");
    }

    private AppException captureAppException(Runnable action) {
        try {
            action.run();
            return new AppException(HttpStatus.OK, "unexpected success");
        } catch (AppException exception) {
            return exception;
        }
    }

    private void authenticate(Long userId) {
        User user = new User("teacher", "password");
        ReflectionTestUtils.setField(user, "id", userId);
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private SchoolClass schoolClass() {
        SchoolClass schoolClass = new SchoolClass(
                10L,
                6L,
                "6A",
                "6A",
                40,
                com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus.ACTIVE);
        ReflectionTestUtils.setField(schoolClass, "id", 20L);
        return schoolClass;
    }

    private Semester semester() {
        Semester semester = new Semester(
                10L,
                "HK1",
                "Học kỳ 1",
                1,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                null,
                com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus.ACTIVE);
        ReflectionTestUtils.setField(semester, "id", 70L);
        return semester;
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
                com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus.ACTIVE);
        ReflectionTestUtils.setField(teacher, "id", 30L);
        return teacher;
    }
}

package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqAttendanceHistoryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResStudentAttendanceHistoryDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
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
class AttendanceHistoryServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AttendanceHistoryItemCollector itemCollector;

    private AttendanceHistoryService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceHistoryService(
                studentRepository,
                itemCollector,
                new AttendanceHistoryResponseMapper());
        User user = new User("student", "password");
        ReflectionTestUtils.setField(user, "id", 100L);
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnSortedItemsAndSummary() {
        Student student = new Student("Student", "S001");
        ReflectionTestUtils.setField(student, "id", 1L);
        Mockito.when(studentRepository.findByUserId(100L)).thenReturn(Optional.of(student));

        ReqAttendanceHistoryQuery query = new ReqAttendanceHistoryQuery(
                null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), 0, 10);
        ResStudentAttendanceHistoryDTO.Item item = new ResStudentAttendanceHistoryDTO.Item(
                LocalDate.of(2026, 9, 1), AttendanceSessionPeriod.MORNING, 20L, "6A", "PRESENT", null, null, null);
        Mockito.when(itemCollector.collectItems(1L, query)).thenReturn(List.of(item));

        ResStudentAttendanceHistoryDTO response = service.getHistory(query);

        Assertions.assertEquals("1:PRESENT:1", response.totalElements() + ":" + response.items().get(0).status()
                + ":" + response.summary().validSessionCount(), "history response should contain item and summary");
    }

    @Test
    void shouldReturnEmptyResponseWhenNoEnrollments() {
        Student student = new Student("Student", "S001");
        ReflectionTestUtils.setField(student, "id", 1L);
        Mockito.when(studentRepository.findByUserId(100L)).thenReturn(Optional.of(student));

        ReqAttendanceHistoryQuery query = new ReqAttendanceHistoryQuery(
                null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), 0, 10);
        Mockito.when(itemCollector.collectItems(1L, query)).thenReturn(List.of());

        ResStudentAttendanceHistoryDTO response = service.getHistory(query);

        Assertions.assertEquals(0, response.totalElements(), "totalElements should be 0");
    }

    @Test
    void shouldRejectInvalidDateRange() {
        ReqAttendanceHistoryQuery query = new ReqAttendanceHistoryQuery(
                null, null, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1), 0, 10);
        Assertions.assertThrows(AppException.class, () -> service.getHistory(query));
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFound() {
        Mockito.when(studentRepository.findByUserId(100L)).thenReturn(Optional.empty());
        ReqAttendanceHistoryQuery query = new ReqAttendanceHistoryQuery(
                null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), 0, 10);
        Assertions.assertThrows(AppException.class, () -> service.getHistory(query));
    }
}

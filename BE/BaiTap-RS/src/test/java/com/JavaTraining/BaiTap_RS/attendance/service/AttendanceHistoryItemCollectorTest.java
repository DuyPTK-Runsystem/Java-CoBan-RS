package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqAttendanceHistoryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResStudentAttendanceHistoryDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceExceptionStatus;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDayType;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AttendanceHistoryItemCollectorTest {

        @Mock
        private StudentYearEnrollmentRepository enrollmentRepository;
        @Mock
        private AttendanceHistoryCalendarReader calendarReader;
        @Mock
        private AttendanceHistorySessionReader sessionReader;

        private AttendanceHistoryItemCollector itemCollector;

        @BeforeEach
        void setUp() {
                itemCollector = new AttendanceHistoryItemCollector(
                                enrollmentRepository,
                                calendarReader,
                                sessionReader,
                                new AttendanceHistoryResponseMapper());
        }

        @Test
        void shouldReturnPresentWhenScheduledSessionHasNoRecord() {
                StudentYearEnrollment enrollment = new StudentYearEnrollment(
                                1L, 10L, 20L, EnrollmentStatus.ACTIVE, LocalDateTime.of(2026, 9, 1, 0, 0));
                ReflectionTestUtils.setField(enrollment, "id", 40L);

                SchoolClass schoolClass = new SchoolClass(10L, 6L, "6A", "6A", 40, SchoolClassStatus.ACTIVE);
                ReflectionTestUtils.setField(schoolClass, "id", 20L);

                CalendarDay day = new CalendarDay(10L, 70L, LocalDate.of(2026, 9, 1), CalendarDayType.SCHOOL_DAY, null,
                                9L);
                ReflectionTestUtils.setField(day, "id", 30L);

                CalendarSession session = new CalendarSession(
                                30L, CalendarSessionPeriod.MORNING, CalendarSessionStatus.SCHEDULED, null, 9L);
                ReflectionTestUtils.setField(session, "id", 31L);

                Mockito.when(enrollmentRepository.findByStudentIdOrderByEnrolledAtAsc(1L))
                                .thenReturn(List.of(enrollment));
                Mockito.when(calendarReader.findCalendarDays(null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1)))
                                .thenReturn(List.of(day));
                Mockito.when(calendarReader.loadClasses(List.of(20L))).thenReturn(Map.of(20L, schoolClass));
                Mockito.when(calendarReader.findCalendarSessions(30L)).thenReturn(List.of(session));
                Mockito.when(sessionReader.findSessionsMap(20L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1)))
                                .thenReturn(Map.of());

                List<ResStudentAttendanceHistoryDTO.Item> items = itemCollector.collectItems(1L, query());

                Assertions.assertEquals("1:PRESENT:6A", items.size() + ":" + items.get(0).status()
                                + ":" + items.get(0).className(),
                                "scheduled session without record should be PRESENT in 6A");
        }

        @Test
        void shouldReturnExceptionStatusWhenRecordExists() {
                StudentYearEnrollment enrollment = new StudentYearEnrollment(
                                1L, 10L, 20L, EnrollmentStatus.ACTIVE, LocalDateTime.of(2026, 9, 1, 0, 0));
                ReflectionTestUtils.setField(enrollment, "id", 40L);

                SchoolClass schoolClass = new SchoolClass(10L, 6L, "6A", "6A", 40, SchoolClassStatus.ACTIVE);
                ReflectionTestUtils.setField(schoolClass, "id", 20L);

                CalendarDay day = new CalendarDay(10L, 70L, LocalDate.of(2026, 9, 1), CalendarDayType.SCHOOL_DAY, null,
                                9L);
                ReflectionTestUtils.setField(day, "id", 30L);

                CalendarSession calendarSession = new CalendarSession(
                                30L, CalendarSessionPeriod.MORNING, CalendarSessionStatus.SCHEDULED, null, 9L);
                ReflectionTestUtils.setField(calendarSession, "id", 31L);

                AttendanceSession session = new AttendanceSession(
                                20L, 70L, LocalDate.of(2026, 9, 1), AttendanceSessionPeriod.MORNING, 9L);
                ReflectionTestUtils.setField(session, "id", 50L);

                AttendanceRecord record = new AttendanceRecord(
                                50L, 1L, AttendanceExceptionStatus.EXCUSED, "Nghỉ ốm", 9L);
                ReflectionTestUtils.setField(record, "id", 60L);

                Mockito.when(enrollmentRepository.findByStudentIdOrderByEnrolledAtAsc(1L))
                                .thenReturn(List.of(enrollment));
                Mockito.when(calendarReader.findCalendarDays(null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1)))
                                .thenReturn(List.of(day));
                Mockito.when(calendarReader.loadClasses(List.of(20L))).thenReturn(Map.of(20L, schoolClass));
                Mockito.when(calendarReader.findCalendarSessions(30L)).thenReturn(List.of(calendarSession));
                Mockito.when(sessionReader.findSessionsMap(20L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1)))
                                .thenReturn(Map.of("2026-09-01:MORNING", session));
                Mockito.when(sessionReader.findRecord(50L, 1L)).thenReturn(Optional.of(record));

                List<ResStudentAttendanceHistoryDTO.Item> items = itemCollector.collectItems(1L, query());

                Assertions.assertEquals("1:EXCUSED_ABSENCE:Nghỉ ốm", items.size() + ":" + items.get(0).status()
                                + ":" + items.get(0).note(),
                                "session with record should have EXCUSED_ABSENCE status and note");
        }

        @Test
        void shouldReturnEmptyListWhenEnrollmentsEmpty() {
                Mockito.when(enrollmentRepository.findByStudentIdOrderByEnrolledAtAsc(1L)).thenReturn(List.of());
                List<ResStudentAttendanceHistoryDTO.Item> items = itemCollector.collectItems(1L, query());
                Assertions.assertTrue(items.isEmpty(), "items should be empty");
        }

        private ReqAttendanceHistoryQuery query() {
                return new ReqAttendanceHistoryQuery(
                                null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), 0, 10);
        }
}

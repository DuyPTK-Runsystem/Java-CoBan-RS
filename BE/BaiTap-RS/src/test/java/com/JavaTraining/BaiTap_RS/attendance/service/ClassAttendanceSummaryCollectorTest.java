package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceExceptionStatus;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClassAttendanceSummaryCollectorTest {

        @Mock
        private AttendanceEnrollmentRepository attendanceEnrollmentRepository;
        @Mock
        private ClassAttendanceSummaryCalendarReader calendarReader;
        @Mock
        private ClassAttendanceSummarySessionReader sessionReader;

        private ClassAttendanceSummaryCollector collector;

        @BeforeEach
        void setUp() {
                collector = new ClassAttendanceSummaryCollector(
                                attendanceEnrollmentRepository,
                                calendarReader,
                                sessionReader);
        }

        @Test
        void shouldCollectAndAggregateClassSummaryWithInferredPresentAndExceptions() {
                Long classId = 10L;
                Long semesterId = 1L;
                LocalDate from = LocalDate.of(2026, 9, 1);
                LocalDate to = LocalDate.of(2026, 9, 2);

                List<ClassAttendanceSummaryCollector.SessionSlot> slots = List.of(
                                new ClassAttendanceSummaryCollector.SessionSlot(from, AttendanceSessionPeriod.MORNING),
                                new ClassAttendanceSummaryCollector.SessionSlot(from,
                                                AttendanceSessionPeriod.AFTERNOON),
                                new ClassAttendanceSummaryCollector.SessionSlot(to, AttendanceSessionPeriod.MORNING));
                Mockito.when(calendarReader.collectScheduledSlots(semesterId, from, to)).thenReturn(slots);

                Student s1 = createStudent(1L, "S001", "Student One");
                Student s2 = createStudent(2L, "S002", "Student Two");
                Mockito.when(attendanceEnrollmentRepository.findActiveStudentsInClassAt(
                                Mockito.eq(classId), Mockito.eq(EnrollmentStatus.ACTIVE), Mockito.any(), Mockito.any()))
                                .thenReturn(List.of(s1, s2));

                StudentYearEnrollment e1 = createEnrollment(1L, 1L, from.atStartOfDay(), null);
                StudentYearEnrollment e2 = createEnrollment(2L, 2L, from.atStartOfDay(), null);
                Mockito.when(attendanceEnrollmentRepository.findActiveEnrollmentsInClassAt(
                                Mockito.eq(classId), Mockito.eq(EnrollmentStatus.ACTIVE), Mockito.any(), Mockito.any()))
                                .thenReturn(List.of(e1, e2));

                Mockito.when(sessionReader.findExceptionMap(classId, from, to)).thenReturn(Map.of(
                                from + ":MORNING:1", AttendanceExceptionStatus.EXCUSED,
                                from + ":AFTERNOON:2", AttendanceExceptionStatus.LATE));

                ClassAttendanceSummaryCollector.AggregatedClassData data = collector.collect(classId, semesterId, from,
                                to);

                String actualSignature = data.validSessionCount() + ":"
                                + data.studentSummaries().size() + ":"
                                + data.studentSummaries().get(0).presentCount() + ":"
                                + data.studentSummaries().get(0).excusedAbsenceCount() + ":"
                                + data.studentSummaries().get(1).lateCount() + ":"
                                + data.classSummary().presentCount();
                Assertions.assertEquals("3:2:2:1:1:4", actualSignature,
                                "summary aggregation should infer present and count exceptions");
        }

        @Test
        void shouldRespectEnrollmentBoundary() {
                Long classId = 10L;
                Long semesterId = 1L;
                LocalDate d1 = LocalDate.of(2026, 9, 1);
                LocalDate d2 = LocalDate.of(2026, 9, 2);
                LocalDate d3 = LocalDate.of(2026, 9, 3);

                List<ClassAttendanceSummaryCollector.SessionSlot> slots = List.of(
                                new ClassAttendanceSummaryCollector.SessionSlot(d1, AttendanceSessionPeriod.MORNING),
                                new ClassAttendanceSummaryCollector.SessionSlot(d2, AttendanceSessionPeriod.MORNING),
                                new ClassAttendanceSummaryCollector.SessionSlot(d3, AttendanceSessionPeriod.MORNING));
                Mockito.when(calendarReader.collectScheduledSlots(semesterId, d1, d3)).thenReturn(slots);

                Student s1 = createStudent(1L, "S001", "Student One");
                Mockito.when(attendanceEnrollmentRepository.findActiveStudentsInClassAt(
                                Mockito.eq(classId), Mockito.eq(EnrollmentStatus.ACTIVE), Mockito.any(), Mockito.any()))
                                .thenReturn(List.of(s1));

                StudentYearEnrollment e1 = createEnrollment(1L, 1L, d2.atStartOfDay(), null);
                Mockito.when(attendanceEnrollmentRepository.findActiveEnrollmentsInClassAt(
                                Mockito.eq(classId), Mockito.eq(EnrollmentStatus.ACTIVE), Mockito.any(), Mockito.any()))
                                .thenReturn(List.of(e1));

                Mockito.when(sessionReader.findExceptionMap(classId, d1, d3)).thenReturn(Collections.emptyMap());

                ClassAttendanceSummaryCollector.AggregatedClassData data = collector.collect(classId, semesterId, d1,
                                d3);

                String signature = data.validSessionCount() + ":"
                                + data.studentSummaries().get(0).validSessionCount() + ":"
                                + data.studentSummaries().get(0).presentCount() + ":"
                                + data.studentSummaries().get(0).attendanceRate();
                Assertions.assertEquals("3:2:2:1.0", signature, "enrollment boundary should filter valid sessions");
        }

        @Test
        void shouldReturnEmptyWhenNoStudents() {
                Long classId = 10L;
                Long semesterId = 1L;
                LocalDate from = LocalDate.of(2026, 9, 1);
                LocalDate to = LocalDate.of(2026, 9, 2);

                List<ClassAttendanceSummaryCollector.SessionSlot> slots = List.of(
                                new ClassAttendanceSummaryCollector.SessionSlot(from, AttendanceSessionPeriod.MORNING));
                Mockito.when(calendarReader.collectScheduledSlots(semesterId, from, to)).thenReturn(slots);

                Mockito.when(attendanceEnrollmentRepository.findActiveStudentsInClassAt(
                                Mockito.eq(classId), Mockito.eq(EnrollmentStatus.ACTIVE), Mockito.any(), Mockito.any()))
                                .thenReturn(Collections.emptyList());

                ClassAttendanceSummaryCollector.AggregatedClassData data = collector.collect(classId, semesterId, from,
                                to);

                String signature = data.validSessionCount() + ":"
                                + data.studentSummaries().size() + ":"
                                + data.classSummary().presentCount();
                Assertions.assertEquals("1:0:0", signature, "empty students should return zero counts");
        }

        private Student createStudent(Long id, String code, String name) {
                Student student = new Student(name, code);
                ReflectionTestUtils.setField(student, "id", id);
                return student;
        }

        private StudentYearEnrollment createEnrollment(
                        Long id, Long studentId, LocalDateTime start, LocalDateTime end) {
                StudentYearEnrollment enrollment = new StudentYearEnrollment(studentId, 1L, 10L,
                                EnrollmentStatus.ACTIVE, start);
                enrollment.setCompletedAt(end);
                ReflectionTestUtils.setField(enrollment, "id", id);
                return enrollment;
        }
}

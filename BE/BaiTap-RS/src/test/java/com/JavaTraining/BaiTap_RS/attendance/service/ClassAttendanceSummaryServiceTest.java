package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqClassAttendanceSummaryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResClassAttendanceSummaryDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
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
class ClassAttendanceSummaryServiceTest {

    @Mock
    private AttendanceGuard attendanceGuard;
    @Mock
    private ClassAttendanceSummaryCollector collector;

    private ClassAttendanceSummaryService service;

    @BeforeEach
    void setUp() {
        service = new ClassAttendanceSummaryService(
                attendanceGuard,
                collector,
                new ClassAttendanceSummaryResponseMapper());
    }

    @Test
    void shouldReturnClassSummarySuccessfully() {
        Long classId = 10L;
        Long semesterId = 1L;
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        ReqClassAttendanceSummaryQuery query = new ReqClassAttendanceSummaryQuery(semesterId, from, to, 0, 20);

        SchoolClass schoolClass = new SchoolClass(1L, 6L, "6A", "6A", 40, SchoolClassStatus.ACTIVE);
        ReflectionTestUtils.setField(schoolClass, "id", classId);
        Semester semester = new Semester(1L, "HK1", "Học kỳ 1", 1, from, to, null, SemesterStatus.ACTIVE);
        ReflectionTestUtils.setField(semester, "id", semesterId);

        Mockito.when(attendanceGuard.findSchoolClass(classId)).thenReturn(schoolClass);
        Mockito.when(attendanceGuard.findSemester(semesterId)).thenReturn(semester);

        ResClassAttendanceSummaryDTO.Summary classSummary = new ResClassAttendanceSummaryDTO.Summary(
                18, 1, 0, 1, 0);
        ResClassAttendanceSummaryDTO.StudentSummary student = new ResClassAttendanceSummaryDTO.StudentSummary(
                100L, "STU001", "Student One", 20, 18, 1, 0, 1, 0, 0.9);
        ClassAttendanceSummaryCollector.AggregatedClassData data =
                new ClassAttendanceSummaryCollector.AggregatedClassData(20, classSummary, List.of(student));

        Mockito.when(collector.collect(classId, semesterId, from, to)).thenReturn(data);

        ResClassAttendanceSummaryDTO response = service.getClassSummary(classId, query);

        String signature = response.classInfo().id() + ":"
                + response.classInfo().name() + ":"
                + response.validSessionCount() + ":"
                + response.totalElements() + ":"
                + response.totalPages() + ":"
                + response.students().size() + ":"
                + response.students().get(0).studentCode() + ":"
                + response.students().get(0).attendanceRate();
        Assertions.assertEquals("10:6A:20:1:1:1:STU001:0.9", signature,
                "summary response should match expected fields");
    }

    @Test
    void shouldRejectInvalidDateRange() {
        Long classId = 10L;
        ReqClassAttendanceSummaryQuery query = new ReqClassAttendanceSummaryQuery(
                1L, LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1), 0, 20);

        Assertions.assertThrows(
                AppException.class,
                () -> service.getClassSummary(classId, query),
                "invalid date range should throw AppException");
    }

    @Test
    void shouldPropagateClassNotFound() {
        Long classId = 999L;
        ReqClassAttendanceSummaryQuery query = new ReqClassAttendanceSummaryQuery(
                1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 0, 20);

        Mockito.when(attendanceGuard.findSchoolClass(classId))
                .thenThrow(new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp"));

        Assertions.assertThrows(
                AppException.class,
                () -> service.getClassSummary(classId, query),
                "missing class should throw AppException");
    }

    @Test
    void shouldPropagateSemesterConflict() {
        Long classId = 10L;
        Long semesterId = 1L;
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        ReqClassAttendanceSummaryQuery query = new ReqClassAttendanceSummaryQuery(semesterId, from, to, 0, 20);

        SchoolClass schoolClass = new SchoolClass(1L, 6L, "6A", "6A", 40, SchoolClassStatus.ACTIVE);
        Semester semester = new Semester(2L, "HK1", "Học kỳ 1", 1, from, to, null, SemesterStatus.ACTIVE);

        Mockito.when(attendanceGuard.findSchoolClass(classId)).thenReturn(schoolClass);
        Mockito.when(attendanceGuard.findSemester(semesterId)).thenReturn(semester);
        Mockito.doThrow(new AppException(HttpStatus.CONFLICT, "Lớp và học kỳ phải thuộc cùng năm học"))
                .when(attendanceGuard).validateClassAndSemester(schoolClass, semester);

        Assertions.assertThrows(
                AppException.class,
                () -> service.getClassSummary(classId, query),
                "conflict semester should throw AppException");
    }

    @Test
    void shouldPropagateForbiddenWhenTeacherNotAssigned() {
        Long classId = 10L;
        Long semesterId = 1L;
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        ReqClassAttendanceSummaryQuery query = new ReqClassAttendanceSummaryQuery(semesterId, from, to, 0, 20);

        SchoolClass schoolClass = new SchoolClass(1L, 6L, "6A", "6A", 40, SchoolClassStatus.ACTIVE);
        Semester semester = new Semester(1L, "HK1", "Học kỳ 1", 1, from, to, null, SemesterStatus.ACTIVE);

        Mockito.when(attendanceGuard.findSchoolClass(classId)).thenReturn(schoolClass);
        Mockito.when(attendanceGuard.findSemester(semesterId)).thenReturn(semester);
        Mockito.doThrow(new AppException(HttpStatus.FORBIDDEN, "GVCN chỉ được thao tác lớp được phân công"))
                .when(attendanceGuard).validateCurrentUserHomeroomInRange(classId, from, to);

        Assertions.assertThrows(
                AppException.class,
                () -> service.getClassSummary(classId, query),
                "unassigned teacher should throw AppException");
    }
}

package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqCreateAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqUpsertAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceStudentDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceExceptionStatus;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceRecordRepository;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceSessionRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.UnitTestContainsTooManyAsserts",
        "PMD.AvoidDuplicateLiterals"
})
class AttendanceServiceTest {

    private static final String ATTENDANCE_RECORD_ID = "attendanceRecordId";

    @Mock
    private AttendanceSessionRepository sessionRepository;

    @Mock
    private AttendanceRecordRepository recordRepository;

    @Mock
    private AttendanceGuard guard;

    @Mock
    private AttendanceAuditService auditService;
    @Mock
    private StudentLookupService studentLookupService;

    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(
                sessionRepository,
                recordRepository,
                guard,
                auditService,
                new AttendanceMapper(),
                studentLookupService);
    }

    @Test
    void createOrGetSessionReturnsExistingSession() {
        SchoolClass schoolClass = Mockito.mock(SchoolClass.class);
        Semester semester = Mockito.mock(Semester.class);
        AttendanceSession session = attendanceSession();
        Mockito.when(guard.findSchoolClass(20L)).thenReturn(schoolClass);
        Mockito.when(guard.findSemester(70L)).thenReturn(semester);
        Mockito.when(schoolClass.getId()).thenReturn(20L);
        Mockito.when(sessionRepository.findByClassIdAndAttendanceDateAndSessionPeriod(
                20L,
                LocalDate.of(2026, 9, 5),
                AttendanceSessionPeriod.MORNING)).thenReturn(Optional.of(session));

        ResAttendanceSessionDTO response = attendanceService.createOrGetSession(new ReqCreateAttendanceSessionDTO(
                20L,
                70L,
                LocalDate.of(2026, 9, 5),
                AttendanceSessionPeriod.MORNING));

        Assertions.assertEquals(80L, response.sessionId(), "existing session should be returned");
    }

    @Test
    void listSessionStudentsReturnsPresentAndExceptionStatuses() {
        AttendanceSession session = attendanceSession();
        Student presentStudent = student(1L, "STU0000001", "Nguyen A");
        Student lateStudent = student(2L, "STU0000002", "Nguyen B");
        Mockito.when(sessionRepository.findById(80L)).thenReturn(Optional.of(session));
        Mockito.when(guard.findActiveClassStudents(20L, LocalDate.of(2026, 9, 5)))
                .thenReturn(List.of(presentStudent, lateStudent));
        Mockito.when(recordRepository.findAllBySessionIdAndStudentIdIn(80L, List.of(1L, 2L)))
                .thenReturn(List.of(attendanceRecord(91L, 2L, AttendanceExceptionStatus.LATE)));

        List<ResAttendanceStudentDTO> students = attendanceService.listSessionStudents(80L);

        Assertions.assertEquals(
                "PRESENT,LATE",
                students.get(0).status() + "," + students.get(1).status(),
                "student list should combine inferred PRESENT and stored exception");
    }

    @Test
    void upsertExceptionUpdatesExistingRecordAndWritesAudit() {
        AttendanceSession session = attendanceSession();
        AttendanceRecord existing = attendanceRecord(91L, 2L, AttendanceExceptionStatus.ABSENT);
        Student student = student(2L, "STU0000002", "Nguyen B");
        Map<String, Object> beforeData = Map.of(ATTENDANCE_RECORD_ID, 91L);
        Mockito.when(sessionRepository.findById(80L)).thenReturn(Optional.of(session));
        Mockito.when(studentLookupService.resolveStudent(2L, null)).thenReturn(student);
        Mockito.when(recordRepository.findBySessionIdAndStudentId(80L, 2L)).thenReturn(Optional.of(existing));
        Mockito.when(auditService.recordData(session, existing)).thenReturn(beforeData);

        ResAttendanceExceptionDTO response = attendanceService.upsertException(
                80L,
                2L,
                new ReqUpsertAttendanceExceptionDTO(AttendanceExceptionStatus.EARLY_LEAVE, "Xin về sớm"));

        Assertions.assertEquals("STU0000002", response.studentCode(), "studentCode should be returned");
        Assertions.assertEquals("Nguyen B", response.studentName(), "studentName should be returned");
        Mockito.verify(auditService)
                .writeRecordAudit("ATTENDANCE_EXCEPTION_UPDATED", session, beforeData, existing);
    }

    @Test
    void upsertExceptionByCodeResolvesStudentAndReturnsMetadata() {
        AttendanceSession session = attendanceSession();
        AttendanceRecord existing = attendanceRecord(91L, 2L, AttendanceExceptionStatus.ABSENT);
        Student student = student(2L, "STU0000002", "Nguyen B");
        Mockito.when(studentLookupService.resolveStudent(null, "STU0000002")).thenReturn(student);
        Mockito.when(studentLookupService.resolveStudent(2L, null)).thenReturn(student);
        Mockito.when(sessionRepository.findById(80L)).thenReturn(Optional.of(session));
        Mockito.when(recordRepository.findBySessionIdAndStudentId(80L, 2L)).thenReturn(Optional.of(existing));

        ResAttendanceExceptionDTO response = attendanceService.upsertExceptionByCode(
                80L,
                "STU0000002",
                new ReqUpsertAttendanceExceptionDTO(AttendanceExceptionStatus.EXCUSED, "Có phép"));

        Assertions.assertEquals("STU0000002", response.studentCode(), "student code should be returned");
        Mockito.verify(guard).assertStudentInClass(2L, 20L, LocalDate.of(2026, 9, 5));
    }

    @Test
    void deleteExceptionDeletesRecordAndWritesAudit() {
        AttendanceSession session = attendanceSession();
        AttendanceRecord existing = attendanceRecord(91L, 2L, AttendanceExceptionStatus.EXCUSED);
        Map<String, Object> beforeData = Map.of(ATTENDANCE_RECORD_ID, 91L);
        Mockito.when(sessionRepository.findById(80L)).thenReturn(Optional.of(session));
        Mockito.when(recordRepository.findBySessionIdAndStudentId(80L, 2L)).thenReturn(Optional.of(existing));
        Mockito.when(auditService.recordData(session, existing)).thenReturn(beforeData);

        attendanceService.deleteException(80L, 2L);

        Mockito.verify(recordRepository).delete(existing);
    }

    private AttendanceSession attendanceSession() {
        AttendanceSession session = new AttendanceSession(
                20L,
                70L,
                LocalDate.of(2026, 9, 5),
                AttendanceSessionPeriod.MORNING,
                100L);
        ReflectionTestUtils.setField(session, "id", 80L);
        return session;
    }

    private Student student(Long id, String code, String name) {
        Student student = new Student(name, code);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private AttendanceRecord attendanceRecord(
            Long id,
            Long studentId,
            AttendanceExceptionStatus status) {
        AttendanceRecord record = new AttendanceRecord(80L, studentId, status, null, 100L);
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }
}

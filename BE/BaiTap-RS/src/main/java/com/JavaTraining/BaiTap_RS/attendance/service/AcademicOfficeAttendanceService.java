package com.JavaTraining.BaiTap_RS.attendance.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqCreateAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqUpsertAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceStudentDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceRecordRepository;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceSessionRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicOfficeAttendanceService {

    private static final String ACTION_EXCEPTION_CREATED = "OFFICE_ATTENDANCE_EXCEPTION_CREATED";
    private static final String ACTION_EXCEPTION_UPDATED = "OFFICE_ATTENDANCE_EXCEPTION_UPDATED";
    private static final String ACTION_EXCEPTION_DELETED = "OFFICE_ATTENDANCE_EXCEPTION_DELETED";

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceGuard guard;
    private final AttendanceAuditService auditService;
    private final AttendanceMapper mapper;

    public AcademicOfficeAttendanceService(
            AttendanceSessionRepository sessionRepository,
            AttendanceRecordRepository recordRepository,
            AttendanceGuard guard,
            AttendanceAuditService auditService,
            AttendanceMapper mapper) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.guard = guard;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional
    public ResAttendanceSessionDTO createOrGetSession(ReqCreateAttendanceSessionDTO request) {
        SchoolClass schoolClass = guard.findSchoolClass(request.classId());
        Semester semester = guard.findSemester(request.semesterId());
        guard.validateClassSemesterAndDate(
                schoolClass, semester, request.attendanceDate(), request.sessionPeriod());
        AttendanceSession session = sessionRepository.findByClassIdAndAttendanceDateAndSessionPeriod(
                        request.classId(),
                        request.attendanceDate(),
                        request.sessionPeriod())
                .orElseGet(() -> sessionRepository.save(new AttendanceSession(
                        request.classId(),
                        request.semesterId(),
                        request.attendanceDate(),
                        request.sessionPeriod(),
                        AuditContext.currentUserId())));
        return mapper.toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<ResAttendanceStudentDTO> listSessionStudents(Long sessionId) {
        AttendanceSession session = findSession(sessionId);
        List<Student> students = guard.findActiveClassStudents(session.getClassId(), session.getAttendanceDate());
        Map<Long, AttendanceRecord> records = findRecordsByStudentId(session, students);
        return students.stream()
                .map(student -> mapper.toStudentResponse(student, records.get(student.getId())))
                .toList();
    }

    @Transactional
    public ResAttendanceExceptionDTO upsertException(
            Long sessionId,
            Long studentId,
            ReqUpsertAttendanceExceptionDTO request) {
        AttendanceSession session = findSession(sessionId);
        guard.assertStudentInClass(studentId, session.getClassId(), session.getAttendanceDate());
        AttendanceRecord record = recordRepository.findBySessionIdAndStudentId(sessionId, studentId)
                .map(existing -> updateRecord(session, existing, request))
                .orElseGet(() -> createRecord(session, studentId, request));
        return mapper.toExceptionResponse(record);
    }

    @Transactional
    public void deleteException(Long sessionId, Long studentId) {
        AttendanceSession session = findSession(sessionId);
        AttendanceRecord record = recordRepository.findBySessionIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> guard.notFound("Không tìm thấy ngoại lệ điểm danh"));
        Map<String, Object> beforeData = auditService.recordData(session, record);
        recordRepository.delete(record);
        auditService.writeRecordAudit(ACTION_EXCEPTION_DELETED, session, beforeData, null);
    }

    private AttendanceSession findSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> guard.notFound("Không tìm thấy buổi điểm danh"));
    }

    private AttendanceRecord createRecord(
            AttendanceSession session,
            Long studentId,
            ReqUpsertAttendanceExceptionDTO request) {
        AttendanceRecord record = recordRepository.save(new AttendanceRecord(
                session.getId(),
                studentId,
                request.status(),
                request.note(),
                AuditContext.currentUserId()));
        auditService.writeRecordAudit(ACTION_EXCEPTION_CREATED, session, null, record);
        return record;
    }

    private AttendanceRecord updateRecord(
            AttendanceSession session,
            AttendanceRecord record,
            ReqUpsertAttendanceExceptionDTO request) {
        Map<String, Object> beforeData = auditService.recordData(session, record);
        record.update(request.status(), request.note(), AuditContext.currentUserId());
        auditService.writeRecordAudit(ACTION_EXCEPTION_UPDATED, session, beforeData, record);
        return record;
    }

    private Map<Long, AttendanceRecord> findRecordsByStudentId(
            AttendanceSession session,
            List<Student> students) {
        List<Long> studentIds = students.stream()
                .map(Student::getId)
                .toList();
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return recordRepository.findAllBySessionIdAndStudentIdIn(session.getId(), studentIds)
                .stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, Function.identity()));
    }
}

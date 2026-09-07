package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceRecordRepository;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceSessionRepository;
import org.springframework.stereotype.Component;

@Component
public class AttendanceHistorySessionReader {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public AttendanceHistorySessionReader(
            AttendanceSessionRepository attendanceSessionRepository,
            AttendanceRecordRepository attendanceRecordRepository) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    public Map<String, AttendanceSession> findSessionsMap(Long classId, LocalDate from, LocalDate to) {
        List<AttendanceSession> sessions = attendanceSessionRepository
                .findAllByClassIdAndAttendanceDateBetween(classId, from, to);
        return sessions.stream().collect(Collectors.toMap(
                session -> session.getAttendanceDate() + ":" + session.getSessionPeriod(),
                session -> session,
                (first, ignored) -> first));
    }

    public Optional<AttendanceRecord> findRecord(Long sessionId, Long studentId) {
        return attendanceRecordRepository.findBySessionIdAndStudentId(sessionId, studentId);
    }
}

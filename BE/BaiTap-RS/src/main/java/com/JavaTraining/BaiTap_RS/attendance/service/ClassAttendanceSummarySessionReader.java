package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceExceptionStatus;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceRecordRepository;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceSessionRepository;
import org.springframework.stereotype.Component;

@Component
public class ClassAttendanceSummarySessionReader {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public ClassAttendanceSummarySessionReader(
            AttendanceSessionRepository attendanceSessionRepository,
            AttendanceRecordRepository attendanceRecordRepository) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    public Map<String, AttendanceExceptionStatus> findExceptionMap(Long classId, LocalDate from, LocalDate to) {
        List<AttendanceSession> sessions = attendanceSessionRepository
                .findAllByClassIdAndAttendanceDateBetween(classId, from, to);
        if (sessions.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> sessionIds = sessions.stream().map(AttendanceSession::getId).toList();
        Map<Long, AttendanceSession> sessionById = sessions.stream().collect(
                Collectors.toMap(AttendanceSession::getId, s -> s));

        List<AttendanceRecord> records = attendanceRecordRepository.findAllBySessionIdIn(sessionIds);
        Map<String, AttendanceExceptionStatus> resultMap = new HashMap<>();
        for (AttendanceRecord record : records) {
            AttendanceSession session = sessionById.get(record.getSessionId());
            if (session != null) {
                String key = session.getAttendanceDate() + ":" + session.getSessionPeriod() + ":"
                        + record.getStudentId();
                resultMap.put(key, record.getStatus());
            }
        }
        return resultMap;
    }
}

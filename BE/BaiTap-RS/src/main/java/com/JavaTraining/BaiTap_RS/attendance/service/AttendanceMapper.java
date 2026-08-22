package com.JavaTraining.BaiTap_RS.attendance.service;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceStudentDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    private static final String PRESENT = "PRESENT";

    public ResAttendanceSessionDTO toSessionResponse(AttendanceSession session) {
        return new ResAttendanceSessionDTO(
                session.getId(),
                session.getClassId(),
                session.getSemesterId(),
                session.getAttendanceDate(),
                session.getSessionPeriod(),
                session.getCreatedBy(),
                session.getCreatedAt());
    }

    public ResAttendanceExceptionDTO toExceptionResponse(AttendanceRecord record) {
        return new ResAttendanceExceptionDTO(
                record.getId(),
                record.getSessionId(),
                record.getStudentId(),
                record.getStatus(),
                record.getNote(),
                record.getRecordedBy(),
                record.getRecordedAt(),
                record.getUpdatedBy(),
                record.getUpdatedAt());
    }

    public ResAttendanceStudentDTO toStudentResponse(Student student, AttendanceRecord record) {
        if (record == null) {
            return new ResAttendanceStudentDTO(
                    student.getId(),
                    student.getStudentCode(),
                    student.getStudentName(),
                    null,
                    PRESENT,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        return new ResAttendanceStudentDTO(
                student.getId(),
                student.getStudentCode(),
                student.getStudentName(),
                record.getId(),
                record.getStatus().name(),
                record.getNote(),
                record.getRecordedBy(),
                record.getRecordedAt(),
                record.getUpdatedBy(),
                record.getUpdatedAt());
    }
}

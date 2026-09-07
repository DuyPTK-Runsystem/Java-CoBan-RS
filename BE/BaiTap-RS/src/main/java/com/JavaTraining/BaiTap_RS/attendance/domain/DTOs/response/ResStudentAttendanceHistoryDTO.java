package com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;

public record ResStudentAttendanceHistoryDTO(
        List<Item> items,
        Summary summary,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public record Item(
            LocalDate attendanceDate,
            AttendanceSessionPeriod sessionPeriod,
            Long classId,
            String className,
            String status,
            Long attendanceRecordId,
            String exceptionStatus,
            String note) {
    }

    public record Summary(
            long validSessionCount,
            long presentCount,
            long excusedAbsenceCount,
            long unexcusedAbsenceCount,
            long lateCount,
            long earlyLeaveCount) {
    }
}

package com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResClassAttendanceSummaryDTO(
        @JsonProperty("class") ClassInfo classInfo,
        Long semesterId,
        LocalDate from,
        LocalDate to,
        long validSessionCount,
        Summary summary,
        List<StudentSummary> students,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public record ClassInfo(
            Long id,
            String name,
            Long gradeLevelId) {
    }

    public record Summary(
            long presentCount,
            long excusedAbsenceCount,
            long unexcusedAbsenceCount,
            long lateCount,
            long earlyLeaveCount) {
    }

    public record StudentSummary(
            Long studentId,
            String studentCode,
            String fullName,
            long validSessionCount,
            long presentCount,
            long excusedAbsenceCount,
            long unexcusedAbsenceCount,
            long lateCount,
            long earlyLeaveCount,
            double attendanceRate) {
    }
}

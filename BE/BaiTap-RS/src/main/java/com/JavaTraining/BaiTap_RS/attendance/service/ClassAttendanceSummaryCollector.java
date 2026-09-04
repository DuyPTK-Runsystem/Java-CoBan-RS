package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResClassAttendanceSummaryDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceExceptionStatus;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class ClassAttendanceSummaryCollector {

    private final AttendanceEnrollmentRepository attendanceEnrollmentRepository;
    private final ClassAttendanceSummaryCalendarReader calendarReader;
    private final ClassAttendanceSummarySessionReader sessionReader;

    public record SessionSlot(LocalDate date, AttendanceSessionPeriod period) {
    }

    public record AggregatedClassData(
            long validSessionCount,
            ResClassAttendanceSummaryDTO.Summary classSummary,
            List<ResClassAttendanceSummaryDTO.StudentSummary> studentSummaries) {
    }

    public ClassAttendanceSummaryCollector(
            AttendanceEnrollmentRepository attendanceEnrollmentRepository,
            ClassAttendanceSummaryCalendarReader calendarReader,
            ClassAttendanceSummarySessionReader sessionReader) {
        this.attendanceEnrollmentRepository = attendanceEnrollmentRepository;
        this.calendarReader = calendarReader;
        this.sessionReader = sessionReader;
    }

    public AggregatedClassData collect(Long classId, Long semesterId, LocalDate from, LocalDate to) {
        List<SessionSlot> slots = calendarReader.collectScheduledSlots(semesterId, from, to);
        long classValidSessionCount = slots.size();

        List<Student> students = attendanceEnrollmentRepository.findActiveStudentsInClassAt(
                classId,
                EnrollmentStatus.ACTIVE,
                from.atStartOfDay(),
                to.atTime(23, 59, 59));

        if (students.isEmpty()) {
            ResClassAttendanceSummaryDTO.Summary emptySummary = new ResClassAttendanceSummaryDTO.Summary(
                    0, 0, 0, 0, 0);
            return new AggregatedClassData(classValidSessionCount, emptySummary, Collections.emptyList());
        }

        List<StudentYearEnrollment> enrollments = attendanceEnrollmentRepository.findActiveEnrollmentsInClassAt(
                classId,
                EnrollmentStatus.ACTIVE,
                from.atStartOfDay(),
                to.atTime(23, 59, 59));

        Map<Long, StudentYearEnrollment> enrollmentMap = enrollments.stream().collect(
                Collectors.toMap(StudentYearEnrollment::getStudentId, e -> e, (e1, e2) -> e1));

        Map<String, AttendanceExceptionStatus> exceptionMap = sessionReader.findExceptionMap(classId, from, to);

        List<ResClassAttendanceSummaryDTO.StudentSummary> studentSummaries = new ArrayList<>();
        for (Student student : students) {
            StudentYearEnrollment enrollment = enrollmentMap.get(student.getId());
            studentSummaries.add(calculateStudentSummary(student, enrollment, slots, exceptionMap));
        }

        ResClassAttendanceSummaryDTO.Summary classSummary = summarizeClass(studentSummaries);
        return new AggregatedClassData(classValidSessionCount, classSummary, studentSummaries);
    }

    private ResClassAttendanceSummaryDTO.StudentSummary calculateStudentSummary(
            Student student,
            StudentYearEnrollment enrollment,
            List<SessionSlot> slots,
            Map<String, AttendanceExceptionStatus> exceptionMap) {
        List<SessionSlot> studentSlots = slots.stream()
                .filter(slot -> isEnrolledOn(enrollment, slot.date()))
                .toList();

        long validSessionCount = studentSlots.size();
        long presentCount = 0;
        long excusedCount = 0;
        long unexcusedCount = 0;
        long lateCount = 0;
        long earlyLeaveCount = 0;

        for (SessionSlot slot : studentSlots) {
            String key = slot.date() + ":" + slot.period() + ":" + student.getId();
            AttendanceExceptionStatus status = exceptionMap.get(key);

            if (status == null) {
                presentCount++;
            } else if (status == AttendanceExceptionStatus.EXCUSED) {
                excusedCount++;
            } else if (status == AttendanceExceptionStatus.ABSENT) {
                unexcusedCount++;
            } else if (status == AttendanceExceptionStatus.LATE) {
                lateCount++;
            } else if (status == AttendanceExceptionStatus.EARLY_LEAVE) {
                earlyLeaveCount++;
            }
        }

        double rate = validSessionCount > 0 ? (double) presentCount / validSessionCount : 0.0;
        return new ResClassAttendanceSummaryDTO.StudentSummary(
                student.getId(),
                student.getStudentCode(),
                student.getStudentName(),
                validSessionCount,
                presentCount,
                excusedCount,
                unexcusedCount,
                lateCount,
                earlyLeaveCount,
                rate);
    }

    private boolean isEnrolledOn(StudentYearEnrollment enrollment, LocalDate date) {
        if (enrollment == null) {
            return false;
        }
        LocalDateTime start = enrollment.getEnrolledAt();
        LocalDateTime end = enrollment.getCompletedAt();
        return !date.isBefore(start.toLocalDate()) && (end == null || !date.isAfter(end.toLocalDate()));
    }

    private ResClassAttendanceSummaryDTO.Summary summarizeClass(
            List<ResClassAttendanceSummaryDTO.StudentSummary> studentSummaries) {
        long present = studentSummaries.stream()
                .mapToLong(ResClassAttendanceSummaryDTO.StudentSummary::presentCount)
                .sum();
        long excused = studentSummaries.stream().mapToLong(
                ResClassAttendanceSummaryDTO.StudentSummary::excusedAbsenceCount).sum();
        long unexcused = studentSummaries.stream().mapToLong(
                ResClassAttendanceSummaryDTO.StudentSummary::unexcusedAbsenceCount).sum();
        long late = studentSummaries.stream().mapToLong(ResClassAttendanceSummaryDTO.StudentSummary::lateCount).sum();
        long early = studentSummaries.stream().mapToLong(
                ResClassAttendanceSummaryDTO.StudentSummary::earlyLeaveCount).sum();
        return new ResClassAttendanceSummaryDTO.Summary(present, excused, unexcused, late, early);
    }
}

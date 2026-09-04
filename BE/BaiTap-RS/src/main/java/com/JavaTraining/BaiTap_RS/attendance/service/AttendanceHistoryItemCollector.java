package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqAttendanceHistoryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResStudentAttendanceHistoryDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import org.springframework.stereotype.Component;

@Component
public class AttendanceHistoryItemCollector {

    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final AttendanceHistoryCalendarReader calendarReader;
    private final AttendanceHistorySessionReader sessionReader;
    private final AttendanceHistoryResponseMapper responseMapper;

    public AttendanceHistoryItemCollector(
            StudentYearEnrollmentRepository enrollmentRepository,
            AttendanceHistoryCalendarReader calendarReader,
            AttendanceHistorySessionReader sessionReader,
            AttendanceHistoryResponseMapper responseMapper) {
        this.enrollmentRepository = enrollmentRepository;
        this.calendarReader = calendarReader;
        this.sessionReader = sessionReader;
        this.responseMapper = responseMapper;
    }

    public List<ResStudentAttendanceHistoryDTO.Item> collectItems(Long studentId, ReqAttendanceHistoryQuery query) {
        List<StudentYearEnrollment> enrollments = enrollmentRepository
                .findByStudentIdOrderByEnrolledAtAsc(studentId);
        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate from = query.from() == null ? enrollments.get(0).getEnrolledAt().toLocalDate() : query.from();
        LocalDate to = query.to() == null ? LocalDate.now() : query.to();
        List<CalendarDay> days = calendarReader.findCalendarDays(query.semesterId(), from, to);
        List<Long> classIds = enrollments.stream().map(StudentYearEnrollment::getCurrentClassId).distinct().toList();
        Map<Long, SchoolClass> classes = calendarReader.loadClasses(classIds);
        List<ResStudentAttendanceHistoryDTO.Item> items = new ArrayList<>();

        for (StudentYearEnrollment enrollment : enrollments) {
            if (query.academicYearId() != null && !query.academicYearId().equals(enrollment.getAcademicYearId())) {
                continue;
            }
            SchoolClass schoolClass = classes.get(enrollment.getCurrentClassId());
            Map<String, AttendanceSession> sessions = sessionReader
                    .findSessionsMap(enrollment.getCurrentClassId(), from, to);
            items.addAll(collectEnrollmentItems(enrollment, schoolClass, days, sessions, studentId));
        }
        return items;
    }

    private List<ResStudentAttendanceHistoryDTO.Item> collectEnrollmentItems(
            StudentYearEnrollment enrollment,
            SchoolClass schoolClass,
            List<CalendarDay> days,
            Map<String, AttendanceSession> sessions,
            Long studentId) {
        List<ResStudentAttendanceHistoryDTO.Item> items = new ArrayList<>();
        for (CalendarDay day : days) {
            if (!day.getAcademicYearId().equals(enrollment.getAcademicYearId())
                    || !isEnrolledOn(enrollment, day.getCalendarDate())) {
                continue;
            }
            items.addAll(collectDayItems(day, schoolClass, sessions, studentId));
        }
        return items;
    }

    private List<ResStudentAttendanceHistoryDTO.Item> collectDayItems(
            CalendarDay day,
            SchoolClass schoolClass,
            Map<String, AttendanceSession> sessions,
            Long studentId) {
        List<ResStudentAttendanceHistoryDTO.Item> items = new ArrayList<>();
        for (CalendarSession calendarSession : calendarReader.findCalendarSessions(day.getId())) {
            if (calendarSession.getSessionStatus() != CalendarSessionStatus.SCHEDULED) {
                continue;
            }
            AttendanceSessionPeriod period = AttendanceSessionPeriod.valueOf(
                    calendarSession.getSessionPeriod().name());
            AttendanceSession attendanceSession = sessions.get(day.getCalendarDate() + ":" + period);
            AttendanceRecord record = attendanceSession == null ? null
                    : sessionReader.findRecord(attendanceSession.getId(), studentId).orElse(null);
            items.add(responseMapper.toItem(day, period, schoolClass, record));
        }
        return items;
    }

    private boolean isEnrolledOn(StudentYearEnrollment enrollment, LocalDate date) {
        LocalDateTime start = enrollment.getEnrolledAt();
        LocalDateTime end = enrollment.getCompletedAt();
        return !date.isBefore(start.toLocalDate()) && (end == null || !date.isAfter(end.toLocalDate()));
    }
}

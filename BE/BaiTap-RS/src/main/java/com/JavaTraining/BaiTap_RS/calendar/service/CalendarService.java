package com.JavaTraining.BaiTap_RS.calendar.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.requests.ReqUpsertCalendarDayDTO;
import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.response.ResCalendarDayDTO;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDayType;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;
import com.JavaTraining.BaiTap_RS.calendar.repository.CalendarDayRepository;
import com.JavaTraining.BaiTap_RS.calendar.repository.CalendarSessionRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarService {

    private final CalendarDayRepository dayRepository;
    private final CalendarSessionRepository sessionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final CalendarMapper mapper;
    private final CalendarAuditService auditService;
    private final CalendarSessionService sessionService;
    private final CalendarValidator validator;

    public CalendarService(
            CalendarDayRepository dayRepository,
            CalendarSessionRepository sessionRepository,
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository,
            CalendarMapper mapper,
            CalendarAuditService auditService,
            CalendarSessionService sessionService,
            CalendarValidator validator) {
        this.dayRepository = dayRepository;
        this.sessionRepository = sessionRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.sessionService = sessionService;
        this.validator = validator;
    }

    @Transactional
    public ResCalendarDayDTO upsertDay(LocalDate calendarDate, ReqUpsertCalendarDayDTO request) {
        AcademicYear academicYear = findAcademicYear(request.academicYearId());
        Semester semester = findSemester(request.semesterId());
        validator.validateScope(academicYear, semester, calendarDate);
        validator.validateSessions(request.dayType(), request.sessions());
        Long userId = AuditContext.currentUserId();
        CalendarDay day = dayRepository.findByAcademicYearIdAndCalendarDate(
                        request.academicYearId(), calendarDate)
                .orElseGet(() -> new CalendarDay(
                        request.academicYearId(),
                        request.semesterId(),
                        calendarDate,
                        request.dayType(),
                        request.reason(),
                        userId));
        CalendarDay before = day.getId() == null ? null : auditService.copyDay(day);
        if (before != null) {
            if (!day.getSemesterId().equals(request.semesterId())) {
                throw conflict("Ngày lịch đã thuộc học kỳ khác");
            }
            day.update(request.dayType(), request.reason(), userId);
        }
        dayRepository.save(day);
        sessionService.upsertSessions(day, request.sessions(), userId);
        auditService.writeAudit(before == null ? "CALENDAR_DAY_CREATED" : "CALENDAR_DAY_UPDATED", before, day);
        return toResponse(day);
    }

    @Transactional(readOnly = true)
    public List<ResCalendarDayDTO> listDays(
            Long academicYearId,
            Long semesterId,
            LocalDate from,
            LocalDate to) {
        AcademicYear academicYear = findAcademicYear(academicYearId);
        Semester semester = findSemester(semesterId);
        validator.validateScope(academicYear, semester, from);
        if (to.isBefore(from) || to.isAfter(semester.getEndDate())) {
            throw conflict("Khoảng ngày lịch không hợp lệ");
        }
        return dayRepository.findAllBySemesterIdAndCalendarDateBetweenOrderByCalendarDateAsc(
                        semesterId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void assertScheduled(Long semesterId, LocalDate attendanceDate, String sessionPeriod) {
        Semester semester = findSemester(semesterId);
        CalendarSessionPeriod period = validator.parsePeriod(sessionPeriod);
        CalendarDay day = dayRepository.findByAcademicYearIdAndCalendarDate(
                        semester.getAcademicYearId(), attendanceDate)
                .filter(candidate -> candidate.getSemesterId().equals(semesterId))
                .orElseThrow(() -> conflict("Ngày điểm danh chưa được cấu hình lịch học"));
        boolean scheduled = sessionRepository.existsByCalendarDayIdAndSessionPeriodAndSessionStatus(
                day.getId(), period, CalendarSessionStatus.SCHEDULED);
        if (day.getDayType() != CalendarDayType.SCHOOL_DAY || !scheduled) {
            throw conflict("Buổi điểm danh không phải buổi học hợp lệ");
        }
    }

    private ResCalendarDayDTO toResponse(CalendarDay day) {
        return mapper.toDayResponse(day, sessionRepository.findAllByCalendarDayIdOrderBySessionPeriodAsc(day.getId()));
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy năm học"));
    }

    private Semester findSemester(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}

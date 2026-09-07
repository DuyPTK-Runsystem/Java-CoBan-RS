package com.JavaTraining.BaiTap_RS.calendar.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class CalendarService {

    private static final String ASSERT_SCHEDULED = "CalendarService.assertScheduled";
    private static final String ENSURE_SCHEDULED = "CalendarService.ensureScheduled";

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
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarService.class,
                "CalendarService.upsertDay");
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
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        CalendarService.class,
                        "CalendarService.listDays");
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
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarService.class,
                ASSERT_SCHEDULED);
        Semester semester = findSemester(semesterId);
        CalendarSessionPeriod period = validator.parsePeriod(sessionPeriod);
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarService.class,
                ASSERT_SCHEDULED,
                "lookup academicYearId={}, semesterId={}, date={}, period={}",
                semester.getAcademicYearId(), semesterId, attendanceDate, period);
        Optional<CalendarDay> calendarDayResult = dayRepository.findByAcademicYearIdAndCalendarDate(
                semester.getAcademicYearId(), attendanceDate);
        if (calendarDayResult.isEmpty()) {
            DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                    CalendarService.class,
                    ASSERT_SCHEDULED,
                    "calendar day not found academicYearId={}, date={}, requestedSemesterId={}",
                    semester.getAcademicYearId(), attendanceDate, semesterId);
            throw conflict("Ngày điểm danh chưa được cấu hình lịch học");
        }
        CalendarDay calendarDay = calendarDayResult.get();
        if (!calendarDay.getSemesterId().equals(semesterId)) {
            DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                    CalendarService.class,
                    ASSERT_SCHEDULED,
                    "calendar day semester mismatch dayId={}, calendarSemesterId={}, requestedSemesterId={}, date={}",
                    calendarDay.getId(), calendarDay.getSemesterId(), semesterId, attendanceDate);
            throw conflict("Ngày điểm danh chưa được cấu hình lịch học");
        }
        CalendarDay day = calendarDay;
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarService.class,
                ASSERT_SCHEDULED,
                "calendar day found dayId={}, daySemesterId={}, dayType={}",
                day.getId(), day.getSemesterId(), day.getDayType());
        boolean scheduled = sessionRepository.existsByCalendarDayIdAndSessionPeriodAndSessionStatus(
                day.getId(), period, CalendarSessionStatus.SCHEDULED);
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarService.class,
                ASSERT_SCHEDULED,
                "session lookup dayId={}, period={}, requiredStatus={}, scheduled={}",
                day.getId(), period, CalendarSessionStatus.SCHEDULED, scheduled);
        if (day.getDayType() != CalendarDayType.SCHOOL_DAY || !scheduled) {
            DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                    CalendarService.class,
                    ASSERT_SCHEDULED,
                    "calendar rejected dayId={}, dayType={}, period={}, scheduled={}",
                    day.getId(), day.getDayType(), period, scheduled);
            throw conflict("Buổi điểm danh không phải buổi học hợp lệ");
        }
    }

    @Transactional
    public void ensureScheduled(Long semesterId, LocalDate attendanceDate, String sessionPeriod) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarService.class,
                ENSURE_SCHEDULED,
                "ensuring calendar academicYearId={}, semesterId={}, date={}, period={}",
                "pending", semesterId, attendanceDate, sessionPeriod);
        Semester semester = findSemester(semesterId);
        CalendarSessionPeriod period = validator.parsePeriod(sessionPeriod);
        Optional<CalendarDay> calendarDayResult = dayRepository.findByAcademicYearIdAndCalendarDate(
                semester.getAcademicYearId(), attendanceDate);
        CalendarDay day;
        if (calendarDayResult.isEmpty()) {
            day = dayRepository.save(new CalendarDay(
                    semester.getAcademicYearId(),
                    semesterId,
                    attendanceDate,
                    CalendarDayType.SCHOOL_DAY,
                    null,
                    AuditContext.currentUserId()));
            auditService.writeAudit("CALENDAR_DAY_CREATED", null, day);
            DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                    CalendarService.class,
                    ENSURE_SCHEDULED,
                    "calendar day auto-created dayId={}, academicYearId={}, semesterId={}, date={}, dayType={}",
                    day.getId(), semester.getAcademicYearId(), semesterId, attendanceDate, day.getDayType());
        } else {
            day = calendarDayResult.get();
            if (!day.getSemesterId().equals(semesterId)) {
                throw conflict("Ngày điểm danh chưa được cấu hình lịch học");
            }
            if (day.getDayType() != CalendarDayType.SCHOOL_DAY) {
                throw conflict("Buổi điểm danh không phải buổi học hợp lệ");
            }
        }
        sessionService.ensureScheduled(day, period, AuditContext.currentUserId());
        assertScheduled(semesterId, attendanceDate, sessionPeriod);
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

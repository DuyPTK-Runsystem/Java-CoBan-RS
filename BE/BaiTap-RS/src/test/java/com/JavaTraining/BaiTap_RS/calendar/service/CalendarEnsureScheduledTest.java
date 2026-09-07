package com.JavaTraining.BaiTap_RS.calendar.service;

import java.time.LocalDate;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDayType;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;
import com.JavaTraining.BaiTap_RS.calendar.repository.CalendarDayRepository;
import com.JavaTraining.BaiTap_RS.calendar.repository.CalendarSessionRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CalendarEnsureScheduledTest {
    private static final String MORNING = "MORNING";
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 9, 5);

    @Mock private CalendarDayRepository dayRepository;
    @Mock private CalendarSessionRepository sessionRepository;
    @Mock private SemesterRepository semesterRepository;
    @Mock private CalendarAuditService auditService;
    private CalendarService calendarService;
    private Semester semester;

    @BeforeEach
    void setUp() {
        semester = new Semester(10L, "HK1", "Học kỳ 1", 1, LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 1, 15), null, SemesterStatus.ACTIVE);
        ReflectionTestUtils.setField(semester, "id", 70L);
        calendarService = new CalendarService(dayRepository, sessionRepository, null, semesterRepository,
                new CalendarMapper(), auditService, new CalendarSessionService(sessionRepository, auditService),
                new CalendarValidator());
    }

    @Test
    void ensureScheduledCreatesDay() {
        configureCreation();
        calendarService.ensureScheduled(70L, TEST_DATE, MORNING);
        Mockito.verify(dayRepository).save(Mockito.argThat(day -> day.getDayType() == CalendarDayType.SCHOOL_DAY
                && day.getSemesterId().equals(70L)));
    }

    @Test
    void ensureScheduledCreatesSession() {
        configureCreation();
        calendarService.ensureScheduled(70L, TEST_DATE, MORNING);
        Mockito.verify(sessionRepository).save(Mockito.argThat(session ->
                session.getSessionPeriod() == CalendarSessionPeriod.MORNING
                        && session.getSessionStatus() == CalendarSessionStatus.SCHEDULED));
    }

    @Test
    void ensureScheduledAuditsDay() {
        configureCreation();
        calendarService.ensureScheduled(70L, TEST_DATE, MORNING);
        Mockito.verify(auditService).writeAudit(Mockito.eq("CALENDAR_DAY_CREATED"),
                Mockito.isNull(CalendarDay.class), Mockito.any(CalendarDay.class));
    }

    @Test
    void ensureScheduledAuditsSession() {
        configureCreation();
        calendarService.ensureScheduled(70L, TEST_DATE, MORNING);
        Mockito.verify(auditService).writeSessionAudit(Mockito.eq("CALENDAR_SESSION_CREATED"),
                Mockito.isNull(CalendarSession.class), Mockito.any(CalendarSession.class));
    }

    @Test
    void ensureScheduledRejectsConfiguredNonSchoolDay() {
        configureHoliday();
        Assertions.assertThrows(AppException.class,
                () -> calendarService.ensureScheduled(70L, TEST_DATE, MORNING));
    }

    @Test
    void ensureScheduledDoesNotPersistConfiguredNonSchoolDay() {
        configureHoliday();
        invokeIgnoringFailure();
        Mockito.verify(dayRepository, Mockito.never()).save(Mockito.any(CalendarDay.class));
    }

    private void invokeIgnoringFailure() {
        try {
            calendarService.ensureScheduled(70L, TEST_DATE, MORNING);
        } catch (AppException ignored) {
            // Expected conflict is asserted by the companion test.
        }
    }

    private void configureCreation() {
        CalendarDay created = new CalendarDay(10L, 70L, TEST_DATE, CalendarDayType.SCHOOL_DAY, null, 1L);
        ReflectionTestUtils.setField(created, "id", 90L);
        Mockito.when(semesterRepository.findById(70L)).thenReturn(Optional.of(semester));
        Mockito.when(dayRepository.findByAcademicYearIdAndCalendarDate(10L, TEST_DATE))
                .thenReturn(Optional.empty(), Optional.of(created));
        Mockito.when(dayRepository.save(Mockito.any(CalendarDay.class))).thenReturn(created);
        Mockito.when(sessionRepository.findByCalendarDayIdAndSessionPeriod(90L, CalendarSessionPeriod.MORNING))
                .thenReturn(Optional.empty());
        Mockito.when(sessionRepository.save(Mockito.any(CalendarSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(sessionRepository.existsByCalendarDayIdAndSessionPeriodAndSessionStatus(
                90L, CalendarSessionPeriod.MORNING, CalendarSessionStatus.SCHEDULED)).thenReturn(true);
    }

    private void configureHoliday() {
        CalendarDay holiday = new CalendarDay(10L, 70L, TEST_DATE, CalendarDayType.HOLIDAY, "Holiday", 1L);
        ReflectionTestUtils.setField(holiday, "id", 90L);
        Mockito.when(semesterRepository.findById(70L)).thenReturn(Optional.of(semester));
        Mockito.when(dayRepository.findByAcademicYearIdAndCalendarDate(10L, TEST_DATE))
                .thenReturn(Optional.of(holiday));
    }
}

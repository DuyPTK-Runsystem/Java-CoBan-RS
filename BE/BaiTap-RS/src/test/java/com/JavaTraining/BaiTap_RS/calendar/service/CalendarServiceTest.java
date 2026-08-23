package com.JavaTraining.BaiTap_RS.calendar.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.requests.ReqCalendarSessionDTO;
import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.requests.ReqUpsertCalendarDayDTO;
import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.response.ResCalendarDayDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private CalendarDayRepository dayRepository;

    @Mock
    private CalendarSessionRepository sessionRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private CalendarAuditService auditService;

    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(
                dayRepository,
                sessionRepository,
                academicYearRepository,
                semesterRepository,
                new CalendarMapper(),
                auditService,
                new CalendarSessionService(sessionRepository, auditService),
                new CalendarValidator());
    }

    @Test
    void upsertDayCreatesScheduledMorningSession() {
        AcademicYear year = academicYear();
        Semester semester = semester();
        Mockito.when(academicYearRepository.findById(10L)).thenReturn(Optional.of(year));
        Mockito.when(semesterRepository.findById(70L)).thenReturn(Optional.of(semester));
        Mockito.when(dayRepository.findByAcademicYearIdAndCalendarDate(10L, date()))
                .thenReturn(Optional.empty());
        Mockito.when(dayRepository.save(Mockito.any(CalendarDay.class)))
                .thenAnswer(invocation -> {
                    CalendarDay saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 90L);
                    return saved;
                });
        Mockito.when(sessionRepository.findByCalendarDayIdAndSessionPeriod(
                Mockito.anyLong(), Mockito.any(CalendarSessionPeriod.class))).thenReturn(Optional.empty());
        Mockito.when(sessionRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        CalendarSession savedSession = new CalendarSession(
                90L,
                CalendarSessionPeriod.MORNING,
                CalendarSessionStatus.SCHEDULED,
                null,
                1L);
        ReflectionTestUtils.setField(savedSession, "id", 91L);
        Mockito.when(sessionRepository.findAllByCalendarDayIdOrderBySessionPeriodAsc(90L))
                .thenReturn(List.of(savedSession));

        ResCalendarDayDTO response = calendarService.upsertDay(date(), request(CalendarSessionStatus.SCHEDULED));

        Assertions.assertEquals(
                "SCHOOL_DAY,SCHEDULED,1",
                response.dayType() + "," + response.sessions().get(0).sessionStatus()
                        + "," + response.sessions().size(),
                "day and session should be scheduled");
    }

    @Test
    void upsertDayRejectsScheduledSessionOnHoliday() {
        prepareAcademicScope();

        AppException exception = capture(() -> calendarService.upsertDay(
                date(),
                new ReqUpsertCalendarDayDTO(
                        10L,
                        70L,
                        CalendarDayType.HOLIDAY,
                        "National holiday",
                        List.of(new ReqCalendarSessionDTO(
                                CalendarSessionPeriod.MORNING,
                                CalendarSessionStatus.SCHEDULED,
                                null)))));

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus(), "holiday cannot be scheduled");
    }

    @Test
    void assertScheduledRejectsMissingCalendarSession() {
        Semester semester = semester();
        Mockito.when(semesterRepository.findById(70L)).thenReturn(Optional.of(semester));
        Mockito.when(dayRepository.findByAcademicYearIdAndCalendarDate(10L, date()))
                .thenReturn(Optional.empty());

        AppException exception = capture(() -> calendarService.assertScheduled(70L, date(), "MORNING"));

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus(), "missing calendar is invalid");
    }

    @Test
    void assertScheduledAcceptsScheduledSchoolSession() {
        Semester semester = semester();
        CalendarDay day = new CalendarDay(
                10L,
                70L,
                date(),
                CalendarDayType.SCHOOL_DAY,
                null,
                1L);
        ReflectionTestUtils.setField(day, "id", 90L);
        Mockito.when(semesterRepository.findById(70L)).thenReturn(Optional.of(semester));
        Mockito.when(dayRepository.findByAcademicYearIdAndCalendarDate(10L, date()))
                .thenReturn(Optional.of(day));
        Mockito.when(sessionRepository.existsByCalendarDayIdAndSessionPeriodAndSessionStatus(
                90L,
                CalendarSessionPeriod.MORNING,
                CalendarSessionStatus.SCHEDULED)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> calendarService.assertScheduled(70L, date(), "MORNING"));
    }

    private void prepareAcademicScope() {
        Mockito.when(academicYearRepository.findById(10L)).thenReturn(Optional.of(academicYear()));
        Mockito.when(semesterRepository.findById(70L)).thenReturn(Optional.of(semester()));
    }

    private ReqUpsertCalendarDayDTO request(CalendarSessionStatus status) {
        return new ReqUpsertCalendarDayDTO(
                10L,
                70L,
                CalendarDayType.SCHOOL_DAY,
                null,
                List.of(new ReqCalendarSessionDTO(CalendarSessionPeriod.MORNING, status, null)));
    }

    private AcademicYear academicYear() {
        AcademicYear year = new AcademicYear(
                "2026-2027",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 5, 31),
                AcademicYearStatus.ACTIVE,
                null);
        ReflectionTestUtils.setField(year, "id", 10L);
        return year;
    }

    private Semester semester() {
        Semester semester = new Semester(
                10L,
                "HK1",
                "Học kỳ 1",
                1,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 1, 15),
                null,
                SemesterStatus.ACTIVE);
        ReflectionTestUtils.setField(semester, "id", 70L);
        return semester;
    }

    private LocalDate date() {
        return LocalDate.of(2026, 9, 5);
    }

    private AppException capture(Runnable action) {
        try {
            action.run();
            return new AppException(HttpStatus.OK, "unexpected success");
        } catch (AppException exception) {
            return exception;
        }
    }
}

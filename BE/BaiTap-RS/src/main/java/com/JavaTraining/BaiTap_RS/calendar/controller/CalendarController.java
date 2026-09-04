package com.JavaTraining.BaiTap_RS.calendar.controller;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.requests.ReqUpsertCalendarDayDTO;
import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.response.ResCalendarDayDTO;
import com.JavaTraining.BaiTap_RS.calendar.service.CalendarService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/calendar")
@SuppressWarnings("PMD.GuardLogStatement")
public class CalendarController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String CALENDAR_DATE = "calendarDate";

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @PutMapping("/days/{calendarDate}")
    @ApiMessage("Cấu hình lịch học trong ngày")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResCalendarDayDTO> upsertDay(
            @PathVariable(CALENDAR_DATE)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate calendarDate,
            @Valid @RequestBody ReqUpsertCalendarDayDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        CalendarController.class,
                        "CalendarController.upsertDay");
        return ResponseEntity.status(HttpStatus.OK).body(calendarService.upsertDay(calendarDate, request));
    }

    @GetMapping("/days")
    @ApiMessage("Lấy lịch học theo khoảng ngày")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER', 'STUDENT')")
    public List<ResCalendarDayDTO> listDays(
            @RequestParam("academicYearId") @Positive Long academicYearId,
            @RequestParam("semesterId") @Positive Long semesterId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        CalendarController.class,
                        "CalendarController.listDays");
        return calendarService.listDays(academicYearId, semesterId, from, to);
    }
}

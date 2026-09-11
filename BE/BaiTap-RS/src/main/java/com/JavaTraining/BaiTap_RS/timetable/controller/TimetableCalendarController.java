package com.JavaTraining.BaiTap_RS.timetable.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqSaveCalendarDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResCalendarDTO;
import com.JavaTraining.BaiTap_RS.timetable.service.TimetableCalendarService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v3/timetable-calendars")
@RequiredArgsConstructor
public class TimetableCalendarController {

    private final TimetableCalendarService calendarService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")
    @ApiMessage("Lấy cấu hình lịch học kỳ")
    public ResCalendarDTO getCalendar(@RequestParam("semesterId") @Positive Long semesterId) {
        return calendarService.getOrCreateCalendar(semesterId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    @ApiMessage("Lưu cấu hình lịch học kỳ")
    public ResCalendarDTO saveCalendar(@Valid @RequestBody ReqSaveCalendarDTO req) {
        return calendarService.saveCalendar(req);
    }
}

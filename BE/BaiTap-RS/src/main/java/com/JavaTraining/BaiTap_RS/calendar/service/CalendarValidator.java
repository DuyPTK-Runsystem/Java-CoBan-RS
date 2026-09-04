package com.JavaTraining.BaiTap_RS.calendar.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.requests.ReqCalendarSessionDTO;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDayType;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CalendarValidator {

    public void validateScope(AcademicYear year, Semester semester, LocalDate date) {
        if (!year.getId().equals(semester.getAcademicYearId())) {
            throw conflict("Học kỳ phải thuộc năm học đã chọn");
        }
        if (date.isBefore(year.getStartDate()) || date.isAfter(year.getEndDate())
                || date.isBefore(semester.getStartDate()) || date.isAfter(semester.getEndDate())) {
            throw conflict("Ngày lịch phải nằm trong năm học và học kỳ");
        }
    }

    public void validateSessions(CalendarDayType dayType, List<ReqCalendarSessionDTO> requests) {
        Set<CalendarSessionPeriod> periods = requests.stream()
                .map(ReqCalendarSessionDTO::sessionPeriod)
                .collect(Collectors.toSet());
        if (periods.size() != requests.size()) {
            throw conflict("Mỗi buổi chỉ được cấu hình một lần");
        }
        if (requests.stream().anyMatch(request -> request.sessionStatus() == CalendarSessionStatus.SCHEDULED)
                && dayType != CalendarDayType.SCHOOL_DAY) {
            throw conflict("Ngày không học không thể có buổi SCHEDULED");
        }
    }

    public CalendarSessionPeriod parsePeriod(String sessionPeriod) {
        try {
            return CalendarSessionPeriod.valueOf(sessionPeriod);
        } catch (IllegalArgumentException exception) {
            throw new AppException(HttpStatus.CONFLICT, "Buổi điểm danh không hợp lệ", exception);
        }
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}

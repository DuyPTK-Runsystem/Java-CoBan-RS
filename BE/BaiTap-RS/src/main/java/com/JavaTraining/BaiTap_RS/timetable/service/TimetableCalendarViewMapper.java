package com.JavaTraining.BaiTap_RS.timetable.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResCalendarDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResClosedDateDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResPeriodDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableClosedDate;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableCalendarViewMapper {

    private final TimetableCalendarPeriodService periodService;
    private final TimetableCalendarClosedDateService closedDateService;

    public ResCalendarDTO toResponse(TimetableCalendar calendar) {
        List<ResPeriodDTO> periods = periodService.findPeriods(calendar.getId()).stream().map(this::toPeriodResponse)
                .toList();
        List<ResClosedDateDTO> closedDates = closedDateService.findClosedDates(calendar.getId()).stream()
                .map(this::toClosedDateResponse).toList();
        return new ResCalendarDTO(calendar.getId(), calendar.getSemesterId(), calendar.getVersion(), periods,
                closedDates);
    }

    private ResPeriodDTO toPeriodResponse(TimetablePeriod period) {
        return new ResPeriodDTO(period.getId(), period.getCalendarId(), period.getDayOfWeek(), period.getSession(),
                period.getPeriodIndex(), period.getName(), period.getStartTime(), period.getEndTime());
    }

    private ResClosedDateDTO toClosedDateResponse(TimetableClosedDate closedDate) {
        return new ResClosedDateDTO(closedDate.getId(), closedDate.getCalendarId(), closedDate.getClosedDate(),
                closedDate.getReason());
    }
}

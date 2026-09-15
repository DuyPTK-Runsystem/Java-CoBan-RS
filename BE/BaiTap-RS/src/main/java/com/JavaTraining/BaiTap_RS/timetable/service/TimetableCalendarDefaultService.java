package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableCalendarDefaultService {

    private final TimetableCalendarRepository calendarRepository;
    private final TimetablePeriodRepository periodRepository;

    public TimetableCalendar create(Long semesterId) {
        TimetableCalendar calendar = calendarRepository.save(new TimetableCalendar(semesterId));
        periodRepository.saveAll(defaultPeriods(calendar.getId()));
        return calendar;
    }

    private List<TimetablePeriod> defaultPeriods(Long calendarId) {
        List<TimetablePeriod> periods = new ArrayList<>();
        for (int dayOfWeek = 2; dayOfWeek <= 7; dayOfWeek++) {
            addSession(periods, calendarId, dayOfWeek, SessionType.MORNING, "Sáng", 7, 0);
            addSession(periods, calendarId, dayOfWeek, SessionType.AFTERNOON, "Chiều", 13, 0);
        }
        return periods;
    }

    private void addSession(List<TimetablePeriod> periods, Long calendarId, int dayOfWeek, SessionType session,
            String label, int startHour, int startMinute) {
        int[] minuteOffsets = { 0, 50, 110, 160 };
        for (int periodIndex = 1; periodIndex <= minuteOffsets.length; periodIndex++) {
            LocalTime start = LocalTime.of(startHour, startMinute).plusMinutes(minuteOffsets[periodIndex - 1]);
            periods.add(new TimetablePeriod(calendarId, dayOfWeek, session, periodIndex,
                    label + " Tiết " + periodIndex, start, start.plusMinutes(45)));
        }
    }
}

package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogCalendarDayResponse;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableClosedDate;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableClosedDateRepository;

import lombok.RequiredArgsConstructor;

/** Reads calendar closures and functional-room names for schedule views. */
@Service
@RequiredArgsConstructor
public class LessonLogScheduleCalendarCatalog {
    private final FunctionalRoomRepository rooms;
    private final TimetableCalendarRepository calendars;
    private final TimetableClosedDateRepository closedDateRepository;

    public Optional<FunctionalRoom> room(Long roomId) {
        return roomId == null ? Optional.empty() : rooms.findById(roomId);
    }

    public boolean closedOn(Long semesterId, LocalDate date) {
        return calendars.findBySemesterId(semesterId)
                .map(calendar -> closedDateRepository.existsByCalendarIdAndClosedDate(calendar.getId(), date))
                .orElse(false);
    }

    public Set<LocalDate> closedDates(Long semesterId) {
        return calendars.findBySemesterId(semesterId)
                .map(calendar -> closedDateRepository.findByCalendarIdOrderByClosedDateAsc(calendar.getId()).stream()
                        .map(TimetableClosedDate::getClosedDate).collect(Collectors.toSet()))
                .orElseGet(HashSet::new);
    }

    public List<LessonLogCalendarDayResponse> calendarDays(Semester semester, LocalDate start) {
        Set<LocalDate> holidayDates = closedDates(semester.getId());
        List<LessonLogCalendarDayResponse> calendarDays = new ArrayList<>();
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = start.plusDays(dayOffset);
            calendarDays.add(new LessonLogCalendarDayResponse(date,
                    date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("vi-VN")),
                    dayKind(semester, holidayDates, date)));
        }
        return calendarDays;
    }

    public String roomName(Long roomId) {
        return room(roomId).map(FunctionalRoom::getName).orElse(null);
    }

    private String dayKind(Semester semester, Set<LocalDate> holidayDates, LocalDate date) {
        if (date.isBefore(semester.getStartDate()) || date.isAfter(semester.getEndDate())) {
            return "OUTSIDE_SEMESTER";
        }
        return holidayDates.contains(date) ? "HOLIDAY" : "SCHOOL_DAY";
    }
}

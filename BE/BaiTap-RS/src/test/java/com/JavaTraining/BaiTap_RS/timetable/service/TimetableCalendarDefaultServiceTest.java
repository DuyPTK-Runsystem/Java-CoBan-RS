package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.concurrent.atomic.AtomicReference;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TimetableCalendarDefaultServiceTest {

    @Mock
    private TimetableCalendarRepository calendarRepository;

    @Mock
    private TimetablePeriodRepository periodRepository;

    @InjectMocks
    private TimetableCalendarDefaultService service;

    @Test
    void createBuildsFortyEightPeriodsForIsoSchoolWeekAndMapsMondayToMonday() {
        AtomicReference<List<TimetablePeriod>> savedPeriodsReference = new AtomicReference<>();
        Mockito.when(calendarRepository.save(Mockito.any(TimetableCalendar.class))).thenAnswer(invocation -> {
            TimetableCalendar calendar = invocation.getArgument(0);
            ReflectionTestUtils.setField(calendar, "id", 41L);
            return calendar;
        });
        Mockito.when(periodRepository.saveAll(Mockito.any())).thenAnswer(invocation -> {
            Iterable<TimetablePeriod> requestedPeriods = invocation.getArgument(0);
            List<TimetablePeriod> savedPeriods = StreamSupport.stream(requestedPeriods.spliterator(), false)
                    .toList();
            savedPeriodsReference.set(savedPeriods);
            return savedPeriods;
        });

        service.create(12L);

        List<TimetablePeriod> periods = savedPeriodsReference.get();
        LocalDate mondayDate = LocalDate.of(2026, 10, 5);
        List<Integer> weekdays = periods.stream().map(TimetablePeriod::getDayOfWeek)
                .distinct().sorted().toList();
        Map<Integer, Long> periodsPerWeekday = periods.stream().collect(Collectors.groupingBy(
                TimetablePeriod::getDayOfWeek, TreeMap::new, Collectors.counting()));
        Integer firstMondayPeriodDay = periods.stream()
                .filter(period -> period.getDayOfWeek() == DayOfWeek.MONDAY.getValue())
                .findFirst()
                .map(TimetablePeriod::getDayOfWeek)
                .orElse(null);
        CalendarSummary actual = new CalendarSummary(
                periods.size(), weekdays, periodsPerWeekday, mondayDate.getDayOfWeek(), firstMondayPeriodDay);
        CalendarSummary expected = new CalendarSummary(
                48, List.of(1, 2, 3, 4, 5, 6), Map.of(1, 8L, 2, 8L, 3, 8L, 4, 8L, 5, 8L, 6, 8L),
                DayOfWeek.MONDAY, DayOfWeek.MONDAY.getValue());

        Assertions.assertEquals(expected, actual,
                "default calendar must contain eight periods on each ISO weekday 1-6 and map 1 to Monday");
    }

    private record CalendarSummary(
            int periodCount,
            List<Integer> weekdays,
            Map<Integer, Long> periodsPerWeekday,
            DayOfWeek mondayDateWeekday,
            Integer firstMondayPeriodWeekday) {
    }
}

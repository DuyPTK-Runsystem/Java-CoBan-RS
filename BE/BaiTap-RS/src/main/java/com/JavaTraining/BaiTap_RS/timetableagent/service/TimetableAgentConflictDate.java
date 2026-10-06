package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentPeriodOption;

/** Package-private helper that resolves the first shared weekday date for conflict descriptions. */
final class TimetableAgentConflictDate {

    private TimetableAgentConflictDate() {
    }

    /* package */
    static LocalDate find(List<TimetableEntry> entries, TimetableAgentPeriodOption period) {
        if (!hasValidPeriod(entries, period)) {
            return null;
        }
        LocalDate start = entries.stream().map(TimetableEntry::getValidFrom).filter(Objects::nonNull)
                .max(LocalDate::compareTo).orElse(null);
        LocalDate end = entries.stream().map(TimetableEntry::getValidTo).filter(Objects::nonNull)
                .min(LocalDate::compareTo).orElse(null);
        return firstDateOnPeriodDay(start, end, period.dayOfWeek());
    }

    private static boolean hasValidPeriod(List<TimetableEntry> entries, TimetableAgentPeriodOption period) {
        return period != null && period.dayOfWeek() != null && period.dayOfWeek() >= 1
                && period.dayOfWeek() <= 7 && entries.size() >= 2;
    }

    private static LocalDate firstDateOnPeriodDay(LocalDate start, LocalDate end, Integer dayOfWeek) {
        if (start == null || end == null || start.isAfter(end)) {
            return null;
        }
        LocalDate date = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)));
        return date.isAfter(end) ? null : date;
    }
}

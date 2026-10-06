package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;

record TimetableAgentDateInterval(LocalDate from, LocalDate to) {

    public static TimetableAgentDateInterval fromSnapshot(TimetableAgentSnapshotEntry entry) {
        return new TimetableAgentDateInterval(entry.validFrom(), entry.validTo());
    }

    public static TimetableAgentDateInterval fromEntity(TimetableEntry entry) {
        return new TimetableAgentDateInterval(entry.getValidFrom(), entry.getValidTo());
    }

    public static List<TimetableAgentDateInterval> subtract(TimetableAgentDateInterval source,
            List<TimetableAgentDateInterval> cuts) {
        List<TimetableAgentDateInterval> remaining = new ArrayList<>();
        LocalDate cursor = source.from();
        for (TimetableAgentDateInterval cut : cuts) {
            if (cut.to().isBefore(cursor)) {
                continue;
            }
            if (cut.from().isAfter(source.to())) {
                break;
            }
            appendUncovered(cursor, cut, source.to(), remaining);
            if (!cut.to().isBefore(source.to())) {
                return remaining;
            }
            cursor = cut.to().plusDays(1);
        }
        appendRemainder(cursor, source.to(), remaining);
        return remaining;
    }

    public static List<TimetableAgentDateInterval> merge(List<TimetableAgentDateInterval> intervals) {
        List<TimetableAgentDateInterval> ordered = intervals.stream().sorted(Comparator
                .comparing(TimetableAgentDateInterval::from).thenComparing(TimetableAgentDateInterval::to)).toList();
        List<TimetableAgentDateInterval> result = new ArrayList<>();
        for (TimetableAgentDateInterval interval : ordered) {
            appendMerged(result, interval);
        }
        return result;
    }

    public static List<TimetableAgentDateInterval> mergeWith(List<TimetableAgentDateInterval> intervals,
            TimetableAgentDateInterval interval) {
        List<TimetableAgentDateInterval> updated = new ArrayList<>();
        updated.addAll(intervals);
        updated.add(interval);
        return merge(updated);
    }

    private static void appendUncovered(LocalDate cursor, TimetableAgentDateInterval cut, LocalDate sourceEnd,
            List<TimetableAgentDateInterval> result) {
        LocalDate gapEnd = cut.from().minusDays(1);
        if (cursor.isBefore(cut.from()) && !gapEnd.isAfter(sourceEnd)) {
            result.add(new TimetableAgentDateInterval(cursor, gapEnd));
        }
    }

    private static void appendRemainder(LocalDate cursor, LocalDate sourceEnd,
            List<TimetableAgentDateInterval> result) {
        if (!cursor.isAfter(sourceEnd)) {
            result.add(new TimetableAgentDateInterval(cursor, sourceEnd));
        }
    }

    private static void appendMerged(List<TimetableAgentDateInterval> result,
            TimetableAgentDateInterval interval) {
        if (result.isEmpty() || result.get(result.size() - 1).to().isBefore(interval.from())) {
            result.add(interval);
        } else {
            TimetableAgentDateInterval last = result.remove(result.size() - 1);
            LocalDate end = last.to().isAfter(interval.to()) ? last.to() : interval.to();
            result.add(new TimetableAgentDateInterval(last.from(), end));
        }
    }

}

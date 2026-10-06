package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;

/** Builds immutable occupied context without changing persisted timetable entries. */
public final class TimetableAgentOccupiedContext {

    private TimetableAgentOccupiedContext() {
    }

    /** Removes from published context the days already represented by current entries. */
    public static List<TimetableAgentSnapshotEntry> withoutCurrentOverlap(
            List<TimetableAgentSnapshotEntry> current, List<TimetableAgentSnapshotEntry> published) {
        Map<EntryKey, List<TimetableAgentDateInterval>> occupied = intervals(current);
        List<TimetableAgentSnapshotEntry> result = new ArrayList<>();
        for (TimetableAgentSnapshotEntry entry : published) {
            EntryKey key = EntryKey.fromSnapshot(entry);
            TimetableAgentDateInterval interval = TimetableAgentDateInterval.fromSnapshot(entry);
            List<TimetableAgentDateInterval> remainingIntervals = TimetableAgentDateInterval.subtract(
                    interval, occupied.getOrDefault(key, List.of()));
            for (TimetableAgentDateInterval remaining : remainingIntervals) {
                result.add(new TimetableAgentSnapshotEntry(entry.entryId(), entry.assignmentId(), entry.periodId(),
                        entry.functionalRoomId(), remaining.from(), remaining.to()));
            }
            occupied.put(key, TimetableAgentDateInterval.mergeWith(occupied.getOrDefault(key, List.of()), interval));
        }
        return List.copyOf(result);
    }

    /** Unions overlapping rows with the same assignment, period, and room for derived load evaluation. */
    public static List<TimetableEntry> normalizedEntities(List<TimetableEntry> entries) {
        Map<EntryKey, List<TimetableEntry>> groups = new HashMap<>();
        for (TimetableEntry entry : entries) {
            groups.computeIfAbsent(EntryKey.fromEntity(entry), ignored -> newEntryList()).add(entry);
        }
        List<TimetableEntry> result = new ArrayList<>();
        for (Map.Entry<EntryKey, List<TimetableEntry>> group : groups.entrySet()) {
            List<TimetableEntry> ordered = group.getValue().stream()
                    .sorted(Comparator.comparing(TimetableEntry::getValidFrom)
                            .thenComparing(TimetableEntry::getValidTo)).toList();
            for (TimetableAgentDateInterval interval : TimetableAgentDateInterval.merge(
                    ordered.stream().map(TimetableAgentDateInterval::fromEntity).toList())) {
                TimetableEntry source = ordered.stream().filter(entry -> !entry.getValidFrom().isAfter(interval.to())
                        && !entry.getValidTo().isBefore(interval.from())).findFirst().orElseThrow();
                result.add(copyWithInterval(source, interval));
            }
        }
        return List.copyOf(result);
    }

    private static Map<EntryKey, List<TimetableAgentDateInterval>> intervals(
            List<TimetableAgentSnapshotEntry> entries) {
        Map<EntryKey, List<TimetableAgentDateInterval>> result = new HashMap<>();
        for (TimetableAgentSnapshotEntry entry : entries) {
            result.computeIfAbsent(EntryKey.fromSnapshot(entry), ignored -> newIntervalList())
                    .add(TimetableAgentDateInterval.fromSnapshot(entry));
        }
        result.replaceAll((key, value) -> TimetableAgentDateInterval.merge(value));
        return result;
    }

    private static TimetableEntry copyWithInterval(TimetableEntry source, TimetableAgentDateInterval interval) {
        TimetableEntry copy = new TimetableEntry(source.getRevisionId(), source.getAssignmentId(), source.getPeriodId(),
                source.getFunctionalRoomId(), interval.from(), interval.to());
        copy.setId(source.getId());
        return copy;
    }

    private static List<TimetableEntry> newEntryList() {
        return new ArrayList<>();
    }

    private static List<TimetableAgentDateInterval> newIntervalList() {
        return new ArrayList<>();
    }

    private record EntryKey(Long assignmentId, Long periodId, Long roomId) {
        private static EntryKey fromSnapshot(TimetableAgentSnapshotEntry entry) {
            return new EntryKey(entry.assignmentId(), entry.periodId(), entry.functionalRoomId());
        }

        private static EntryKey fromEntity(TimetableEntry entry) {
            return new EntryKey(entry.getAssignmentId(), entry.getPeriodId(), entry.getFunctionalRoomId());
        }
    }

}

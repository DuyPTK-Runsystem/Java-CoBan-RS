package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableIssueDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentPeriodOption;

/** Package-private formatter for proposal-facing timetable conflict descriptions. */
final class TimetableAgentConflictDescription {

    private TimetableAgentConflictDescription() {
    }

    /* package */
    static ConflictContext describe(TimetableAgentSnapshot snapshot, ResTimetableIssueDTO issue,
            List<TimetableEntry> candidate, List<TimetableEntry> retained) {
        TimetableAgentConflictParticipants.Resolution participants =
                TimetableAgentConflictParticipants.resolve(issue, candidate, retained);
        List<Integer> indexes = candidateIndexes(participants.entries());
        String path = path(indexes);
        if (indexes.isEmpty()) {
            return new ConflictContext(path, "");
        }
        return new ConflictContext(path, detail(snapshot, issue, participants, indexes));
    }

    private static List<Integer> candidateIndexes(List<TimetableAgentConflictParticipants.Participant> participants) {
        return participants.stream().map(TimetableAgentConflictParticipants.Participant::candidateIndex)
                .filter(Objects::nonNull)
                .distinct().sorted().toList();
    }

    private static String path(List<Integer> indexes) {
        if (indexes.isEmpty()) {
            return "entries";
        }
        return String.join(", ", indexes.stream().map(index -> "entries[" + index + "]").toList());
    }

    private static String detail(TimetableAgentSnapshot snapshot, ResTimetableIssueDTO issue,
            TimetableAgentConflictParticipants.Resolution participants, List<Integer> indexes) {
        StringBuilder result = new StringBuilder(128);
        result.append(" Conflict involves proposal ").append(indexes.size() == 1 ? "entry " : "entries ")
                .append(path(indexes));
        if (!participants.counterpartLabels().isEmpty()) {
            result.append(" and ").append(String.join(" and ", participants.counterpartLabels()));
        }
        List<TimetableAgentPeriodOption> periods = periods(snapshot, issue, participants.entries());
        appendPeriodDescription(result, periods);
        LocalDate conflictDate = TimetableAgentConflictDate.find(participants.entries().stream()
                .map(TimetableAgentConflictParticipants.Participant::entry).toList(),
                periods.isEmpty() ? null : periods.get(0));
        if (conflictDate != null) {
            result.append(" on ").append(conflictDate);
        }
        return result.toString();
    }

    private static List<TimetableAgentPeriodOption> periods(TimetableAgentSnapshot snapshot,
            ResTimetableIssueDTO issue, List<TimetableAgentConflictParticipants.Participant> participants) {
        List<TimetableAgentPeriodOption> result = participants.stream()
                .map(TimetableAgentConflictParticipants.Participant::entry)
                .map(TimetableEntry::getPeriodId).map(periodId -> findPeriod(snapshot, periodId))
                .filter(Objects::nonNull).distinct().toList();
        if (!result.isEmpty()) {
            return result;
        }
        TimetableAgentPeriodOption issuePeriod = findPeriod(snapshot, issue.periodId());
        return issuePeriod == null ? List.of() : List.of(issuePeriod);
    }

    private static void appendPeriodDescription(StringBuilder result, List<TimetableAgentPeriodOption> periods) {
        if (periods.isEmpty()) {
            return;
        }
        result.append("; conflicting period");
        if (periods.size() > 1) {
            result.append('s');
        }
        result.append(' ').append(String.join(" and ", periods.stream()
                .map(TimetableAgentConflictDescription::periodDescription).toList()));
    }

    private static TimetableAgentPeriodOption findPeriod(TimetableAgentSnapshot snapshot, Long periodId) {
        if (periodId == null || snapshot.periods() == null) {
            return null;
        }
        return snapshot.periods().stream().filter(Objects::nonNull)
                .filter(period -> Objects.equals(period.periodId(), periodId)).findFirst().orElse(null);
    }

    private static String periodDescription(TimetableAgentPeriodOption period) {
        String label = period.name() != null && !period.name().isBlank() ? period.name()
                : "day " + period.dayOfWeek() + " period " + period.periodIndex();
        if (period.startTime() != null && period.endTime() != null) {
            label = new StringBuilder().append(label).append(" (").append(period.startTime())
                    .append('-').append(period.endTime()).append(')').toString();
        }
        return label;
    }

    /* package */
    record ConflictContext(String path, String detail) {
    }

}

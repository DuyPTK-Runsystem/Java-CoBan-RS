package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentAssignmentOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentPeriodOption;

public class TimetableAgentWeeklyDemandValidator {

    public void validateWeeklyDemand(TimetableAgentSnapshot snapshot,
            List<TimetableAgentProposalEntryDTO> proposed,
            Map<Long, TimetableAgentAssignmentOption> assignments, Long assignmentId,
            int demandPerWeek, List<ResTimetableAgentIssueDTO> issues) {
        TimetableAgentAssignmentOption assignment = assignments.get(assignmentId);
        if (assignment == null) {
            return;
        }
        Map<Long, Integer> dayByPeriod = snapshot.periods().stream().collect(Collectors.toMap(
                TimetableAgentPeriodOption::periodId, TimetableAgentPeriodOption::dayOfWeek, (first, second) -> first));
        LocalDate weekStart = snapshot.validFrom().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!weekStart.isAfter(snapshot.validTo())) {
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDate effectiveFrom = max(snapshot.validFrom(), weekStart, assignment.validFrom());
            LocalDate effectiveTo = min(snapshot.validTo(), weekEnd,
                    assignment.validTo() == null ? snapshot.validTo() : assignment.validTo());
            if (!effectiveFrom.isAfter(effectiveTo)) {
                int actual = countOccurrences(snapshot, proposed, assignmentId, dayByPeriod,
                        effectiveFrom, effectiveTo);
                if (actual != demandPerWeek) {
                    issues.add(TimetableAgentValidationIssues.blocking("DEMAND_MISMATCH",
                            "demands[assignmentId=" + assignmentId + "]",
                            "Proposal has " + actual + " weekly occurrences for week " + weekStart
                                    + "; confirmed demand is " + demandPerWeek + "."));
                }
            }
            weekStart = weekStart.plusWeeks(1);
        }
    }

    private boolean hasOccurrence(LocalDate entryFrom, LocalDate entryTo, LocalDate effectiveFrom,
            LocalDate effectiveTo, int dayOfWeek, List<LocalDate> closedDates) {
        LocalDate from = max(entryFrom, effectiveFrom);
        LocalDate to = min(entryTo, effectiveTo);
        LocalDate first = from.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)));
        if (first.isAfter(to)) {
            return false;
        }
        for (LocalDate occurrence = first; !occurrence.isAfter(to); occurrence = occurrence.plusWeeks(1)) {
            if (!closedDates.contains(occurrence)) {
                return true;
            }
        }
        return false;
    }

    private LocalDate max(LocalDate... dates) {
        return java.util.Arrays.stream(dates).max(Comparator.naturalOrder()).orElseThrow();
    }

    private LocalDate min(LocalDate... dates) {
        return java.util.Arrays.stream(dates).min(Comparator.naturalOrder()).orElseThrow();
    }

    private int countOccurrences(TimetableAgentSnapshot snapshot,
            List<TimetableAgentProposalEntryDTO> proposed, Long assignmentId, Map<Long, Integer> dayByPeriod,
            LocalDate effectiveFrom, LocalDate effectiveTo) {
        int actual = 0;
        for (TimetableAgentProposalEntryDTO entry : proposed) {
            if (!Objects.equals(assignmentId, entry.assignmentId())) {
                continue;
            }
            Integer day = dayByPeriod.get(entry.periodId());
            if (day != null && hasOccurrence(entry.validFrom(), entry.validTo(), effectiveFrom, effectiveTo, day,
                    snapshot.closedDates())) {
                actual++;
            }
        }
        return actual;
    }
}

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
                for (LocalDate date = effectiveFrom; !date.isAfter(effectiveTo); date = date.plusDays(1)) {
                    int actual = countPatternSlots(proposed, assignmentId, dayByPeriod, date);
                    if (actual != demandPerWeek) {
                        issues.add(TimetableAgentValidationIssues.blocking("DEMAND_MISMATCH",
                                "demands[assignmentId=" + assignmentId + "]",
                                "Proposal has " + actual + " weekly pattern slots effective on " + date
                                        + " in week " + weekStart + "; confirmed demand is " + demandPerWeek + "."));
                        break;
                    }
                }
            }
            weekStart = weekStart.plusWeeks(1);
        }
    }

    private LocalDate max(LocalDate... dates) {
        return java.util.Arrays.stream(dates).max(Comparator.naturalOrder()).orElseThrow();
    }

    private LocalDate min(LocalDate... dates) {
        return java.util.Arrays.stream(dates).min(Comparator.naturalOrder()).orElseThrow();
    }

    private int countPatternSlots(List<TimetableAgentProposalEntryDTO> proposed, Long assignmentId,
            Map<Long, Integer> dayByPeriod, LocalDate date) {
        int actual = 0;
        for (TimetableAgentProposalEntryDTO entry : proposed) {
            if (Objects.equals(assignmentId, entry.assignmentId())
                    && dayByPeriod.containsKey(entry.periodId())
                    && !date.isBefore(entry.validFrom()) && !date.isAfter(entry.validTo())) {
                actual++;
            }
        }
        return actual;
    }
}

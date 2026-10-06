package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentAssignmentOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentPeriodOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentRoomOption;

public class TimetableAgentEntryScopeValidator {

    public Set<Long> validate(TimetableAgentSnapshot snapshot, List<TimetableAgentProposalEntryDTO> entries,
            Map<Long, TimetableAgentAssignmentOption> assignments, List<ResTimetableAgentIssueDTO> issues) {
        Set<Long> periodIds = snapshot.periods().stream().map(TimetableAgentPeriodOption::periodId)
                .collect(Collectors.toSet());
        Set<Long> roomIds = snapshot.rooms().stream().map(TimetableAgentRoomOption::roomId)
                .collect(Collectors.toSet());
        Set<String> uniqueSlots = new HashSet<>();
        Set<Long> presentAssignmentIds = new HashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            TimetableAgentProposalEntryDTO entry = entries.get(index);
            String path = "entries[" + index + "]";
            TimetableAgentAssignmentOption assignment = assignments.get(entry.assignmentId());
            if (assignment == null) {
                issues.add(TimetableAgentValidationIssues.blocking("ASSIGNMENT_OUT_OF_SCOPE", path + ".assignmentId",
                        "Assignment is outside the captured active scope."));
                continue;
            }
            validatePeriod(entry, periodIds, path, issues);
            validateDates(snapshot, entry, assignment, path, issues);
            validateRoom(snapshot, entry, assignment, roomIds, path, issues);
            String key = entry.assignmentId() + ":" + entry.periodId() + ":" + entry.validFrom()
                    + ":" + entry.validTo();
            if (!uniqueSlots.add(key)) {
                issues.add(TimetableAgentValidationIssues.blocking("DUPLICATE_ENTRY", path,
                        "Proposal contains a duplicate assignment and period."));
            }
            presentAssignmentIds.add(entry.assignmentId());
        }
        return presentAssignmentIds;
    }

    private void validatePeriod(TimetableAgentProposalEntryDTO entry, Set<Long> periodIds, String path,
            List<ResTimetableAgentIssueDTO> issues) {
        if (!periodIds.contains(entry.periodId())) {
            issues.add(TimetableAgentValidationIssues.blocking("PERIOD_OUT_OF_SCOPE", path + ".periodId",
                    "Period is outside the captured timetable calendar."));
        }
    }

    private void validateDates(TimetableAgentSnapshot snapshot, TimetableAgentProposalEntryDTO entry,
            TimetableAgentAssignmentOption assignment, String path, List<ResTimetableAgentIssueDTO> issues) {
        boolean outsideSnapshot = entry.validFrom().isBefore(snapshot.validFrom())
                || entry.validTo().isAfter(snapshot.validTo()) || entry.validTo().isBefore(entry.validFrom());
        boolean outsideAssignment = assignment.validFrom().isAfter(entry.validFrom())
                || (assignment.validTo() != null && assignment.validTo().isBefore(entry.validTo()));
        if (outsideSnapshot || outsideAssignment) {
            issues.add(TimetableAgentValidationIssues.blocking("DATE_RANGE_OUT_OF_SCOPE", path,
                    "Proposal entry dates must stay within the confirmed range and active assignment."));
        }
    }

    private void validateRoom(TimetableAgentSnapshot snapshot, TimetableAgentProposalEntryDTO entry,
            TimetableAgentAssignmentOption assignment, Set<Long> roomIds, String path,
            List<ResTimetableAgentIssueDTO> issues) {
        if (entry.functionalRoomId() == null) {
            return;
        }
        if (!roomIds.contains(entry.functionalRoomId())) {
            issues.add(TimetableAgentValidationIssues.blocking("ROOM_OUT_OF_SCOPE", path + ".functionalRoomId",
                    "Functional room is outside the captured active room catalog."));
            return;
        }
        if (!snapshot.subjectRoomIds().getOrDefault(assignment.subjectId(), List.of())
                .contains(entry.functionalRoomId())) {
            issues.add(TimetableAgentValidationIssues.blocking("ROOM_SUBJECT_MISMATCH", path + ".functionalRoomId",
                    "Functional room is not eligible for this assignment's subject."));
        }
    }
}

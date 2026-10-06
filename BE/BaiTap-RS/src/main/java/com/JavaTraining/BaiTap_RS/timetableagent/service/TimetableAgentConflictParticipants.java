package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableIssueDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;

/** Package-private resolver for candidate and retained entries referenced by a timetable conflict. */
final class TimetableAgentConflictParticipants {

    private TimetableAgentConflictParticipants() {
    }

    /* package */
    static Resolution resolve(ResTimetableIssueDTO issue, List<TimetableEntry> candidate,
            List<TimetableEntry> retained) {
        Map<Long, Integer> candidateIndexes = candidateIndexes(candidate);
        List<Participant> entries = new ArrayList<>();
        List<String> counterpartLabels = new ArrayList<>();
        List<Long> ids = issue.entryIds() == null ? List.of() : issue.entryIds();
        for (Long id : ids) {
            appendParticipant(id, candidate, retained, candidateIndexes, entries, counterpartLabels);
        }
        return new Resolution(entries, counterpartLabels);
    }

    private static Map<Long, Integer> candidateIndexes(List<TimetableEntry> candidate) {
        Map<Long, Integer> result = new HashMap<>();
        for (int index = 0; index < candidate.size(); index++) {
            Long id = candidate.get(index).getId();
            if (id != null) {
                result.put(id, index);
            }
        }
        return result;
    }

    private static void appendParticipant(Long id, List<TimetableEntry> candidate, List<TimetableEntry> retained,
            Map<Long, Integer> candidateIndexes, List<Participant> entries, List<String> counterpartLabels) {
        Integer index = id == null ? null : candidateIndexes.get(id);
        if (index != null) {
            entries.add(new Participant(candidate.get(index), index));
            return;
        }
        TimetableEntry retainedEntry = findRetained(id, retained);
        if (retainedEntry == null) {
            counterpartLabels.add("an entry with unavailable context");
            return;
        }
        entries.add(new Participant(retainedEntry, null));
        counterpartLabels.add(retainedLabel(retainedEntry));
    }

    private static TimetableEntry findRetained(Long id, List<TimetableEntry> retained) {
        if (id == null) {
            return null;
        }
        return retained.stream().filter(entry -> Objects.equals(entry.getId(), id)).findFirst().orElse(null);
    }

    private static String retainedLabel(TimetableEntry entry) {
        return entry.getAssignmentId() == null ? "retained timetable context"
                : "retained context assignmentId " + entry.getAssignmentId();
    }

    /* package */
    record Participant(TimetableEntry entry, Integer candidateIndex) {
    }

    /* package */
    record Resolution(List<Participant> entries, List<String> counterpartLabels) {
    }
}

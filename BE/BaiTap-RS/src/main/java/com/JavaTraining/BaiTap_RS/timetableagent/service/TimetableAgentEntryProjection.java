package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentDiffDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentExistingEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;

public class TimetableAgentEntryProjection {

    public void validateLockedEntries(TimetableAgentSnapshot snapshot,
            List<TimetableAgentProposalEntryDTO> candidate, List<ResTimetableAgentIssueDTO> issues) {
        Map<Long, TimetableAgentSnapshotEntry> existingById = snapshot.currentEntries().stream()
                .collect(Collectors.toMap(TimetableAgentSnapshotEntry::entryId, item -> item));
        Set<Integer> matchedIndexes = new HashSet<>();
        for (Long lockedId : snapshot.lockedEntryIds()) {
            TimetableAgentSnapshotEntry locked = existingById.get(lockedId);
            int index = locked == null ? -1 : matchingIndex(locked, candidate, matchedIndexes);
            if (index < 0) {
                issues.add(TimetableAgentValidationIssues.blocking("LOCKED_ENTRY_CHANGED",
                        "lockedEntryIds[" + lockedId + "]",
                        "The proposal must preserve every locked entry exactly."));
            } else {
                matchedIndexes.add(index);
            }
        }
    }

    private int matchingIndex(TimetableAgentSnapshotEntry locked,
            List<TimetableAgentProposalEntryDTO> candidate, Set<Integer> matchedIndexes) {
        for (int index = 0; index < candidate.size(); index++) {
            if (!matchedIndexes.contains(index) && sameValue(locked, candidate.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private boolean sameValue(TimetableAgentSnapshotEntry old, TimetableAgentProposalEntryDTO candidate) {
        return Objects.equals(old.assignmentId(), candidate.assignmentId())
                && Objects.equals(old.periodId(), candidate.periodId())
                && Objects.equals(old.functionalRoomId(), candidate.functionalRoomId())
                && Objects.equals(old.validFrom(), candidate.validFrom())
                && Objects.equals(old.validTo(), candidate.validTo());
    }

    public List<TimetableAgentSnapshotEntry> inScopeOldEntries(TimetableAgentSnapshot snapshot,
            Set<Long> scopedAssignmentIds) {
        return snapshot.currentEntries().stream()
                .filter(item -> scopedAssignmentIds.contains(item.assignmentId()))
                .filter(item -> overlaps(item.validFrom(), item.validTo(), snapshot.validFrom(), snapshot.validTo()))
                .toList();
    }

    public List<TimetableEntry> retainedEntities(TimetableAgentSnapshot snapshot, Set<Long> scopedAssignmentIds) {
        List<TimetableEntry> retained = new ArrayList<>();
        long syntheticId = -100_000L;
        for (TimetableAgentSnapshotEntry entry : snapshot.currentEntries()) {
            boolean replaceable = scopedAssignmentIds.contains(entry.assignmentId())
                    && overlaps(entry.validFrom(), entry.validTo(), snapshot.validFrom(), snapshot.validTo());
            if (!replaceable) {
                retained.add(toEntity(snapshot, entry.assignmentId(), entry.periodId(), entry.functionalRoomId(),
                        entry.validFrom(), entry.validTo(), syntheticId--));
                continue;
            }
            if (entry.validFrom().isBefore(snapshot.validFrom())) {
                retained.add(toEntity(snapshot, entry.assignmentId(), entry.periodId(), entry.functionalRoomId(),
                        entry.validFrom(), snapshot.validFrom().minusDays(1), syntheticId--));
            }
            if (entry.validTo().isAfter(snapshot.validTo())) {
                retained.add(toEntity(snapshot, entry.assignmentId(), entry.periodId(), entry.functionalRoomId(),
                        snapshot.validTo().plusDays(1), entry.validTo(), syntheticId--));
            }
        }
        for (TimetableAgentSnapshotEntry entry : snapshot.publishedContextEntries()) {
            retained.add(toEntity(snapshot, entry.assignmentId(), entry.periodId(), entry.functionalRoomId(),
                    entry.validFrom(), entry.validTo(), syntheticId--));
        }
        return retained;
    }

    public List<TimetableEntry> toEntities(TimetableAgentSnapshot snapshot,
            List<TimetableAgentProposalEntryDTO> entries, long startId) {
        List<TimetableEntry> result = new ArrayList<>();
        long id = startId;
        for (TimetableAgentProposalEntryDTO entry : entries) {
            result.add(toEntity(snapshot, entry.assignmentId(), entry.periodId(), entry.functionalRoomId(),
                    entry.validFrom(), entry.validTo(), id--));
        }
        return result;
    }

    private TimetableEntry toEntity(TimetableAgentSnapshot snapshot, Long assignmentId, Long periodId,
            Long roomId, LocalDate from, LocalDate to, Long syntheticId) {
        TimetableEntry result = new TimetableEntry(snapshot.targetRevisionId(), assignmentId, periodId,
                roomId, from, to);
        result.setId(syntheticId);
        return result;
    }

    public ResTimetableAgentDiffDTO buildDiff(List<TimetableAgentSnapshotEntry> existing,
            List<TimetableAgentProposalEntryDTO> proposed) {
        List<ResTimetableAgentExistingEntryDTO> unchanged = new ArrayList<>();
        List<TimetableAgentProposalEntryDTO> added = new ArrayList<>();
        Set<Long> retainedIds = new HashSet<>();
        for (TimetableAgentProposalEntryDTO candidate : proposed) {
            TimetableAgentSnapshotEntry match = existing.stream()
                    .filter(item -> !retainedIds.contains(item.entryId())).filter(item ->
                    Objects.equals(item.assignmentId(), candidate.assignmentId())
                            && Objects.equals(item.periodId(), candidate.periodId())
                            && Objects.equals(item.functionalRoomId(), candidate.functionalRoomId())
                            && Objects.equals(item.validFrom(), candidate.validFrom())
                            && Objects.equals(item.validTo(), candidate.validTo())).findFirst().orElse(null);
            if (match == null) {
                added.add(candidate);
            } else {
                unchanged.add(existingEntry(match));
                retainedIds.add(match.entryId());
            }
        }
        List<ResTimetableAgentExistingEntryDTO> removed = existing.stream()
                .filter(item -> !retainedIds.contains(item.entryId()))
                .map(this::existingEntry)
                .toList();
        return new ResTimetableAgentDiffDTO(List.copyOf(added), removed, List.copyOf(unchanged));
    }

    private ResTimetableAgentExistingEntryDTO existingEntry(TimetableAgentSnapshotEntry item) {
        return new ResTimetableAgentExistingEntryDTO(item.entryId(), item.assignmentId(), item.periodId(),
                item.functionalRoomId(), item.validFrom(), item.validTo());
    }

    private boolean overlaps(LocalDate firstFrom, LocalDate firstTo, LocalDate secondFrom, LocalDate secondTo) {
        return !firstFrom.isAfter(secondTo) && !firstTo.isBefore(secondFrom);
    }

}

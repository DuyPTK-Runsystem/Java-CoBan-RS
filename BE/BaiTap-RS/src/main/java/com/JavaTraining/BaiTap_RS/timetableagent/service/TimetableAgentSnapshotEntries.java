package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentSnapshotEntries {

    private final TimetableRevisionRepository revisionRepository;
    private final TimetableHeadRepository headRepository;
    private final TimetableEntryRepository entryRepository;

    public TimetableRevision revision(ReqCreateTimetableAgentProposalDTO request) {
        TimetableRevision revision = revisionRepository.findById(request.targetRevisionId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Target revision was not found."));
        if (revision.getStatus() != TimetableRevisionStatus.DRAFT) {
            throw invalid("The target revision must be a draft.");
        }
        if (!Objects.equals(revision.getVersion(), request.expectedVersion())) {
            throw new AppException(HttpStatus.CONFLICT, "The target revision has changed. Reload and try again.");
        }
        if (outsideRevisionDates(revision, request)) {
            throw invalid("The requested date range is outside the target draft revision.");
        }
        return revision;
    }

    public EntryData read(TimetableRevision revision, ReqCreateTimetableAgentProposalDTO request,
            TimetableAgentSnapshotCatalog.Catalog catalog) {
        List<TimetableAgentSnapshotEntry> currentEntries = entryRepository.findByRevisionId(revision.getId()).stream()
                .map(this::snapshotEntry)
                .sorted(Comparator.comparing(TimetableAgentSnapshotEntry::entryId)).toList();
        validateLockedEntryScope(request, currentEntries, catalog.activeAssignments(), catalog.classSubjectById());
        return new EntryData(currentEntries, currentPublishedContext(revision, request,
                catalog.selectedAssignmentIds(), currentEntries));
    }

    private List<TimetableAgentSnapshotEntry> currentPublishedContext(TimetableRevision target,
            ReqCreateTimetableAgentProposalDTO request, Set<Long> selectedAssignmentIds,
            List<TimetableAgentSnapshotEntry> targetEntries) {
        TimetableHead head = headRepository.findBySemesterId(target.getSemesterId()).orElse(null);
        if (head == null || head.getCurrentRevisionId() == null
                || Objects.equals(head.getCurrentRevisionId(), target.getId())) {
            return List.of();
        }
        TimetableRevision published = revisionRepository.findById(head.getCurrentRevisionId()).orElse(null);
        if (published == null || published.getStatus() != TimetableRevisionStatus.PUBLISHED) {
            return List.of();
        }
        Set<String> targetKeys = targetEntries.stream().map(this::entryKey).collect(Collectors.toSet());
        return entryRepository.findByRevisionId(published.getId()).stream()
                .filter(entry -> !entry.getValidFrom().isAfter(request.validTo())
                        && !entry.getValidTo().isBefore(request.validFrom()))
                .filter(entry -> !selectedAssignmentIds.contains(entry.getAssignmentId()))
                .map(this::snapshotEntry)
                .filter(entry -> !targetKeys.contains(entryKey(entry)))
                .sorted(Comparator.comparing(TimetableAgentSnapshotEntry::entryId)).toList();
    }

    private String entryKey(TimetableAgentSnapshotEntry entry) {
        return entry.assignmentId() + ":" + entry.periodId() + ":" + entry.functionalRoomId()
                + ":" + entry.validFrom() + ":" + entry.validTo();
    }

    private boolean outsideRevisionDates(TimetableRevision revision, ReqCreateTimetableAgentProposalDTO request) {
        return request.validTo().isBefore(request.validFrom())
                || request.validFrom().isBefore(revision.getEffectiveFrom())
                || (revision.getEffectiveTo() != null && request.validTo().isAfter(revision.getEffectiveTo()));
    }

    private TimetableAgentSnapshotEntry snapshotEntry(TimetableEntry entry) {
        return new TimetableAgentSnapshotEntry(entry.getId(), entry.getAssignmentId(), entry.getPeriodId(),
                entry.getFunctionalRoomId(), entry.getValidFrom(), entry.getValidTo());
    }

    private void validateLockedEntryScope(ReqCreateTimetableAgentProposalDTO request,
            List<TimetableAgentSnapshotEntry> entries, List<SubjectTeachingAssignment> assignments,
            Map<Long, ClassSubject> classSubjects) {
        Set<Long> lockedIds = request.lockedEntryIds() == null ? Set.of() : new HashSet<>(request.lockedEntryIds());
        validateUniqueLockedIds(request, lockedIds);
        Set<Long> scopedAssignments = assignments.stream().map(SubjectTeachingAssignment::getId)
                .collect(Collectors.toSet());
        Map<Long, TimetableAgentSnapshotEntry> entryMap = entries.stream()
                .collect(Collectors.toMap(TimetableAgentSnapshotEntry::entryId, item -> item));
        for (Long lockedId : lockedIds) {
            TimetableAgentSnapshotEntry entry = entryMap.get(lockedId);
            if (outsideLockedScope(entry, request, scopedAssignments)) {
                throw invalid("Every locked entry must belong to the target revision and selected date/class scope.");
            }
            SubjectTeachingAssignment assignment = assignments.stream()
                    .filter(item -> Objects.equals(item.getId(), entry.assignmentId())).findFirst().orElse(null);
            if (assignment == null || !classSubjects.containsKey(assignment.getClassSubjectId())) {
                throw invalid("A locked entry is outside the selected class scope.");
            }
        }
    }

    private void validateUniqueLockedIds(ReqCreateTimetableAgentProposalDTO request, Set<Long> lockedIds) {
        int requestedCount = request.lockedEntryIds() == null ? 0 : request.lockedEntryIds().size();
        if (lockedIds.size() != requestedCount) {
            throw invalid("Locked entry IDs must be unique.");
        }
    }

    private boolean outsideLockedScope(TimetableAgentSnapshotEntry entry,
            ReqCreateTimetableAgentProposalDTO request, Set<Long> assignments) {
        return entry == null || !assignments.contains(entry.assignmentId())
                || entry.validFrom().isBefore(request.validFrom()) || entry.validTo().isAfter(request.validTo());
    }

    private AppException invalid(String message) {
        return new AppException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public record EntryData(List<TimetableAgentSnapshotEntry> current, List<TimetableAgentSnapshotEntry> published) {
    }
}

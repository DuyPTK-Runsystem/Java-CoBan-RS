package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableIssueDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.timetable.service.TeacherLoadEvaluator;
import com.JavaTraining.BaiTap_RS.timetable.service.TimetableValidationService;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentIssueSeverity;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentPeriodOption;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TimetableAgentDomainValidation {

    private final TimetableRevisionRepository revisionRepository;
    private final TimetableValidationService timetableValidationService;
    private final TeacherLoadEvaluator teacherLoadEvaluator;

    public void validate(TimetableAgentSnapshot snapshot, List<TimetableEntry> candidate,
            List<TimetableEntry> retained, List<ResTimetableAgentIssueDTO> issues) {
        TimetableRevision revision = revisionRepository.findById(snapshot.targetRevisionId()).orElse(null);
        if (revision == null) {
            issues.add(TimetableAgentValidationIssues.blocking("REVISION_NOT_FOUND", "targetRevisionId",
                    "Target revision is no longer available."));
            return;
        }
        Map<Long, Integer> candidateIndexes = new HashMap<>();
        for (int index = 0; index < candidate.size(); index++) {
            Long id = candidate.get(index).getId();
            if (id != null) {
                candidateIndexes.put(id, index);
            }
        }
        for (ResTimetableIssueDTO item : timetableValidationService.checkCandidateAgainstExisting(
                revision, candidate, retained)) {
            boolean warning = "WARNING".equals(item.severity());
            String path = "entries";
            String message = item.message();
            if ("TEACHER_OVERLAP".equals(item.code())) {
                ConflictContext context = describeTeacherOverlap(snapshot, item, candidate, retained, candidateIndexes);
                path = context.path();
                message += context.detail();
            }
            issues.add(new ResTimetableAgentIssueDTO(item.code(), warning ? TimetableAgentIssueSeverity.WARNING
                    : TimetableAgentIssueSeverity.BLOCKING, path, message));
        }
        List<TimetableEntry> allEntries = new ArrayList<>(candidate);
        allEntries.addAll(retained);
        appendLoadIssues(snapshot, revision, allEntries, issues);
    }

    private ConflictContext describeTeacherOverlap(TimetableAgentSnapshot snapshot, ResTimetableIssueDTO issue,
            List<TimetableEntry> candidate, List<TimetableEntry> retained, Map<Long, Integer> candidateIndexes) {
        List<ConflictParticipant> participants = new ArrayList<>();
        List<String> counterpartLabels = new ArrayList<>();
        for (Long id : issue.entryIds() == null ? List.<Long>of() : issue.entryIds()) {
            Integer index = id == null ? null : candidateIndexes.get(id);
            if (index != null) {
                participants.add(new ConflictParticipant(candidate.get(index), index, false));
            } else if (id != null && retained.stream().anyMatch(entry -> Objects.equals(entry.getId(), id))) {
                TimetableEntry retainedEntry = retained.stream()
                        .filter(entry -> Objects.equals(entry.getId(), id)).findFirst().orElse(null);
                if (retainedEntry != null) {
                    participants.add(new ConflictParticipant(retainedEntry, null, true));
                    counterpartLabels.add(retainedEntry.getAssignmentId() == null ? "retained timetable context"
                            : "retained context assignmentId " + retainedEntry.getAssignmentId());
                }
            } else {
                counterpartLabels.add("an entry with unavailable context");
            }
        }
        List<Integer> indexes = participants.stream().map(ConflictParticipant::candidateIndex)
                .filter(Objects::nonNull)
                .distinct().sorted().toList();
        String path = indexes.isEmpty() ? "entries" : String.join(", ", indexes.stream()
                .map(index -> "entries[" + index + "]").toList());
        if (indexes.isEmpty()) {
            return new ConflictContext(path, "");
        }

        StringBuilder detail = new StringBuilder(" Conflict involves proposal ");
        detail.append(indexes.size() == 1 ? "entry " : "entries ")
                .append(String.join(", ", indexes.stream().map(index -> "entries[" + index + "]").toList()));
        if (!counterpartLabels.isEmpty()) {
            detail.append(" and ").append(String.join(" and ", counterpartLabels));
        }
        List<TimetableAgentPeriodOption> periods = participants.stream().map(ConflictParticipant::entry)
                .map(TimetableEntry::getPeriodId).map(periodId -> findPeriod(snapshot, periodId))
                .filter(Objects::nonNull).distinct().toList();
        if (periods.isEmpty()) {
            TimetableAgentPeriodOption issuePeriod = findPeriod(snapshot, issue.periodId());
            if (issuePeriod != null) {
                periods = List.of(issuePeriod);
            }
        }
        if (!periods.isEmpty()) {
            detail.append("; conflicting period");
            if (periods.size() > 1) {
                detail.append('s');
            }
            detail.append(' ').append(String.join(" and ", periods.stream()
                    .map(this::periodDescription).toList()));
        }
        LocalDate conflictDate = findConflictDate(participants.stream().map(ConflictParticipant::entry).toList(),
                periods.isEmpty() ? null : periods.get(0));
        if (conflictDate != null) {
            detail.append(" on ").append(conflictDate);
        }
        return new ConflictContext(path, detail.toString() + ".");
    }

    private LocalDate findConflictDate(List<TimetableEntry> entries, TimetableAgentPeriodOption period) {
        if (period == null || period.dayOfWeek() == null || period.dayOfWeek() < 1 || period.dayOfWeek() > 7
                || entries.size() < 2) {
            return null;
        }
        LocalDate start = entries.stream().map(TimetableEntry::getValidFrom).filter(Objects::nonNull)
                .max(LocalDate::compareTo).orElse(null);
        LocalDate end = entries.stream().map(TimetableEntry::getValidTo).filter(Objects::nonNull)
                .min(LocalDate::compareTo).orElse(null);
        if (start == null || end == null || start.isAfter(end)) {
            return null;
        }
        LocalDate date = start.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(period.dayOfWeek())));
        return date.isAfter(end) ? null : date;
    }

    private TimetableAgentPeriodOption findPeriod(TimetableAgentSnapshot snapshot, Long periodId) {
        if (periodId == null || snapshot.periods() == null) {
            return null;
        }
        return snapshot.periods().stream().filter(Objects::nonNull)
                .filter(period -> Objects.equals(period.periodId(), periodId))
                .findFirst().orElse(null);
    }

    private String periodDescription(TimetableAgentPeriodOption period) {
        String label = period.name() != null && !period.name().isBlank() ? period.name()
                : "day " + period.dayOfWeek() + " period " + period.periodIndex();
        if (period.startTime() != null && period.endTime() != null) {
            label += " (" + period.startTime() + "-" + period.endTime() + ")";
        }
        return label;
    }

    private record ConflictContext(String path, String detail) {
    }

    private record ConflictParticipant(TimetableEntry entry, Integer candidateIndex, boolean retained) {
    }

    private void appendLoadIssues(TimetableAgentSnapshot snapshot, TimetableRevision revision,
            List<TimetableEntry> entries, List<ResTimetableAgentIssueDTO> issues) {
        LocalDate weekStart = snapshot.validFrom().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!weekStart.isAfter(snapshot.validTo())) {
            LocalDate weekEnd = weekStart.plusDays(6);
            List<ResTeacherLoadDTO> loads = teacherLoadEvaluator.evaluateLoads(entries, weekStart, weekEnd,
                    revision.getPolicyId());
            for (ResTeacherLoadDTO load : loads) {
                if ("LOAD_UNDETERMINED".equals(load.evaluationStatus())) {
                    issues.add(TimetableAgentValidationIssues.blocking("LOAD_UNDETERMINED", "teacherLoads",
                            "Teacher load cannot be determined from the confirmed policy."));
                } else if ("LOAD_BELOW_TARGET".equals(load.evaluationStatus())) {
                    issues.add(new ResTimetableAgentIssueDTO("LOAD_BELOW_TARGET", TimetableAgentIssueSeverity.WARNING,
                            "teacherLoads", "Teacher load is below the policy target for week " + weekStart + "."));
                }
            }
            weekStart = weekStart.plusWeeks(1);
        }
    }

}

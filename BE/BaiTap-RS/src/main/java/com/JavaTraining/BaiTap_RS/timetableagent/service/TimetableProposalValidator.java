package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.timetable.service.TeacherLoadEvaluator;
import com.JavaTraining.BaiTap_RS.timetable.service.TimetableValidationService;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentIssueSeverity;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqTimetableAgentDemandDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentDiffDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentIssueDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentAssignmentOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableProposalValidator {

    private final TimetableRevisionRepository revisionRepository;
    private final SubjectTeachingAssignmentRepository assignmentRepository;
    private final TimetableValidationService timetableValidationService;
    private final TeacherLoadEvaluator teacherLoadEvaluator;

    public TimetableAgentValidationResult validate(TimetableAgentSnapshot snapshot,
            TimetableAgentModelProposalDTO proposal) {
        if (!"1".equals(proposal.schemaVersion()) || !Objects.equals(snapshot.snapshotId(), proposal.snapshotId())) {
            return new TimetableAgentValidationResult(TimetableAgentProposalStatus.CONFLICTS, List.of(),
                    List.of(TimetableAgentValidationIssues.blocking("SNAPSHOT_MISMATCH", "snapshotId",
                            "Proposal snapshot does not match the request.")),
                    TimetableAgentValidationIssues.emptyDiff(), proposal.explanation());
        }
        if (proposal.status() == TimetableAgentModelProposalStatus.NEEDS_INPUT) {
            return TimetableAgentValidationIssues.terminal(proposal, TimetableAgentProposalStatus.NEEDS_INPUT,
                    TimetableAgentIssueSeverity.BLOCKING);
        }
        if (proposal.status() == TimetableAgentModelProposalStatus.NO_SOLUTION_FOUND) {
            return TimetableAgentValidationIssues.terminal(proposal, TimetableAgentProposalStatus.NO_SOLUTION_FOUND,
                    TimetableAgentIssueSeverity.WARNING);
        }
        return validateProposed(snapshot, proposal);
    }

    private TimetableAgentValidationResult validateProposed(TimetableAgentSnapshot snapshot,
            TimetableAgentModelProposalDTO proposal) {
        List<ResTimetableAgentIssueDTO> issues = new ArrayList<>();
        proposal.unresolvedConstraints().forEach(item -> issues.add(TimetableAgentValidationIssues.blocking(
                item.code(), item.field(), item.message())));
        Map<Long, TimetableAgentAssignmentOption> assignments = snapshot.assignments().stream()
                .collect(Collectors.toMap(TimetableAgentAssignmentOption::assignmentId, item -> item));
        List<TimetableAgentProposalEntryDTO> entries = proposal.entries();
        Set<Long> present = new TimetableAgentEntryScopeValidator().validate(snapshot, entries, assignments, issues);
        validateDemands(snapshot, entries, assignments, present, issues);
        TimetableAgentEntryProjection projection = new TimetableAgentEntryProjection();
        projection.validateLockedEntries(snapshot, entries, issues);
        new TimetableAgentDomainValidation(revisionRepository, timetableValidationService, teacherLoadEvaluator)
                .validate(snapshot, projection.toEntities(snapshot, entries, -1L),
                        projection.retainedEntities(snapshot, assignments.keySet()), issues);
        ResTimetableAgentDiffDTO diff = projection.buildDiff(
                projection.inScopeOldEntries(snapshot, assignments.keySet()), entries);
        boolean hasBlocking = issues.stream().anyMatch(item -> item.severity() == TimetableAgentIssueSeverity.BLOCKING);
        return new TimetableAgentValidationResult(hasBlocking ? TimetableAgentProposalStatus.CONFLICTS
                : TimetableAgentProposalStatus.READY_FOR_REVIEW, entries, List.copyOf(issues), diff,
                proposal.explanation());
    }

    private void validateDemands(TimetableAgentSnapshot snapshot, List<TimetableAgentProposalEntryDTO> entries,
            Map<Long, TimetableAgentAssignmentOption> assignments, Set<Long> present,
            List<ResTimetableAgentIssueDTO> issues) {
        Map<Long, Integer> required = snapshot.demands().stream().collect(Collectors.toMap(
                ReqTimetableAgentDemandDTO::assignmentId, ReqTimetableAgentDemandDTO::periodsPerWeek));
        TimetableAgentWeeklyDemandValidator validator = new TimetableAgentWeeklyDemandValidator();
        required.forEach((id, periods) -> validator.validateWeeklyDemand(snapshot, entries, assignments,
                id, periods, issues));
        present.stream().filter(id -> !required.containsKey(id)).forEach(id ->
                issues.add(TimetableAgentValidationIssues.blocking("DEMAND_OUT_OF_SCOPE", "entries",
                        "Proposal contains an assignment without confirmed demand.")));
    }
}

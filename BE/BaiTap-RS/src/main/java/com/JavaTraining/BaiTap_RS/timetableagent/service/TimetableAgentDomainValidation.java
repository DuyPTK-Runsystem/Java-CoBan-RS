package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

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
        for (ResTimetableIssueDTO item : timetableValidationService.checkCandidateAgainstExisting(
                revision, candidate, retained)) {
            boolean warning = "WARNING".equals(item.severity());
            String path = "entries";
            String detail = "";
            if ("TEACHER_OVERLAP".equals(item.code())) {
                TimetableAgentConflictDescription.ConflictContext context =
                        TimetableAgentConflictDescription.describe(snapshot, item, candidate, retained);
                path = context.path();
                detail = context.detail();
            }
            issues.add(new ResTimetableAgentIssueDTO(item.code(), warning ? TimetableAgentIssueSeverity.WARNING
                    : TimetableAgentIssueSeverity.BLOCKING, path, appendDetail(item.message(), detail)));
        }
        List<TimetableEntry> allEntries = new ArrayList<>(candidate);
        allEntries.addAll(retained);
        appendLoadIssues(snapshot, revision, allEntries, issues);
    }

    private String appendDetail(String message, String detail) {
        return new StringBuilder().append(message).append(detail).toString();
    }

    private void appendLoadIssues(TimetableAgentSnapshot snapshot, TimetableRevision revision,
            List<TimetableEntry> entries, List<ResTimetableAgentIssueDTO> issues) {
        LocalDate weekStart = snapshot.validFrom().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!weekStart.isAfter(snapshot.validTo())) {
            LocalDate weekEnd = weekStart.plusDays(6);
            List<ResTeacherLoadDTO> loads = teacherLoadEvaluator.evaluateLoads(
                    TimetableAgentOccupiedContext.normalizedEntities(entries), weekStart, weekEnd,
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

package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.timetable.service.TeacherLoadEvaluator;
import com.JavaTraining.BaiTap_RS.timetable.service.TimetableValidationService;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqTimetableAgentDemandDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentAssignmentOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentPeriodOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class TimetableProposalValidatorTest {

    private static final String SNAPSHOT_ID = "snap-1";
    private final TimetableRevisionRepository revisionRepository = Mockito.mock(TimetableRevisionRepository.class);
    private final SubjectTeachingAssignmentRepository assignmentRepository =
            Mockito.mock(SubjectTeachingAssignmentRepository.class);
    private final TimetableValidationService timetableValidationService =
            Mockito.mock(TimetableValidationService.class);
    private final TeacherLoadEvaluator teacherLoadEvaluator = Mockito.mock(TeacherLoadEvaluator.class);
    private TimetableProposalValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TimetableProposalValidator(revisionRepository, assignmentRepository,
                timetableValidationService, teacherLoadEvaluator);
        TimetableRevision revision = Mockito.mock(TimetableRevision.class);
        Mockito.when(revision.getPolicyId()).thenReturn(3L);
        Mockito.when(revisionRepository.findById(77L)).thenReturn(Optional.of(revision));
        Mockito.when(timetableValidationService.checkCandidateAgainstExisting(
                ArgumentMatchers.any(), ArgumentMatchers.anyList(), ArgumentMatchers.anyList()))
                .thenReturn(List.of());
        Mockito.when(teacherLoadEvaluator.evaluateLoads(ArgumentMatchers.anyList(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(List.of());
    }

    @Test
    void validateCountsCompletePatternEvenWhenWeekdayFallsOutsidePartialScope() {
        LocalDate from = LocalDate.of(2026, 10, 6); // Tuesday
        LocalDate to = LocalDate.of(2026, 10, 8); // Thursday
        TimetableAgentSnapshot snapshot = snapshot(from, to, List.of(), List.of(), List.of(1L), List.of());
        TimetableAgentModelProposalDTO proposal =
                proposal(SNAPSHOT_ID, List.of(entry(1L, 101L, null, from, to)));

        TimetableAgentValidationResult result = validator.validate(snapshot, proposal);

        Assertions.assertFalse(result.issues().stream().anyMatch(issue -> "DEMAND_MISMATCH".equals(issue.code())),
                "partial dates clip actual lessons without reducing the complete weekly pattern");
    }

    @Test
    void validatePassesEffectiveEntriesOutsideSelectedAssignmentsToConflictValidation() {
        LocalDate from = LocalDate.of(2026, 10, 5);
        LocalDate to = LocalDate.of(2026, 10, 11);
        TimetableAgentSnapshotEntry external = new TimetableAgentSnapshotEntry(
                900L, 2L, 101L, 700L, from, to);
        TimetableAgentSnapshot snapshot = snapshot(from, to, List.of(), List.of(external), List.of(1L), List.of());
        TimetableAgentModelProposalDTO proposal = proposal(SNAPSHOT_ID,
                List.of(entry(1L, 101L, 700L, from, to)));

        validator.validate(snapshot, proposal);

        List<com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry> retainedEntries =
                captureRetainedEntriesPassedToConflictValidation();
        Assertions.assertTrue(retainedEntries.size() == 1
                        && Long.valueOf(2L).equals(retainedEntries.get(0).getAssignmentId())
                        && Long.valueOf(700L).equals(retainedEntries.get(0).getFunctionalRoomId()),
                "published entries outside the selected assignment scope must reach conflict validation");
    }

    @Test
    void validateKeepsNeedsInputTerminalWithoutRunningBackendCandidateValidation() {
        LocalDate from = LocalDate.of(2026, 10, 5);
        LocalDate to = LocalDate.of(2026, 10, 11);
        TimetableAgentSnapshot snapshot = snapshot(from, to, List.of(), List.of(), List.of(1L), List.of());
        TimetableAgentModelProposalDTO proposal = new TimetableAgentModelProposalDTO(
                "1", TimetableAgentModelProposalStatus.NEEDS_INPUT, SNAPSHOT_ID,
                List.of(), List.of(), "Need more information");

        TimetableAgentValidationResult result = validator.validate(snapshot, proposal);

        Assertions.assertEquals(TimetableAgentProposalStatus.NEEDS_INPUT, result.status());
        Mockito.verifyNoInteractions(revisionRepository, assignmentRepository,
                timetableValidationService, teacherLoadEvaluator);
    }

    @Test
    void validateRequiresEveryLockedEntryMultiplicityToBePreserved() {
        LocalDate from = LocalDate.of(2026, 10, 5);
        LocalDate to = LocalDate.of(2026, 10, 11);
        TimetableAgentSnapshotEntry locked1 = new TimetableAgentSnapshotEntry(901L, 1L, 101L, null, from, to);
        TimetableAgentSnapshotEntry locked2 = new TimetableAgentSnapshotEntry(902L, 1L, 101L, null, from, to);
        TimetableAgentSnapshot snapshot = snapshot(from, to, List.of(locked1, locked2), List.of(), List.of(1L),
                List.of(901L, 902L));

        TimetableAgentValidationResult result = validator.validate(snapshot, proposal(SNAPSHOT_ID,
                List.of(entry(1L, 101L, null, from, to))));

        Assertions.assertTrue(TimetableAgentProposalStatus.CONFLICTS.equals(result.status())
                        && result.issues().stream().anyMatch(issue -> "LOCKED_ENTRY_CHANGED".equals(issue.code())),
                "one proposed row cannot satisfy two separately locked source rows");
    }

    @SuppressWarnings("unchecked")
    private List<com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry>
            captureRetainedEntriesPassedToConflictValidation() {
        ArgumentCaptor<List<com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry>> captor =
                ArgumentCaptor.forClass(List.class);
        Mockito.verify(timetableValidationService).checkCandidateAgainstExisting(
                ArgumentMatchers.any(), ArgumentMatchers.anyList(), captor.capture());
        return captor.getValue();
    }

    private TimetableAgentSnapshot snapshot(LocalDate from, LocalDate to,
            List<TimetableAgentSnapshotEntry> currentEntries, List<TimetableAgentSnapshotEntry> publishedEntries,
            List<Long> assignmentIds, List<Long> lockedIds) {
        List<TimetableAgentAssignmentOption> assignments = assignmentIds.stream()
                .map(id -> new TimetableAgentAssignmentOption(id, id, "Class " + id, id + 100, "Subject",
                        id + 200, "Teacher", from, to, 1))
                .toList();
        return new TimetableAgentSnapshot(SNAPSHOT_ID, "fingerprint", "{}", "{}", "{}", 9L, 77L, 5L,
                4L, from, to, List.of(1L), List.of(new ReqTimetableAgentDemandDTO(1L, 1)), lockedIds,
                currentEntries, publishedEntries, assignments,
                List.of(new TimetableAgentPeriodOption(101L, 1, "MORNING", 1,
                        "Period 1", "07:00", "07:45")), List.of(), List.of(), List.of(), Map.of());
    }

    private TimetableAgentModelProposalDTO proposal(String snapshotId, List<TimetableAgentProposalEntryDTO> entries) {
        return new TimetableAgentModelProposalDTO("1", TimetableAgentModelProposalStatus.PROPOSED,
                snapshotId, entries, List.of(), "test proposal");
    }

    private TimetableAgentProposalEntryDTO entry(Long assignmentId, Long periodId, Long roomId,
            LocalDate from, LocalDate to) {
        return new TimetableAgentProposalEntryDTO(assignmentId, periodId, roomId, from, to);
    }
}

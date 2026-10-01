package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicyStatus;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailability;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadEligibilityRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadPolicyRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadRuleRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherUnavailabilityRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableClosedDateRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.timetable.service.TeacherLoadEvaluator;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqTimetableAgentDemandDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class TimetableSnapshotServiceTest {

    private final TimetableRevisionRepository revisionRepository = Mockito.mock(TimetableRevisionRepository.class);
    private final TimetableHeadRepository headRepository = Mockito.mock(TimetableHeadRepository.class);
    private final TeacherLoadPolicyRepository loadPolicyRepository = Mockito.mock(TeacherLoadPolicyRepository.class);
    private final TeacherLoadRuleRepository loadRuleRepository = Mockito.mock(TeacherLoadRuleRepository.class);
    private final TeacherLoadEligibilityRepository eligibilityRepository =
            Mockito.mock(TeacherLoadEligibilityRepository.class);
    private final HomeroomAssignmentRepository homeroomRepository = Mockito.mock(HomeroomAssignmentRepository.class);
    private final TeacherLoadEvaluator teacherLoadEvaluator = Mockito.mock(TeacherLoadEvaluator.class);
    private final TimetableEntryRepository entryRepository = Mockito.mock(TimetableEntryRepository.class);
    private final TimetableCalendarRepository calendarRepository = Mockito.mock(TimetableCalendarRepository.class);
    private final TimetablePeriodRepository periodRepository = Mockito.mock(TimetablePeriodRepository.class);
    private final TimetableClosedDateRepository closedDateRepository =
            Mockito.mock(TimetableClosedDateRepository.class);
    private final ClassSubjectRepository classSubjectRepository = Mockito.mock(ClassSubjectRepository.class);
    private final SchoolClassRepository schoolClassRepository = Mockito.mock(SchoolClassRepository.class);
    private final SubjectRepository subjectRepository = Mockito.mock(SubjectRepository.class);
    private final SubjectFunctionalRoomRepository subjectFunctionalRoomRepository =
            Mockito.mock(SubjectFunctionalRoomRepository.class);
    private final SubjectTeachingAssignmentRepository assignmentRepository =
            Mockito.mock(SubjectTeachingAssignmentRepository.class);
    private final TeacherRepository teacherRepository = Mockito.mock(TeacherRepository.class);
    private final FunctionalRoomRepository roomRepository = Mockito.mock(FunctionalRoomRepository.class);
    private final TeacherUnavailabilityRepository unavailabilityRepository =
            Mockito.mock(TeacherUnavailabilityRepository.class);

    private TimetableSnapshotService snapshotService;
    private TimetableRevision revision;

    @BeforeEach
    void setUp() {
        snapshotService = new TimetableSnapshotService(
                new TimetableAgentSnapshotEntries(revisionRepository, headRepository, entryRepository),
                new TimetableAgentSnapshotCatalog(classSubjectRepository,
                        new TimetableAgentSnapshotAssignments(assignmentRepository),
                        new TimetableAgentSnapshotAssignmentLabels(schoolClassRepository, subjectRepository,
                                teacherRepository)),
                new TimetableAgentSnapshotCalendar(calendarRepository, periodRepository, closedDateRepository,
                        new TimetableAgentSnapshotRooms(roomRepository, subjectFunctionalRoomRepository),
                        unavailabilityRepository),
                new TimetableAgentSnapshotPolicy(loadPolicyRepository, loadRuleRepository,
                        new TimetableAgentSnapshotPolicyScope(eligibilityRepository, homeroomRepository),
                        new TimetableAgentSnapshotLoads(teacherLoadEvaluator)),
                new ObjectMapper().findAndRegisterModules());

        revision = new TimetableRevision(1L, 5L, 1, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31), 13L);
        ReflectionTestUtils.setField(revision, "id", 42L);
        ReflectionTestUtils.setField(revision, "version", 7L);
        TeacherLoadPolicy policy = new TeacherLoadPolicy("P13", "Test policy", LocalDate.of(2026, 1, 1),
                null, 19, 4, 3);
        ReflectionTestUtils.setField(policy, "id", 13L);
        policy.setStatus(TeacherLoadPolicyStatus.ACTIVE);
        Mockito.when(loadPolicyRepository.findById(13L)).thenReturn(Optional.of(policy));
        Mockito.when(loadPolicyRepository.findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus.ACTIVE))
                .thenReturn(Optional.of(policy));
        SubjectTeachingAssignment assignment = new SubjectTeachingAssignment(100L, 201L, LocalDate.of(2026, 9, 1),
                null, AssignmentStatus.ACTIVE, 9L);
        ReflectionTestUtils.setField(assignment, "id", 501L);

        Mockito.when(revisionRepository.findById(42L)).thenReturn(Optional.of(revision));
        ClassSubject classSubject = new ClassSubject(11L, 99L, 5L, ClassSubjectStatus.ACTIVE);
        ReflectionTestUtils.setField(classSubject, "id", 100L);
        Mockito.when(classSubjectRepository.findAllBySemesterId(5L)).thenReturn(List.of(classSubject));
        Mockito.when(schoolClassRepository.findAllById(ArgumentMatchers.any())).thenReturn(List.of(
                new SchoolClass(1L, 10L, "10A1", "10A1", 40, SchoolClassStatus.ACTIVE)));
        ReflectionTestUtils.setField(schoolClassRepository.findAllById(List.of(11L)).get(0), "id", 11L);
        Mockito.when(subjectRepository.findAllById(ArgumentMatchers.any())).thenReturn(List.of(
                new Subject("MATH", "ToÃ¡n", SubjectType.ACADEMIC, ApplicationScope.GRADE, SubjectStatus.ACTIVE)));
        ReflectionTestUtils.setField(subjectRepository.findAllById(List.of(99L)).get(0), "id", 99L);
        Mockito.when(assignmentRepository.findAllByClassIdAndSemesterIdOrderByValidFromDesc(11L, 5L))
                .thenReturn(List.of(assignment));
        Teacher teacher = Mockito.mock(Teacher.class);
        Mockito.when(teacher.getId()).thenReturn(201L);
        Mockito.when(teacher.getTeacherName()).thenReturn("CÃ´ An");
        Mockito.when(teacherRepository.findAllById(ArgumentMatchers.any())).thenReturn(List.of(teacher));

        TimetableCalendar calendar = new TimetableCalendar(5L);
        ReflectionTestUtils.setField(calendar, "id", 77L);
        ReflectionTestUtils.setField(calendar, "version", 2L);
        Mockito.when(calendarRepository.findBySemesterId(5L)).thenReturn(Optional.of(calendar));
        TimetablePeriod period = new TimetablePeriod(77L, 2, SessionType.MORNING, 1,
                "Tiáº¿t 1", LocalTime.of(7, 0), LocalTime.of(7, 45));
        ReflectionTestUtils.setField(period, "id", 701L);
        Mockito.when(periodRepository.findByCalendarIdOrderByDayOfWeekAscSessionAscPeriodIndexAsc(77L))
                .thenReturn(List.of(period));
        Mockito.when(roomRepository.findByStatus(RoomStatus.ACTIVE)).thenReturn(List.of());
        Mockito.when(subjectFunctionalRoomRepository.findAll()).thenReturn(List.of());
        Mockito.when(entryRepository.findByRevisionId(42L)).thenReturn(List.of());
        Mockito.when(unavailabilityRepository.findApprovedInSemester(5L, LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 12, 31))).thenReturn(List.of());
        Mockito.when(closedDateRepository.findByCalendarIdOrderByClosedDateAsc(77L)).thenReturn(List.of());
    }

    @Test
    void createBuildsScopedSnapshotAndOmitsPrivateUnavailabilityNotesWithoutTimetableWrites() {
        TeacherUnavailability availability = new TeacherUnavailability(201L, 5L, 2, null,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31), SessionType.MORNING,
                "1, 3", "PERSONAL_LEAVE_SECRET");
        ReflectionTestUtils.setField(availability, "decisionReason", "APPROVER_REASON_SECRET");
        Mockito.when(unavailabilityRepository.findApprovedInSemester(5L, LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 12, 31))).thenReturn(List.of(availability));

        TimetableAgentSnapshot snapshot = snapshotService.create(9L, request(List.of(501L), List.of()));

        checkScopedSnapshotPrivacyContract(snapshot);
        checkNoTimetableWrites();
    }

    @Test
    void createRejectsIncompleteWeeklyDemandBeforeBuildingModelContext() {
        expectRejectedWithoutTimetableWrites(() -> snapshotService.create(9L, request(List.of(), List.of())));
    }

    @Test
    void createRejectsDuplicateAssignmentDemandBeforeBuildingModelContext() {
        expectRejectedWithoutTimetableWrites(
                () -> snapshotService.create(9L, request(List.of(501L, 501L), List.of())));
    }

    @Test
    void createRejectsLockedEntryOutsideSelectedAssignmentScope() {
        TimetableEntry foreignEntry = new TimetableEntry(42L, 999L, 701L, null,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31));
        ReflectionTestUtils.setField(foreignEntry, "id", 808L);
        Mockito.when(entryRepository.findByRevisionId(42L)).thenReturn(List.of(foreignEntry));

        expectRejectedWithoutTimetableWrites(
                () -> snapshotService.create(9L, request(List.of(501L), List.of(808L))));
    }

    @Test
    void createRejectsMissingTeacherLoadPolicyBeforeModelContext() {
        revision.setPolicyId(null);

        expectRejectedWithoutTimetableWrites(() -> snapshotService.create(9L, request(List.of(501L), List.of())));
    }

    private ReqCreateTimetableAgentProposalDTO request(List<Long> demandIds, List<Long> lockedIds) {
        List<ReqTimetableAgentDemandDTO> demands = demandIds.stream()
                .map(id -> new ReqTimetableAgentDemandDTO(id, 4)).toList();
        return new ReqCreateTimetableAgentProposalDTO(42L, 7L, List.of(11L),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31), demands,
                lockedIds, "", "Test request");
    }

    private void checkNoTimetableWrites() {
        Mockito.verify(entryRepository, Mockito.never()).save(ArgumentMatchers.any());
        Mockito.verify(entryRepository, Mockito.never()).saveAll(ArgumentMatchers.any());
        Mockito.verify(entryRepository, Mockito.never()).deleteAllById(ArgumentMatchers.any());
        Mockito.verify(revisionRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    private void expectRejectedWithoutTimetableWrites(org.junit.jupiter.api.function.Executable operation) {
        AppException exception = Assertions.assertThrows(AppException.class, operation,
                "invalid snapshot inputs must be rejected before model context or timetable writes");
        Assertions.assertEquals(422, exception.getStatus().value(),
                "snapshot policy violations must map to an unprocessable request");
        checkNoTimetableWrites();
    }

    private void checkScopedSnapshotPrivacyContract(TimetableAgentSnapshot snapshot) {
        Assertions.assertTrue(snapshot.targetRevisionId() == 42L && snapshot.expectedVersion() == 7L
                        && snapshot.classIds().equals(List.of(11L))
                        && snapshot.assignments().get(0).assignmentId() == 501L
                        && snapshot.assignments().get(0).periodsPerWeek() == 4
                        && snapshot.approvedAvailability().get(0).periodIndexes().equals(List.of(1, 3))
                        && !snapshot.snapshotJson().contains("PERSONAL_LEAVE_SECRET")
                        && !snapshot.snapshotJson().contains("APPROVER_REASON_SECRET"),
                "the snapshot must preserve approved scope data while omitting private reasons");
    }
}


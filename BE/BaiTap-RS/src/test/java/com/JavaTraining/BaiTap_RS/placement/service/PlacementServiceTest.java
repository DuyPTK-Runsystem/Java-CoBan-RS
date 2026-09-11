package com.JavaTraining.BaiTap_RS.placement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.service.EnrollmentService;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqConfirmPlacementDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqCreatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqPlacementActionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementResultDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementIssueSeverity;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResultStatus;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSessionStatus;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSourceType;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementCandidateRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementResultRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementSessionRepository;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementAllocationEngine;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementCandidateEligibilityValidator;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementCandidateSnapshotService;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementConfirmationSupport;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementConfirmationValidator;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementResponseMapper;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementScopeService;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementScopeValidator;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementSessionAccess;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementSessionRules;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementSimulationSupport;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentGender;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import com.JavaTraining.BaiTap_RS.student.repository.StudentInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlacementServiceTest {
    @Mock private PlacementSessionRepository sessions;
    @Mock private AcademicYearRepository academicYears;
    @Mock private GradeLevelRepository gradeLevels;
    @Mock private StudentAnnualTranscriptRepository annualTranscripts;
    @Mock private PlacementCandidateRepository candidates;
    @Mock private PlacementResultRepository results;
    @Mock private SchoolClassRepository classes;
    @Mock private StudentYearEnrollmentRepository enrollments;
    @Mock private EnrollmentService enrollmentService;
    @Mock private StudentInfoRepository studentInfos;
    @Mock private AuditLogRepository audits;

    private PlacementService service;
    private final List<PlacementResult> storedResults = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        PlacementScopeValidator scopeValidator = new PlacementScopeValidator(classes, enrollments);
        PlacementScopeService scopeService = new PlacementScopeService(scopeValidator, mapper);
        PlacementCandidateEligibilityValidator eligibilityValidator = new PlacementCandidateEligibilityValidator(
                gradeLevels, classes, enrollments);
        PlacementCandidateSnapshotService snapshots = new PlacementCandidateSnapshotService(academicYears,
                annualTranscripts, studentInfos, candidates, eligibilityValidator);
        PlacementResponseMapper responseMapper = new PlacementResponseMapper(scopeService);
        PlacementSessionAccess access = new PlacementSessionAccess(sessions, candidates, results, responseMapper);
        PlacementSessionRules rules = new PlacementSessionRules();
        service = new PlacementService(sessions, scopeService, snapshots,
                new PlacementConfirmationValidator(scopeService, candidates, enrollments), access, rules,
                new PlacementConfirmationSupport(sessions, enrollmentService, audits, access),
                new PlacementSimulationSupport(sessions, candidates, results, scopeService,
                        new PlacementAllocationEngine(), access, rules));
        org.mockito.Mockito.lenient().when(results.findAllBySessionIdOrderByStudentIdAsc(1L))
                .thenAnswer(invocation -> storedResults);
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            storedResults.clear();
            return null;
        }).when(results).deleteAllBySessionId(1L);
        org.mockito.Mockito.lenient().when(results.saveAll(any())).thenAnswer(invocation -> {
            Iterable<PlacementResult> values = invocation.getArgument(0);
            for (PlacementResult result : values) {
                ReflectionTestUtils.setField(result, "id", (long) storedResults.size() + 1);
                storedResults.add(result);
            }
            return values;
        });
        org.mockito.Mockito.lenient().when(sessions.save(any(PlacementSession.class))).thenAnswer(invocation -> {
            PlacementSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                ReflectionTestUtils.setField(session, "id", 1L);
            }
            return session;
        });
        org.mockito.Mockito.lenient().when(enrollments.countByCurrentClassIdAndStatus(
                any(Long.class), any(EnrollmentStatus.class))).thenReturn(0L);
    }

    @Test
    void createUsesOfficialPreviousYearTranscriptAndSnapshotsGender() {
        AcademicYear target = year(2L, LocalDate.of(2027, 8, 1), LocalDate.of(2028, 5, 31));
        AcademicYear previous = year(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2027, 5, 31));
        SchoolClass targetClass = schoolClass(10L, 2L, 8L, 30);
        StudentAnnualTranscript transcript = new StudentAnnualTranscript(20L, 1L);
        ReflectionTestUtils.setField(transcript, "id", 99L);
        transcript.setCalculationStatus(CalculationStatus.FINISH);
        transcript.setFinalDtbcn(new BigDecimal("8.5"));
        transcript.setSourceVersion(4L);
        transcript.setCalculatedVersion(4L);
        StudentInfo info = new StudentInfo(null, null, null, StudentGender.FEMALE);
        when(academicYears.findById(2L)).thenReturn(Optional.of(target));
        when(academicYears.findTopByEndDateLessThanOrderByEndDateDesc(target.getStartDate())).thenReturn(Optional.of(previous));
        when(classes.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(List.of(10L), 2L)).thenReturn(List.of(targetClass));
        when(annualTranscripts.findAllByAcademicYearIdAndStudentIdIn(1L, List.of(20L))).thenReturn(List.of(transcript));
        when(studentInfos.findByStudentId(20L)).thenReturn(Optional.of(info));
        when(enrollments.findByStudentIdAndAcademicYearId(20L, 1L)).thenReturn(Optional.of(
                new com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment(
                        20L, 1L, 50L, EnrollmentStatus.ACTIVE, java.time.LocalDateTime.now())));
        when(classes.findById(50L)).thenReturn(Optional.of(schoolClass(50L, 1L, 7L, 30)));
        GradeLevel previousGrade = new GradeLevel("G7", "Khối 7", 7, 7, 8L, true, null);
        ReflectionTestUtils.setField(previousGrade, "id", 7L);
        when(gradeLevels.findById(7L)).thenReturn(Optional.of(previousGrade));

        service.create(new ReqCreatePlacementSessionDTO(2L, 8L,
                List.of(new ReqCreatePlacementSessionDTO.TargetClass(10L, PlacementClassProfile.REGULAR, 30)),
                List.of(new ReqCreatePlacementSessionDTO.Candidate(20L, 8L, PlacementSourceType.CONTINUING,
                        new BigDecimal("1.0"), "caller-value", "MALE", null, null)), "P1-P6-v1"));

        ArgumentCaptor<PlacementCandidate> captor = ArgumentCaptor.forClass(PlacementCandidate.class);
        verify(candidates).save(captor.capture());
        assertEquals(new BigDecimal("8.5"), captor.getValue().getScore());
        assertEquals("student_annual_transcript:99:finalDtbcn:v4", captor.getValue().getScoreSourceReference());
        assertEquals("FEMALE", captor.getValue().getGenderSnapshot());
    }

    @Test
    void simulateAssignsTopScoreToAdvancedAndLeavesMissingGenderForManualPlacement() {
        PlacementSession session = sessionWithScope(1L, advancedAndRegularScope());
        PlacementCandidate top = candidate(1L, new BigDecimal("9.5"), "MALE");
        PlacementCandidate missingGender = candidate(2L, new BigDecimal("8.5"), null);
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(classes.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(List.of(10L, 11L), 2L))
                .thenReturn(List.of(schoolClass(10L, 2L, 8L, 1), schoolClass(11L, 2L, 8L, 1)));
        when(candidates.findAllBySessionIdOrderByStudentIdAsc(1L)).thenReturn(List.of(top, missingGender));

        ResPlacementSessionDTO response = service.simulate(1L, new ReqPlacementActionDTO(0L, null));

        assertEquals(PlacementSessionStatus.READY_FOR_CONFIRM, response.status());
        PlacementResult auto = storedResults.stream().filter(r -> r.getStudentId().equals(1L)).findFirst().orElseThrow();
        PlacementResult manual = storedResults.stream().filter(r -> r.getStudentId().equals(2L)).findFirst().orElseThrow();
        assertEquals(PlacementResultStatus.AUTO_ASSIGNED, auto.getResultStatus());
        assertEquals(10L, auto.getTargetClassId());
        assertEquals(PlacementResultStatus.MANUAL_REQUIRED, manual.getResultStatus());
        assertEquals("MISSING_DATA", manual.getIssueCode());
        assertEquals(PlacementIssueSeverity.WARNING, manual.getIssueSeverity());
    }

    @Test
    void simulateMarksEqualScoreAtAdvancedBoundaryForManualPlacement() {
        PlacementSession session = sessionWithScope(1L, advancedAndRegularScope());
        PlacementCandidate first = candidate(1L, new BigDecimal("9.0"), "MALE");
        PlacementCandidate tied = candidate(2L, new BigDecimal("9.0"), "FEMALE");
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(classes.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(List.of(10L, 11L), 2L))
                .thenReturn(List.of(schoolClass(10L, 2L, 8L, 1), schoolClass(11L, 2L, 8L, 1)));
        when(candidates.findAllBySessionIdOrderByStudentIdAsc(1L)).thenReturn(List.of(first, tied));

        service.simulate(1L, new ReqPlacementActionDTO(0L, null));

        assertTrue(storedResults.stream().anyMatch(r -> r.getStudentId().equals(1L)
                && r.getResultStatus() == PlacementResultStatus.MANUAL_REQUIRED
                && "SCORE_TIE".equals(r.getIssueCode())));
        assertTrue(storedResults.stream().anyMatch(r -> r.getStudentId().equals(2L)
                && r.getResultStatus() == PlacementResultStatus.MANUAL_REQUIRED
                && "SCORE_TIE".equals(r.getIssueCode())));
    }

    @Test
    void simulateUsesHardCapacityForAutomaticResults() {
        PlacementSession session = sessionWithScope(1L, regularScope());
        PlacementCandidate first = candidate(1L, new BigDecimal("8.0"), "MALE");
        PlacementCandidate second = candidate(2L, new BigDecimal("7.0"), "FEMALE");
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(classes.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(List.of(10L), 2L))
                .thenReturn(List.of(schoolClass(10L, 2L, 8L, 1)));
        when(candidates.findAllBySessionIdOrderByStudentIdAsc(1L)).thenReturn(List.of(first, second));

        ResPlacementSessionDTO response = service.simulate(1L, new ReqPlacementActionDTO(0L, null));

        assertEquals(PlacementSessionStatus.SIMULATED, response.status());
        assertTrue(storedResults.stream().anyMatch(r -> r.getIssueSeverity() == PlacementIssueSeverity.BLOCKING
                && "CAPACITY_EXCEEDED".equals(r.getIssueCode())));
        assertEquals(1, storedResults.stream().filter(r -> r.getResultStatus() == PlacementResultStatus.AUTO_ASSIGNED).count());
    }

    @Test
    void confirmReplaysOnlyWhenIdempotencyKeyBelongsToSameSession() {
        PlacementSession session = sessionWithScope(1L, regularScope());
        ReflectionTestUtils.setField(session, "status", PlacementSessionStatus.CONFIRMED);
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(sessions.findByConfirmIdempotencyKey("confirm-1")).thenReturn(Optional.of(session));

        ResPlacementSessionDTO response = service.confirm(1L, new ReqConfirmPlacementDTO(0L, "confirm-1"));

        assertEquals(PlacementSessionStatus.CONFIRMED, response.status());
        verifyNoInteractions(enrollmentService);
    }

    @Test
    void confirmRejectsIdempotencyKeyBelongingToAnotherSession() {
        PlacementSession requested = sessionWithScope(1L, regularScope());
        PlacementSession owner = sessionWithScope(2L, regularScope());
        ReflectionTestUtils.setField(owner, "status", PlacementSessionStatus.CONFIRMED);
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(requested));
        when(sessions.findByConfirmIdempotencyKey("confirm-1")).thenReturn(Optional.of(owner));

        AppException exception = assertThrows(AppException.class,
                () -> service.confirm(1L, new ReqConfirmPlacementDTO(0L, "confirm-1")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verifyNoInteractions(enrollmentService);
    }

    @Test
    void confirmRequiresIdempotencyKeyBeforeLoadingSession() {
        AppException exception = assertThrows(AppException.class,
                () -> service.confirm(1L, new ReqConfirmPlacementDTO(0L, null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(sessions, results, enrollmentService, audits);
    }

    @Test
    void simulateRejectsStaleOptimisticLockVersionBeforeDeletingResults() {
        PlacementSession session = sessionWithScope(1L, regularScope());
        ReflectionTestUtils.setField(session, "version", 3L);
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));

        AppException exception = assertThrows(AppException.class,
                () -> service.simulate(1L, new ReqPlacementActionDTO(2L, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(results, never()).deleteAllBySessionId(1L);
        verifyNoInteractions(enrollmentService, audits);
    }

    @Test
    void confirmRejectsStaleOptimisticLockVersionAfterReplayLookup() {
        PlacementSession session = sessionWithScope(1L, regularScope());
        ReflectionTestUtils.setField(session, "version", 3L);
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(sessions.findByConfirmIdempotencyKey("confirm-stale")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> service.confirm(1L, new ReqConfirmPlacementDTO(2L, "confirm-stale")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(results, never()).findAllBySessionIdOrderByStudentIdAsc(1L);
        verifyNoInteractions(enrollmentService, audits);
    }

    @Test
    void cancelChangesDraftSessionStatusAndAuditsTheMutation() {
        PlacementSession session = sessionWithScope(1L, regularScope());
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(candidates.findAllBySessionIdOrderByStudentIdAsc(1L)).thenReturn(List.of());

        ResPlacementSessionDTO response = service.cancel(1L, new ReqPlacementActionDTO(0L, null));

        assertEquals(PlacementSessionStatus.CANCELLED, response.status());
        verify(sessions).save(session);
        verify(audits).save(any());
        verify(results).findAllBySessionIdOrderByStudentIdAsc(1L);
    }

    @Test
    void getSessionDoesNotLoadResultsThatHaveTheirOwnPaginationRoute() {
        PlacementSession session = sessionWithScope(1L, regularScope());
        when(sessions.findById(1L)).thenReturn(Optional.of(session));

        ResPlacementSessionDTO response = service.get(1L);

        assertTrue(response.results().isEmpty());
        verify(candidates).findAllBySessionIdOrderByStudentIdAsc(1L);
        verify(results, never())
                .findAllBySessionIdOrderByStudentIdAsc(1L);
    }

    @Test
    void resultsRouteUsesPagedRepositoryQuery() {
        PlacementSession session = sessionWithScope(1L, regularScope());
        PlacementResult result = new PlacementResult(1L, 20L, 10L, PlacementResultStatus.AUTO_ASSIGNED,
                new BigDecimal("8.0"), null, null, "assigned");
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
        when(results.findBySessionIdOrderByStudentIdAsc(1L, PageRequest.of(0, 2)))
                .thenReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 2), 1));

        ResultPaginationDTO<ResPlacementResultDTO> response = service.getResults(1L, PageRequest.of(0, 2));

        assertEquals(1, response.result().size(), "paged route should map returned result content");
        assertEquals(1, response.meta().totalItems(), "paged route should preserve total item count");
        verify(results).findBySessionIdOrderByStudentIdAsc(1L, PageRequest.of(0, 2));
        verify(results, never())
                .findAllBySessionIdOrderByStudentIdAsc(1L);
    }

    private PlacementCandidate candidate(Long studentId, BigDecimal score, String gender) {
        return new PlacementCandidate(1L, studentId, 8L, PlacementSourceType.CONTINUING,
                score, "transcript", gender, null, null);
    }

    private PlacementSession sessionWithScope(Long id, String scope) {
        PlacementSession session = new PlacementSession(2L, 8L, "P1-P6-v1", scope, 1L);
        ReflectionTestUtils.setField(session, "id", id);
        ReflectionTestUtils.setField(session, "version", 0L);
        return session;
    }

    private String advancedAndRegularScope() {
        return "[{\"classId\":10,\"classCode\":\"8A\",\"className\":\"8A\","
                + "\"profile\":\"ADVANCED\",\"capacity\":1},{\"classId\":11,"
                + "\"classCode\":\"8B\",\"className\":\"8B\",\"profile\":\"REGULAR\","
                + "\"capacity\":1}]";
    }

    private String regularScope() {
        return "[{\"classId\":10,\"classCode\":\"8A\",\"className\":\"8A\","
                + "\"profile\":\"REGULAR\",\"capacity\":1}]";
    }

    private AcademicYear year(Long id, LocalDate start, LocalDate end) {
        AcademicYear result = new AcademicYear("Y" + id, start, end, AcademicYearStatus.ACTIVE, null);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private SchoolClass schoolClass(Long id, Long yearId, Long gradeId, Integer capacity) {
        SchoolClass result = new SchoolClass(yearId, gradeId, "8" + id, "8" + id, capacity, SchoolClassStatus.ACTIVE);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}

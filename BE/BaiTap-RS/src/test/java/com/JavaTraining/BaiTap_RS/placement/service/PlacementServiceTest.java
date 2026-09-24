package com.JavaTraining.BaiTap_RS.placement.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqCreateEnrollmentDTO;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlacementServiceTest {
    private static final String GENDER_MALE = "MALE";
    private static final BigDecimal SCORE_EIGHT = new BigDecimal("8.0");
    private static final String PLACEMENT_ENTITY = "placement_session";

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
    private PlacementSessionAccess access;
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
        access = new PlacementSessionAccess(sessions, candidates, results, responseMapper);
        PlacementSessionRules rules = new PlacementSessionRules();
        service = new PlacementService(sessions, scopeService, snapshots,
                new PlacementConfirmationValidator(scopeService, candidates, enrollments), access, rules,
                new PlacementConfirmationSupport(sessions, enrollmentService, audits, access, mapper),
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
    void createUsesOfficialPreviousYearTranscriptAndSnapshotsGender() throws JsonProcessingException {
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
                        new BigDecimal("1.0"), "caller-value", GENDER_MALE, null, null)), "P1-P6-v1"));

        ArgumentCaptor<PlacementCandidate> captor = ArgumentCaptor.forClass(PlacementCandidate.class);
        verify(candidates).save(captor.capture());
        assertEquals(new BigDecimal("8.5"), captor.getValue().getScore());
        assertEquals("student_annual_transcript:99:finalDtbcn:v4", captor.getValue().getScoreSourceReference());
        assertEquals("FEMALE", captor.getValue().getGenderSnapshot());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audits).save(auditCaptor.capture());
        checkPlacementAudit(auditCaptor.getValue(), "PLACEMENT_SESSION_CREATED", "classes", 1);
    }

    @Test
    void simulateAssignsTopScoreToAdvancedAndLeavesMissingGenderForManualPlacement() {
        PlacementSession session = sessionWithScope(1L, advancedAndRegularScope());
        PlacementCandidate top = candidate(1L, new BigDecimal("9.5"), GENDER_MALE);
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
        assertNull(auto.getIssueCode());
        assertNull(auto.getIssueSeverity());
        assertEquals("Lớp chọn", auto.getExplanation());
        assertEquals(PlacementResultStatus.MANUAL_REQUIRED, manual.getResultStatus());
        assertEquals("MISSING_DATA", manual.getIssueCode());
        assertEquals(PlacementIssueSeverity.WARNING, manual.getIssueSeverity());
    }

    @Test
    void simulateMarksEqualScoreAtAdvancedBoundaryForManualPlacement() {
        PlacementSession session = sessionWithScope(1L, advancedAndRegularScope());
        PlacementCandidate first = candidate(1L, new BigDecimal("9.0"), GENDER_MALE);
        PlacementCandidate tied = candidate(2L, new BigDecimal("9.0"), "FEMALE");
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(classes.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(List.of(10L, 11L), 2L))
                .thenReturn(List.of(schoolClass(10L, 2L, 8L, 1), schoolClass(11L, 2L, 8L, 1)));
        when(candidates.findAllBySessionIdOrderByStudentIdAsc(1L)).thenReturn(List.of(first, tied));

        service.simulate(1L, new ReqPlacementActionDTO(0L, null));

        List<PlacementResult> tiedResults = storedResults.stream()
                .filter(r -> "SCORE_TIE".equals(r.getIssueCode())).toList();
        assertEquals(2, tiedResults.size());
        assertTrue(tiedResults.stream().allMatch(r -> r.getResultStatus() == PlacementResultStatus.MANUAL_REQUIRED
                && r.getIssueSeverity() == PlacementIssueSeverity.WARNING
                && r.getTargetClassId() == null
                && "Có nhiều học sinh bằng điểm tại ranh giới chỉ tiêu; giáo vụ cần xếp lớp thủ công."
                        .equals(r.getExplanation())));
    }

    @Test
    void simulateLeavesCapacityOverflowAsManualWarningWithoutBlockingAutomaticResults() {
        PlacementSession session = sessionWithScope(1L, regularScope());
        PlacementCandidate first = candidate(1L, SCORE_EIGHT, GENDER_MALE);
        PlacementCandidate second = candidate(2L, new BigDecimal("7.0"), "FEMALE");
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(classes.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(List.of(10L), 2L))
                .thenReturn(List.of(schoolClass(10L, 2L, 8L, 1)));
        when(candidates.findAllBySessionIdOrderByStudentIdAsc(1L)).thenReturn(List.of(first, second));

        ResPlacementSessionDTO response = service.simulate(1L, new ReqPlacementActionDTO(0L, null));

        assertEquals(PlacementSessionStatus.READY_FOR_CONFIRM, response.status());
        PlacementResult overflow = storedResults.stream()
                .filter(r -> "CAPACITY_EXCEEDED".equals(r.getIssueCode())).findFirst().orElseThrow();
        assertEquals(PlacementResultStatus.MANUAL_REQUIRED, overflow.getResultStatus());
        assertEquals(PlacementIssueSeverity.WARNING, overflow.getIssueSeverity());
        assertNull(overflow.getTargetClassId());
        PlacementResult automatic = storedResults.stream()
                .filter(r -> r.getResultStatus() == PlacementResultStatus.AUTO_ASSIGNED).findFirst().orElseThrow();
        assertEquals(1, storedResults.stream().filter(r -> r.getResultStatus() == PlacementResultStatus.AUTO_ASSIGNED).count());
        assertEquals(10L, automatic.getTargetClassId());
        assertNull(automatic.getIssueCode());
        assertNull(automatic.getIssueSeverity());
        assertEquals("Phân bổ cân bằng theo điểm học tập và tỷ lệ nam, nữ.", automatic.getExplanation());
    }

    @Test
    void confirmCreatesOnlyAutomaticEnrollmentWhenOverflowAndTieRemainManual() {
        PlacementSession session = sessionWithScope(1L, fourRegularClassesScope());
        ReflectionTestUtils.setField(session, "status", PlacementSessionStatus.READY_FOR_CONFIRM);
        PlacementCandidate automatic = candidate(1L, new BigDecimal("9.5"), GENDER_MALE);
        PlacementCandidate overflow = candidate(2L, new BigDecimal("8.5"), "FEMALE");
        PlacementCandidate tie = candidate(3L, new BigDecimal("8.5"), GENDER_MALE);
        PlacementResult automaticResult = new PlacementResult(1L, 1L, 10L,
                PlacementResultStatus.AUTO_ASSIGNED, automatic.getScore(), null, null, "assigned");
        PlacementResult overflowResult = new PlacementResult(1L, 2L, null,
                PlacementResultStatus.MANUAL_REQUIRED, overflow.getScore(), "CAPACITY_EXCEEDED",
                PlacementIssueSeverity.WARNING, "overflow");
        PlacementResult tieResult = new PlacementResult(1L, 3L, null,
                PlacementResultStatus.MANUAL_REQUIRED, tie.getScore(), "SCORE_TIE",
                PlacementIssueSeverity.WARNING, "tie");
        storedResults.addAll(List.of(automaticResult, overflowResult, tieResult));
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(sessions.findByConfirmIdempotencyKey("confirm-overflow")).thenReturn(Optional.empty());
        when(candidates.findAllBySessionIdOrderByStudentIdAsc(1L))
                .thenReturn(List.of(automatic, overflow, tie));
        when(classes.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(
                List.of(10L, 11L, 12L, 13L), 2L)).thenReturn(List.of(
                        schoolClass(10L, 2L, 8L, 7), schoolClass(11L, 2L, 8L, 10),
                        schoolClass(12L, 2L, 8L, 10), schoolClass(13L, 2L, 8L, 10)));

        ResPlacementSessionDTO response = service.confirm(1L,
                new ReqConfirmPlacementDTO(0L, "confirm-overflow"));

        assertEquals(PlacementSessionStatus.CONFIRMED, response.status());
        ArgumentCaptor<ReqCreateEnrollmentDTO> enrollmentCaptor =
                ArgumentCaptor.forClass(ReqCreateEnrollmentDTO.class);
        verify(enrollmentService).createEnrollment(enrollmentCaptor.capture());
        assertEquals(1L, enrollmentCaptor.getValue().studentId());
        assertEquals(10L, enrollmentCaptor.getValue().classId());
        verify(enrollmentService, org.mockito.Mockito.times(1)).createEnrollment(any());
        assertTrue(storedResults.stream().filter(r -> r.getResultStatus() == PlacementResultStatus.MANUAL_REQUIRED)
                .allMatch(r -> r.getTargetClassId() == null));
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
    void confirmAuditsResultCountAsJson() throws JsonProcessingException {
        PlacementSession session = sessionWithScope(1L, regularScope());
        ReflectionTestUtils.setField(session, "status", PlacementSessionStatus.READY_FOR_CONFIRM);
        PlacementCandidate placementCandidate = candidate(1L, SCORE_EIGHT, GENDER_MALE);
        PlacementResult result = new PlacementResult(1L, 1L, 10L, PlacementResultStatus.AUTO_ASSIGNED,
                SCORE_EIGHT, null, null, "assigned");
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(sessions.findByConfirmIdempotencyKey("confirm-json")).thenReturn(Optional.empty());
        when(candidates.findAllBySessionIdOrderByStudentIdAsc(1L)).thenReturn(List.of(placementCandidate));
        when(classes.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(List.of(10L), 2L))
                .thenReturn(List.of(schoolClass(10L, 2L, 8L, 1)));
        storedResults.add(result);

        service.confirm(1L, new ReqConfirmPlacementDTO(0L, "confirm-json"));

        checkConfirmedAudit();
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
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audits).save(auditCaptor.capture());
        checkCancelledPlacementAudit(auditCaptor.getValue());
        verify(results).findAllBySessionIdOrderByStudentIdAsc(1L);
    }

    @Test
    void auditSerializationFailurePropagatesWithoutSavingAudit() throws JsonProcessingException {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("forced failure") { });
        PlacementConfirmationSupport support = new PlacementConfirmationSupport(sessions, enrollmentService, audits,
                access, failingMapper);

        checkSerializationFailure(support);
    }

    @Test
    void getSessionDoesNotLoadResultsThatHaveTheirOwnPaginationRoute() {
        PlacementSession session = sessionWithScope(1L, advancedAndRegularScope());
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
        when(enrollments.countRosterAt(org.mockito.ArgumentMatchers.eq(10L), any(LocalDateTime.class)))
                .thenReturn(28L);
        when(enrollments.countRosterAt(org.mockito.ArgumentMatchers.eq(11L), any(LocalDateTime.class)))
                .thenReturn(17L);

        ResPlacementSessionDTO response = service.get(1L);

        assertTrue(response.results().isEmpty());
        assertEquals(List.of(10L, 11L), response.targetClasses().stream()
                .map(target -> target.classId()).toList());
        assertEquals(List.of(28L, 17L), response.targetClasses().stream()
                .map(target -> target.currentStudentCount()).toList());
        verify(enrollments).countRosterAt(org.mockito.ArgumentMatchers.eq(10L), any(LocalDateTime.class));
        verify(enrollments).countRosterAt(org.mockito.ArgumentMatchers.eq(11L), any(LocalDateTime.class));
        verify(candidates).findAllBySessionIdOrderByStudentIdAsc(1L);
        verify(results, never())
                .findAllBySessionIdOrderByStudentIdAsc(1L);
    }

    @Test
    void resultsRouteUsesPagedRepositoryQuery() {
        PlacementSession session = sessionWithScope(1L, regularScope());
        PlacementResult result = new PlacementResult(1L, 20L, 10L, PlacementResultStatus.AUTO_ASSIGNED,
                SCORE_EIGHT, null, null, "Phân bổ cân bằng theo điểm học tập và tỷ lệ nam, nữ.");
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
        PageRequest stablePage = PageRequest.of(0, 2,
                Sort.by(Sort.Order.asc("studentId"), Sort.Order.asc("id")));
        when(results.findBySessionId(1L, stablePage))
                .thenReturn(new PageImpl<>(List.of(result), stablePage, 1));

        ResultPaginationDTO<ResPlacementResultDTO> response = service.getResults(1L,
                PageRequest.of(0, 2, Sort.by(Sort.Order.desc("score"))));

        assertEquals(1, response.result().size(), "paged route should map returned result content");
        assertEquals(1, response.meta().totalItems(), "paged route should preserve total item count");
        assertEquals("Phân bổ cân bằng theo điểm học tập và tỷ lệ nam, nữ.",
                response.result().get(0).explanation(), "results response must preserve the REGULAR explanation");
        verify(results).findBySessionId(1L, stablePage);
        verify(results, never())
                .findAllBySessionIdOrderByStudentIdAsc(1L);
    }

    private void checkConfirmedAudit() throws JsonProcessingException {
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audits).save(auditCaptor.capture());
        checkPlacementAudit(auditCaptor.getValue(), "PLACEMENT_SESSION_CONFIRMED", "results", 1);
    }

    private void checkPlacementAudit(AuditLog audit, String expectedAction, String expectedCountField,
            int expectedCount) throws JsonProcessingException {
        JsonNode afterData = new ObjectMapper().readTree(audit.getAfterData());
        assertAll("placement audit fields",
                () -> assertEquals(expectedAction, audit.getAction(), "audit action should be preserved"),
                () -> assertEquals(PLACEMENT_ENTITY, audit.getEntityType(), "audit entity type should be preserved"),
                () -> assertEquals("1", audit.getEntityId(), "audit entity id should be preserved"),
                () -> assertEquals(expectedCount, afterData.get(expectedCountField).asInt(),
                        "audit count should be serialized as JSON"));
    }

    private void checkCancelledPlacementAudit(AuditLog audit) {
        assertAll("cancelled placement audit fields",
                () -> assertEquals("PLACEMENT_SESSION_CANCELLED", audit.getAction(),
                        "cancel audit action should be preserved"),
                () -> assertEquals(PLACEMENT_ENTITY, audit.getEntityType(),
                        "cancel audit entity type should be preserved"),
                () -> assertEquals("1", audit.getEntityId(), "cancel audit entity id should be preserved"),
                () -> assertNull(audit.getAfterData(), "cancel audit after_data should remain null"));
    }

    private void checkSerializationFailure(PlacementConfirmationSupport support) {
        assertAll("audit serialization failure",
                () -> {
                    AppException exception = assertThrows(AppException.class,
                            () -> support.audit("PLACEMENT_SESSION_CREATED", 1L, Map.of("classes", 1)),
                            "audit serialization failure should propagate as an application error");
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus(),
                            "audit serialization failure should use server error status");
                });
        verify(audits, never()).save(any());
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

    private String fourRegularClassesScope() {
        return "[{\"classId\":10,\"classCode\":\"7A1\",\"className\":\"7A1\","
                + "\"profile\":\"REGULAR\",\"capacity\":7},{\"classId\":11,"
                + "\"classCode\":\"7A2\",\"className\":\"7A2\",\"profile\":\"REGULAR\","
                + "\"capacity\":10},{\"classId\":12,\"classCode\":\"7A3\","
                + "\"className\":\"7A3\",\"profile\":\"REGULAR\",\"capacity\":10},"
                + "{\"classId\":13,\"classCode\":\"7A4\",\"className\":\"7A4\","
                + "\"profile\":\"REGULAR\",\"capacity\":10}]";
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

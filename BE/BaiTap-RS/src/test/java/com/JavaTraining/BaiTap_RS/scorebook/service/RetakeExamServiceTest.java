package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpdateRetakeScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExam;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExamStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.repository.RetakeExamRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "PMD.UnitTestContainsTooManyAsserts",
        "PMD.TooManyMethods"
})
class RetakeExamServiceTest {

    private static final Long STUDENT_ID = 100L;
    private static final Long ACADEMIC_YEAR_ID = 10L;
    private static final Long SUBJECT_ID = 20L;
    private static final Long ANNUAL_TRANSCRIPT_ID = 300L;
    private static final Long RETAKE_ID = 500L;
    private static final String ROLE_OFFICE = "ACADEMIC_OFFICE";
    private static final BigDecimal DEFAULT_PRE_SCORE = new BigDecimal("4.5");
    private static final String MSG_RETAKE_ID_MATCH = "Retake ID should match";

    @Mock
    private RetakeExamRepository retakeExamRepository;
    @Mock
    private StudentAnnualTranscriptRepository annualTranscriptRepository;
    @Mock
    private StudentSubjectAnnualResultRepository annualResultRepository;
    @Mock
    private TranscriptStateService transcriptStateService;
    @Mock
    private CalculationTaskService calculationTaskService;
    @Mock
    private ScorebookAuditService auditService;

    private RetakeExamService retakeExamService;

    @BeforeEach
    void setUp() {
        retakeExamService = new RetakeExamService(
                retakeExamRepository,
                annualTranscriptRepository,
                annualResultRepository,
                transcriptStateService,
                calculationTaskService,
                auditService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRetakeExamPlannedSuccess() {
        authenticate(200L, ROLE_OFFICE);
        StudentAnnualTranscript transcript = new StudentAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
        ReflectionTestUtils.setField(transcript, "id", ANNUAL_TRANSCRIPT_ID);

        StudentSubjectAnnualResult subjectResult = new StudentSubjectAnnualResult(
                ANNUAL_TRANSCRIPT_ID, SUBJECT_ID, SubjectType.ACADEMIC);
        subjectResult.setRegularDtbmhCn(DEFAULT_PRE_SCORE);

        ReqCreateRetakeExamDTO req = new ReqCreateRetakeExamDTO(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, LocalDate.of(2026, 8, 30), null, "Kế hoạch thi lại");

        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.of(transcript));
        Mockito.when(annualResultRepository.findByAnnualTranscriptIdAndSubjectId(ANNUAL_TRANSCRIPT_ID, SUBJECT_ID))
                .thenReturn(Optional.of(subjectResult));
        Mockito.when(retakeExamRepository.findByStudentIdAndAcademicYearIdAndSubjectId(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID)).thenReturn(Optional.empty());
        Mockito.when(retakeExamRepository.save(Mockito.any(RetakeExam.class)))
                .thenAnswer(inv -> {
                    RetakeExam saved = inv.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", RETAKE_ID);
                    return saved;
                });

        ResRetakeExamDTO res = retakeExamService.createRetakeExam(req);

        Assertions.assertEquals(RETAKE_ID, res.retakeId(), MSG_RETAKE_ID_MATCH);
        Assertions.assertEquals(DEFAULT_PRE_SCORE, res.preRetakeScore(), "Pre-retake score should match");
        Assertions.assertNull(res.retakeScore(), "Retake score should be null for PLANNED");
        Assertions.assertEquals(RetakeExamStatus.PLANNED, res.status(), "Status should be PLANNED");
        Mockito.verify(auditService).writeAudit(
                Mockito.eq("RETAKE_EXAM_CREATED"), Mockito.eq("retake_exam"),
                Mockito.eq(RETAKE_ID), Mockito.isNull(), Mockito.anyMap());
        Mockito.verifyNoInteractions(transcriptStateService, calculationTaskService);
    }

    @Test
    void createRetakeExamScoredSuccessTouchesTranscriptAndTask() {
        authenticate(200L, ROLE_OFFICE);
        StudentAnnualTranscript transcript = new StudentAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
        ReflectionTestUtils.setField(transcript, "id", ANNUAL_TRANSCRIPT_ID);

        StudentSubjectAnnualResult subjectResult = new StudentSubjectAnnualResult(
                ANNUAL_TRANSCRIPT_ID, SUBJECT_ID, SubjectType.ACADEMIC);
        subjectResult.setRegularDtbmhCn(DEFAULT_PRE_SCORE);

        ReqCreateRetakeExamDTO req = new ReqCreateRetakeExamDTO(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, LocalDate.of(2026, 8, 30),
                new BigDecimal("6.5"), "Nhập điểm trực tiếp");

        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.of(transcript));
        Mockito.when(annualResultRepository.findByAnnualTranscriptIdAndSubjectId(ANNUAL_TRANSCRIPT_ID, SUBJECT_ID))
                .thenReturn(Optional.of(subjectResult));
        Mockito.when(retakeExamRepository.findByStudentIdAndAcademicYearIdAndSubjectId(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID)).thenReturn(Optional.empty());
        Mockito.when(retakeExamRepository.save(Mockito.any(RetakeExam.class)))
                .thenAnswer(inv -> {
                    RetakeExam saved = inv.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", RETAKE_ID);
                    return saved;
                });
        Mockito.when(transcriptStateService.touchAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID)).thenReturn(4L);

        ResRetakeExamDTO res = retakeExamService.createRetakeExam(req);

        Assertions.assertEquals(RETAKE_ID, res.retakeId(), MSG_RETAKE_ID_MATCH);
        Assertions.assertEquals(DEFAULT_PRE_SCORE, res.preRetakeScore(), "Pre-retake score should match");
        Assertions.assertEquals(new BigDecimal("6.5"), res.retakeScore(), "Retake score should match");
        Assertions.assertEquals(RetakeExamStatus.SCORED, res.status(), "Status should be SCORED");
        Mockito.verify(transcriptStateService).touchAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
        Mockito.verify(calculationTaskService).ensureRecalcTask(STUDENT_ID, ACADEMIC_YEAR_ID, 4L);
    }

    @Test
    void createRetakeExamMissingTranscriptThrowsNotFound() {
        ReqCreateRetakeExamDTO req = new ReqCreateRetakeExamDTO(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, null, null, null);
        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                AppException.class,
                () -> retakeExamService.createRetakeExam(req),
                "Should throw AppException when annual transcript is not found");
    }

    @Test
    void createRetakeExamMissingRegularScoreThrowsConflict() {
        StudentAnnualTranscript transcript = new StudentAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
        ReflectionTestUtils.setField(transcript, "id", ANNUAL_TRANSCRIPT_ID);
        StudentSubjectAnnualResult subjectResult = new StudentSubjectAnnualResult(
                ANNUAL_TRANSCRIPT_ID, SUBJECT_ID, SubjectType.ACADEMIC);
        subjectResult.setRegularDtbmhCn(null);

        ReqCreateRetakeExamDTO req = new ReqCreateRetakeExamDTO(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, null, null, null);

        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.of(transcript));
        Mockito.when(annualResultRepository.findByAnnualTranscriptIdAndSubjectId(ANNUAL_TRANSCRIPT_ID, SUBJECT_ID))
                .thenReturn(Optional.of(subjectResult));

        Assertions.assertThrows(
                AppException.class,
                () -> retakeExamService.createRetakeExam(req),
                "Should throw AppException when regularDtbmhCn is null");
    }

    @Test
    void createRetakeExamAlreadyExistsThrowsConflict() {
        StudentAnnualTranscript transcript = new StudentAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
        ReflectionTestUtils.setField(transcript, "id", ANNUAL_TRANSCRIPT_ID);
        StudentSubjectAnnualResult subjectResult = new StudentSubjectAnnualResult(
                ANNUAL_TRANSCRIPT_ID, SUBJECT_ID, SubjectType.ACADEMIC);
        subjectResult.setRegularDtbmhCn(new BigDecimal("4.0"));

        ReqCreateRetakeExamDTO req = new ReqCreateRetakeExamDTO(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, null, null, null);
        RetakeExam existing = new RetakeExam(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, new BigDecimal("4.0"), RetakeExamStatus.PLANNED);

        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.of(transcript));
        Mockito.when(annualResultRepository.findByAnnualTranscriptIdAndSubjectId(ANNUAL_TRANSCRIPT_ID, SUBJECT_ID))
                .thenReturn(Optional.of(subjectResult));
        Mockito.when(retakeExamRepository.findByStudentIdAndAcademicYearIdAndSubjectId(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID)).thenReturn(Optional.of(existing));

        Assertions.assertThrows(
                AppException.class,
                () -> retakeExamService.createRetakeExam(req),
                "Should throw AppException when retake already exists");
    }

    @Test
    void updateRetakeScoreSuccessTriggersRecalculation() {
        authenticate(200L, ROLE_OFFICE);
        RetakeExam exam = new RetakeExam(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, DEFAULT_PRE_SCORE, RetakeExamStatus.PLANNED);
        ReflectionTestUtils.setField(exam, "id", RETAKE_ID);

        ReqUpdateRetakeScoreDTO req = new ReqUpdateRetakeScoreDTO(
                new BigDecimal("7.0"), LocalDate.of(2026, 9, 1), "Cập nhật điểm thi lại");

        Mockito.when(retakeExamRepository.findById(RETAKE_ID)).thenReturn(Optional.of(exam));
        Mockito.when(retakeExamRepository.save(exam)).thenReturn(exam);
        Mockito.when(transcriptStateService.touchAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID)).thenReturn(5L);

        ResRetakeExamDTO res = retakeExamService.updateRetakeScore(RETAKE_ID, req);

        Assertions.assertEquals(RetakeExamStatus.SCORED, res.status(), "Status should be updated to SCORED");
        Assertions.assertEquals(new BigDecimal("7.0"), res.retakeScore(), "Retake score should match");
        Mockito.verify(transcriptStateService).touchAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
        Mockito.verify(calculationTaskService).ensureRecalcTask(STUDENT_ID, ACADEMIC_YEAR_ID, 5L);
        Mockito.verify(auditService).writeAudit(
                Mockito.eq("RETAKE_EXAM_SCORE_UPDATED"), Mockito.eq("retake_exam"),
                Mockito.eq(RETAKE_ID), Mockito.anyMap(), Mockito.anyMap());
    }

    @Test
    void updateRetakeScoreCancelledExamThrowsConflict() {
        RetakeExam exam = new RetakeExam(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, DEFAULT_PRE_SCORE, RetakeExamStatus.CANCELLED);
        ReflectionTestUtils.setField(exam, "id", RETAKE_ID);

        ReqUpdateRetakeScoreDTO req = new ReqUpdateRetakeScoreDTO(
                new BigDecimal("7.0"), null, null);

        Mockito.when(retakeExamRepository.findById(RETAKE_ID)).thenReturn(Optional.of(exam));

        Assertions.assertThrows(
                AppException.class,
                () -> retakeExamService.updateRetakeScore(RETAKE_ID, req),
                "Should throw AppException when updating cancelled retake exam");
    }

    @Test
    void cancelRetakeExamWasScoredTriggersRecalculation() {
        authenticate(200L, ROLE_OFFICE);
        RetakeExam exam = new RetakeExam(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, DEFAULT_PRE_SCORE, RetakeExamStatus.SCORED);
        ReflectionTestUtils.setField(exam, "id", RETAKE_ID);
        exam.setRetakeScore(new BigDecimal("8.0"));

        Mockito.when(retakeExamRepository.findById(RETAKE_ID)).thenReturn(Optional.of(exam));
        Mockito.when(retakeExamRepository.save(exam)).thenReturn(exam);
        Mockito.when(transcriptStateService.touchAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID)).thenReturn(6L);

        ResRetakeExamDTO res = retakeExamService.cancelRetakeExam(RETAKE_ID);

        Assertions.assertEquals(RetakeExamStatus.CANCELLED, res.status(), "Status should be CANCELLED");
        Mockito.verify(transcriptStateService).touchAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
        Mockito.verify(calculationTaskService).ensureRecalcTask(STUDENT_ID, ACADEMIC_YEAR_ID, 6L);
        Mockito.verify(auditService).writeAudit(
                Mockito.eq("RETAKE_EXAM_CANCELLED"), Mockito.eq("retake_exam"),
                Mockito.eq(RETAKE_ID), Mockito.anyMap(), Mockito.anyMap());
    }

    @Test
    void cancelRetakeExamWasPlannedDoesNotTriggerRecalculation() {
        authenticate(200L, ROLE_OFFICE);
        RetakeExam exam = new RetakeExam(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, DEFAULT_PRE_SCORE, RetakeExamStatus.PLANNED);
        ReflectionTestUtils.setField(exam, "id", RETAKE_ID);

        Mockito.when(retakeExamRepository.findById(RETAKE_ID)).thenReturn(Optional.of(exam));
        Mockito.when(retakeExamRepository.save(exam)).thenReturn(exam);

        ResRetakeExamDTO res = retakeExamService.cancelRetakeExam(RETAKE_ID);

        Assertions.assertEquals(RetakeExamStatus.CANCELLED, res.status(), "Status should be CANCELLED");
        Mockito.verifyNoInteractions(transcriptStateService, calculationTaskService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findRetakeExamsReturnsPage() {
        RetakeExam exam = new RetakeExam(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, DEFAULT_PRE_SCORE, RetakeExamStatus.PLANNED);
        ReflectionTestUtils.setField(exam, "id", RETAKE_ID);

        ReqFilterRetakeExamDTO filter = new ReqFilterRetakeExamDTO();
        filter.setStudentId(STUDENT_ID);

        Mockito.when(retakeExamRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exam)));

        Page<ResRetakeExamDTO> page = retakeExamService.findRetakeExams(filter);

        Assertions.assertEquals(1, page.getTotalElements(), "Total elements should be 1");
        Assertions.assertEquals(RETAKE_ID, page.getContent().get(0).retakeId(), MSG_RETAKE_ID_MATCH);
    }

    @Test
    void findRetakeExamSuccess() {
        RetakeExam exam = new RetakeExam(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, DEFAULT_PRE_SCORE, RetakeExamStatus.PLANNED);
        ReflectionTestUtils.setField(exam, "id", RETAKE_ID);

        Mockito.when(retakeExamRepository.findById(RETAKE_ID)).thenReturn(Optional.of(exam));

        ResRetakeExamDTO res = retakeExamService.getRetakeExam(RETAKE_ID);

        Assertions.assertEquals(RETAKE_ID, res.retakeId(), MSG_RETAKE_ID_MATCH);
        Assertions.assertEquals(DEFAULT_PRE_SCORE, res.preRetakeScore(), "Pre-retake score should match");
    }

    @Test
    void findRetakeExamNotFoundThrowsAppException() {
        Mockito.when(retakeExamRepository.findById(RETAKE_ID)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                AppException.class,
                () -> retakeExamService.getRetakeExam(RETAKE_ID),
                "Should throw AppException when retake is not found");
    }

    @Test
    void cancelRetakeExamAlreadyCancelledReturnsCurrent() {
        RetakeExam exam = new RetakeExam(
                STUDENT_ID, ACADEMIC_YEAR_ID, SUBJECT_ID, DEFAULT_PRE_SCORE, RetakeExamStatus.CANCELLED);
        ReflectionTestUtils.setField(exam, "id", RETAKE_ID);

        Mockito.when(retakeExamRepository.findById(RETAKE_ID)).thenReturn(Optional.of(exam));

        ResRetakeExamDTO res = retakeExamService.cancelRetakeExam(RETAKE_ID);

        Assertions.assertEquals(RetakeExamStatus.CANCELLED, res.status(), "Status should remain CANCELLED");
        Mockito.verifyNoInteractions(auditService, transcriptStateService, calculationTaskService);
    }

    private void authenticate(Long userId, String roleCode) {
        User user = new User("user" + userId, "password");
        ReflectionTestUtils.setField(user, "id", userId);
        user.addRole(new Role(roleCode, roleCode, roleCode));
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

package com.JavaTraining.BaiTap_RS.academic.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterCompletenessReportDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterLockReport;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterLockReportStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterLockReportRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequestStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScoreChangeRequestRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
                "PMD.UnitTestContainsTooManyAsserts",
                "PMD.CouplingBetweenObjects",
                "PMD.ExcessiveImports"
})
class SemesterCompletenessServiceTest {

        @Mock
        private ClassSubjectRepository classSubjectRepository;

        @Mock
        private SubjectRepository subjectRepository;

        @Mock
        private ScorebookRepository scorebookRepository;

        @Mock
        private AssessmentColumnRepository assessmentColumnRepository;

        @Mock
        private StudentScoreRepository studentScoreRepository;

        @Mock
        private StudentYearEnrollmentRepository studentYearEnrollmentRepository;

        @Mock
        private ScoreChangeRequestRepository scoreChangeRequestRepository;

        @Mock
        private SemesterLockReportRepository reportRepository;

        private SemesterCompletenessService completenessService;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                completenessService = new SemesterCompletenessService(
                                classSubjectRepository,
                                subjectRepository,
                                scorebookRepository,
                                assessmentColumnRepository,
                                studentScoreRepository,
                                studentYearEnrollmentRepository,
                                scoreChangeRequestRepository,
                                reportRepository,
                                objectMapper);
        }

        @Test
        void evaluateCompletenessAllComplete() {
                ClassSubject cs = classSubject(1L, 10L, 20L, 1L);
                Mockito.when(classSubjectRepository.findAllBySemesterIdAndStatus(1L, ClassSubjectStatus.ACTIVE))
                                .thenReturn(List.of(cs));

                Scorebook scorebook = scorebook(100L, 1L, ScorebookStatus.PUBLISHED);
                Mockito.when(scorebookRepository.findByClassSubjectId(1L)).thenReturn(Optional.of(scorebook));

                AssessmentColumn ktdk = column(11L, 100L, AssessmentType.KTDK, 1, true);
                AssessmentColumn ktck = column(12L, 100L, AssessmentType.KTCK, 1, true);
                Mockito.when(assessmentColumnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(100L))
                                .thenReturn(List.of(ktdk, ktck));

                Subject subject = subject(20L, SubjectType.ACADEMIC);
                Mockito.when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject));

                StudentYearEnrollment enrollment = enrollment(501L, 10L);
                Mockito.when(studentYearEnrollmentRepository.findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                                10L, EnrollmentStatus.ACTIVE))
                                .thenReturn(List.of(enrollment));

                StudentScore score1 = score(501L, 11L, ScoreStatus.SCORED, BigDecimal.valueOf(8.5));
                StudentScore score2 = score(501L, 12L, ScoreStatus.SCORED, BigDecimal.valueOf(9.0));
                Mockito.when(studentScoreRepository.findAllByAssessmentColumnIdIn(List.of(11L, 12L)))
                                .thenReturn(List.of(score1, score2));

                Mockito.when(scoreChangeRequestRepository.countByAssessmentColumnIdInAndStatus(
                                List.of(11L, 12L), ScoreChangeRequestStatus.PENDING))
                                .thenReturn(0L);

                SemesterCompletenessSummaryDTO summary = completenessService.evaluateCompleteness(1L);

                Assertions.assertTrue(summary.complete(), "completeness should be true");
                Assertions.assertEquals(0, summary.missingKtdkCount(), "missing KTDK should be 0");
                Assertions.assertEquals(0, summary.invalidKtckCount(), "invalid KTCK should be 0");
                Assertions.assertEquals(0, summary.unenteredScoreCount(), "unentered score count should be 0");
                Assertions.assertEquals(0, summary.unpublishedScorebookCount(),
                                "unpublished scorebook count should be 0");
                Assertions.assertEquals(0, summary.pendingScoreChangeRequestCount(),
                                "pending change requests should be 0");
        }

        @Test
        void evaluateCompletenessDetectsMissingKtdkAndUnpublishedScorebook() {
                ClassSubject cs = classSubject(1L, 10L, 20L, 1L);
                Mockito.when(classSubjectRepository.findAllBySemesterIdAndStatus(1L, ClassSubjectStatus.ACTIVE))
                                .thenReturn(List.of(cs));

                Scorebook scorebook = scorebook(100L, 1L, ScorebookStatus.OPEN);
                Mockito.when(scorebookRepository.findByClassSubjectId(1L)).thenReturn(Optional.of(scorebook));

                AssessmentColumn ktck = column(12L, 100L, AssessmentType.KTCK, 1, true);
                Mockito.when(assessmentColumnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(100L))
                                .thenReturn(List.of(ktck));

                Subject subject = subject(20L, SubjectType.ACADEMIC);
                Mockito.when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject));
                Mockito.when(studentYearEnrollmentRepository.findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                                10L, EnrollmentStatus.ACTIVE))
                                .thenReturn(List.of());

                Mockito.when(scoreChangeRequestRepository.countByAssessmentColumnIdInAndStatus(
                                List.of(12L), ScoreChangeRequestStatus.PENDING))
                                .thenReturn(1L);

                SemesterCompletenessSummaryDTO summary = completenessService.evaluateCompleteness(1L);

                Assertions.assertFalse(summary.complete(), "completeness should be false");
                Assertions.assertEquals(1, summary.missingKtdkCount(), "missing KTDK count should be 1");
                Assertions.assertEquals(1, summary.unpublishedScorebookCount(),
                                "unpublished scorebook count should be 1");
                Assertions.assertEquals(1, summary.pendingScoreChangeRequestCount(),
                                "pending score change requests should be 1");
        }

        @Test
        void evaluateAndSaveReportPersistsAndIsIdempotent() {
                Mockito.when(reportRepository.findByRunIdAndSemesterIdAndCheckpointCode(10L, 1L, "t"))
                                .thenReturn(Optional.empty());
                Mockito.when(classSubjectRepository.findAllBySemesterIdAndStatus(1L, ClassSubjectStatus.ACTIVE))
                                .thenReturn(List.of());
                Mockito.when(reportRepository.save(Mockito.any(SemesterLockReport.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                SemesterLockReport report = completenessService.evaluateAndSaveReport(10L, 1L, "t", "CORR-1");

                Assertions.assertNotNull(report, "saved report should not be null");
                Assertions.assertEquals("t", report.getCheckpointCode(), "checkpointCode should match");
                Assertions.assertEquals(SemesterLockReportStatus.COMPLETE, report.getReportStatus(),
                                "status should be COMPLETE");

                Mockito.when(reportRepository.findByRunIdAndSemesterIdAndCheckpointCode(10L, 1L, "t"))
                                .thenReturn(Optional.of(report));
                SemesterLockReport secondCall = completenessService.evaluateAndSaveReport(10L, 1L, "t", "CORR-1");
                Assertions.assertSame(report, secondCall, "second call should return existing report");
        }

        @Test
        void latestReportRetrievesExistingReport() {
                SemesterLockReport report = new SemesterLockReport(
                                10L,
                                1L,
                                "t",
                                SemesterLockReportStatus.COMPLETE,
                                LocalDateTime.now(),
                                "SEMESTER",
                                "{\"complete\":true,\"missingKtdkCount\":0,\"invalidKtckCount\":0,"
                                                + "\"missingSkillColumnsCount\":0,\"unenteredScoreCount\":0,"
                                                + "\"studentWithoutScoreDataCount\":0,\"unpublishedScorebookCount\":0,"
                                                + "\"pendingScoreChangeRequestCount\":0,\"details\":[]}",
                                null,
                                "CORR-1");
                ReflectionTestUtils.setField(report, "id", 77L);

                Mockito.when(reportRepository.findFirstBySemesterIdAndCheckpointCodeOrderByEvaluatedAtDesc(1L, "t"))
                                .thenReturn(Optional.of(report));

                ResSemesterCompletenessReportDTO res = completenessService.getLatestReport(1L, "t");

                Assertions.assertEquals(77L, res.reportId(), "reportId should match");
                Assertions.assertEquals("t", res.checkpointCode(), "checkpointCode should match");
                Assertions.assertTrue(res.summary().complete(), "summary should be complete");
        }

        private ClassSubject classSubject(Long id, Long classId, Long subjectId, Long semesterId) {
                ClassSubject cs = new ClassSubject(classId, subjectId, semesterId, ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(cs, "id", id);
                return cs;
        }

        private Scorebook scorebook(Long id, Long classSubjectId, ScorebookStatus status) {
                Scorebook sb = new Scorebook(classSubjectId, status);
                ReflectionTestUtils.setField(sb, "id", id);
                return sb;
        }

        private AssessmentColumn column(
                        Long id,
                        Long scorebookId,
                        AssessmentType type,
                        Integer columnNo,
                        boolean required) {
                AssessmentColumn col = new AssessmentColumn(
                                scorebookId,
                                type,
                                columnNo,
                                type.name(),
                                BigDecimal.ONE,
                                required);
                ReflectionTestUtils.setField(col, "id", id);
                return col;
        }

        private Subject subject(Long id, SubjectType type) {
                Subject s = new Subject("MATH", "Toán", type, ApplicationScope.GRADE, SubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(s, "id", id);
                return s;
        }

        private StudentYearEnrollment enrollment(Long studentId, Long classId) {
                StudentYearEnrollment e = new StudentYearEnrollment(
                                studentId,
                                10L,
                                classId,
                                EnrollmentStatus.ACTIVE,
                                LocalDateTime.now());
                ReflectionTestUtils.setField(e, "id", studentId + 1000L);
                return e;
        }

        private StudentScore score(Long studentId, Long columnId, ScoreStatus status, BigDecimal val) {
                StudentScore s = new StudentScore(columnId, studentId, status, val, null, 1L);
                ReflectionTestUtils.setField(s, "id", studentId + 2000L);
                return s;
        }
}

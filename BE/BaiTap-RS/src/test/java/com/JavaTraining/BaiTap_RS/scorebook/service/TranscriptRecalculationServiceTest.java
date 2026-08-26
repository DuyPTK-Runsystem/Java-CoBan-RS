package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationResultSource;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.SkillWeightConfig;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.SkillWeightConfigRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
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
                "PMD.CouplingBetweenObjects",
                "PMD.ExcessiveImports",
                "PMD.TooManyMethods",
                "PMD.AvoidDuplicateLiterals",
                "PMD.UnitTestAssertionsShouldIncludeMessage",
                "PMD.UnitTestContainsTooManyAsserts"
})
class TranscriptRecalculationServiceTest {

        private static final Long STUDENT_ID = 200L;
        private static final Long ACADEMIC_YEAR_ID = 10L;
        private static final Long CLASS_ID = 20L;
        private static final Long SUBJECT_ID = 70L;
        private static final Long ANNUAL_TRANSCRIPT_ID = 500L;
        private static final Long HK1_ID = 801L;
        private static final Long HK2_ID = 802L;

        @Mock
        private StudentYearEnrollmentRepository enrollmentRepository;
        @Mock
        private SemesterRepository semesterRepository;
        @Mock
        private ClassSubjectRepository classSubjectRepository;
        @Mock
        private SubjectRepository subjectRepository;
        @Mock
        private ScorebookRepository scorebookRepository;
        @Mock
        private AssessmentColumnRepository columnRepository;
        @Mock
        private StudentScoreRepository scoreRepository;
        @Mock
        private SkillWeightConfigRepository skillWeightRepository;
        @Mock
        private StudentAnnualTranscriptRepository annualTranscriptRepository;
        @Mock
        private StudentTermTranscriptRepository termTranscriptRepository;
        @Mock
        private StudentSubjectTermResultRepository termResultRepository;
        @Mock
        private StudentSubjectAnnualResultRepository annualResultRepository;

        private TranscriptRecalculationService recalculationService;

        @BeforeEach
        void setUp() {
                recalculationService = new TranscriptRecalculationService(
                                new SubjectScoreCalculator(),
                                enrollmentRepository,
                                semesterRepository,
                                classSubjectRepository,
                                subjectRepository,
                                scorebookRepository,
                                columnRepository,
                                scoreRepository,
                                skillWeightRepository,
                                annualTranscriptRepository,
                                termTranscriptRepository,
                                termResultRepository,
                                annualResultRepository);
        }

        @Test
        void recalculatesTermsAndAnnualResultsInEndDateOrder() {
                StudentAnnualTranscript annual = stubCalculationContext(3L);

                recalculationService.recalculate(STUDENT_ID, ACADEMIC_YEAR_ID, 3L, 900L);

                Mockito.verify(semesterRepository)
                                .findAllByAcademicYearIdOrderByEndDateAscDisplayOrderAscIdAsc(ACADEMIC_YEAR_ID);
                Assertions.assertEquals(new BigDecimal("7.0"), findTerm(HK1_ID).getDtbhk());
                Assertions.assertEquals(new BigDecimal("8.0"), findTerm(HK2_ID).getDtbhk());
                Assertions.assertEquals(new BigDecimal("7.7"), annual.getRegularDtbcn());
                Assertions.assertEquals(new BigDecimal("7.7"), annual.getFinalDtbcn());
                Assertions.assertEquals(CalculationStatus.FINISH, annual.getCalculationStatus());
                Assertions.assertEquals(CalculationResultSource.REGULAR, annual.getResultSource());
                Assertions.assertEquals(3L, annual.getCalculatedVersion());
                Assertions.assertEquals(900L, annual.getLastCalculationTaskId());

                StudentSubjectAnnualResult annualResult = savedAnnualResult();
                Assertions.assertEquals(new BigDecimal("7.7"), annualResult.getRegularDtbmhCn());
                Assertions.assertEquals(9010L, annualResult.getHk1TermResultId());
                Assertions.assertEquals(9020L, annualResult.getHk2TermResultId());
                Assertions.assertEquals(CalculationResultSource.REGULAR, annualResult.getCalculationSource());
        }

        @Test
        void keepsTranscriptsInProgressWhenSourceVersionIsNewerThanRequestedVersion() {
                StudentAnnualTranscript annual = stubCalculationContext(3L);

                recalculationService.recalculate(STUDENT_ID, ACADEMIC_YEAR_ID, 2L, 901L);

                Assertions.assertEquals(CalculationStatus.IN_PROGRESS, annual.getCalculationStatus());
                Assertions.assertEquals(CalculationStatus.IN_PROGRESS, findTerm(HK1_ID).getCalculationStatus());
                Assertions.assertEquals(CalculationStatus.IN_PROGRESS, findTerm(HK2_ID).getCalculationStatus());
                Assertions.assertEquals(new BigDecimal("7.7"), annual.getRegularDtbcn());
                Assertions.assertEquals(2L, annual.getCalculatedVersion());
        }

        @Test
        void calculatesSkillSubjectAndExcludesFromTermAndAnnualAverages() {
                Long skillSubjectId = 71L;
                StudentAnnualTranscript annual = stubCalculationContextWithSkill(3L, skillSubjectId);

                recalculationService.recalculate(STUDENT_ID, ACADEMIC_YEAR_ID, 3L, 905L);

                Assertions.assertEquals(new BigDecimal("7.0"), findTerm(HK1_ID).getDtbhk());
                Assertions.assertEquals(new BigDecimal("8.0"), findTerm(HK2_ID).getDtbhk());
                Assertions.assertEquals(new BigDecimal("7.7"), annual.getRegularDtbcn());
                Assertions.assertEquals(new BigDecimal("7.7"), annual.getFinalDtbcn());
                Assertions.assertEquals(CalculationStatus.FINISH, annual.getCalculationStatus());

                List<StudentSubjectTermResult> termResults = savedTermResults();
                StudentSubjectTermResult skillTermResult = termResults.stream()
                                .filter(r -> skillSubjectId.equals(r.getSubjectId())
                                                && HK1_ID.equals(r.getTermTranscriptId() / 10))
                                .findFirst()
                                .orElseThrow();
                Assertions.assertNull(skillTermResult.getDtbmh());
                Assertions.assertEquals(new BigDecimal("8.3"), skillTermResult.getSkillScore());
                Assertions.assertEquals(3L, skillTermResult.getCalculatedVersion());

                List<StudentSubjectAnnualResult> annualResults = savedAnnualResults();
                StudentSubjectAnnualResult skillAnnualResult = annualResults.stream()
                                .filter(r -> skillSubjectId.equals(r.getSubjectId()))
                                .findFirst()
                                .orElseThrow();
                Assertions.assertNull(skillAnnualResult.getRegularDtbmhCn());
                Assertions.assertNull(skillAnnualResult.getOfficialDtbmhCn());
                Assertions.assertEquals(3L, skillAnnualResult.getCalculatedVersion());
        }

        @Test
        void handlesSingleSemesterSubjectAndMissingScoresGracefully() {
                Long singleSemSubjectId = 72L;
                StudentAnnualTranscript annual = stubCalculationContextWithSingleSemesterSubject(3L,
                                singleSemSubjectId);

                recalculationService.recalculate(STUDENT_ID, ACADEMIC_YEAR_ID, 3L, 906L);

                List<StudentSubjectAnnualResult> annualResults = savedAnnualResults();
                StudentSubjectAnnualResult mathResult = annualResults.stream()
                                .filter(r -> SUBJECT_ID.equals(r.getSubjectId()))
                                .findFirst()
                                .orElseThrow();
                Assertions.assertNull(mathResult.getRegularDtbmhCn());

                StudentSubjectAnnualResult singleResult = annualResults.stream()
                                .filter(r -> singleSemSubjectId.equals(r.getSubjectId()))
                                .findFirst()
                                .orElseThrow();
                Assertions.assertEquals(new BigDecimal("9.0"), singleResult.getRegularDtbmhCn());
                Assertions.assertEquals(new BigDecimal("9.0"), annual.getRegularDtbcn());
        }

        @Test
        void rejectsMissingActiveEnrollmentBeforeWritingResults() {
                Mockito.when(enrollmentRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                                .thenReturn(Optional.empty());

                Assertions.assertThrows(
                                RuntimeException.class,
                                () -> recalculationService.recalculate(STUDENT_ID, ACADEMIC_YEAR_ID, 1L, 1L));
                Mockito.verifyNoInteractions(annualTranscriptRepository, termTranscriptRepository);
        }

        private StudentAnnualTranscript stubCalculationContext(long sourceVersion) {
                StudentAnnualTranscript annual = new StudentAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
                ReflectionTestUtils.setField(annual, "id", ANNUAL_TRANSCRIPT_ID);
                annual.setSourceVersion(sourceVersion);
                annual.setCalculationStatus(CalculationStatus.IN_PROGRESS);

                StudentYearEnrollment enrollment = new StudentYearEnrollment(
                                STUDENT_ID,
                                ACADEMIC_YEAR_ID,
                                CLASS_ID,
                                EnrollmentStatus.ACTIVE,
                                LocalDateTime.now());
                Semester hk1 = semester(HK1_ID, "HK1", LocalDate.of(2026, 12, 31), 1);
                Semester hk2 = semester(HK2_ID, "HK2", LocalDate.of(2027, 5, 31), 2);
                ClassSubject hk1Subject = classSubject(201L, HK1_ID);
                ClassSubject hk2Subject = classSubject(202L, HK2_ID);
                Subject subject = new Subject(
                                "MATH",
                                "Toán",
                                SubjectType.ACADEMIC,
                                ApplicationScope.CLASS,
                                SubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(subject, "id", SUBJECT_ID);

                Mockito.when(enrollmentRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                                .thenReturn(Optional.of(enrollment));
                Mockito.when(annualTranscriptRepository.findForUpdate(STUDENT_ID, ACADEMIC_YEAR_ID))
                                .thenReturn(Optional.of(annual));
                Mockito.when(semesterRepository
                                .findAllByAcademicYearIdOrderByEndDateAscDisplayOrderAscIdAsc(ACADEMIC_YEAR_ID))
                                .thenReturn(List.of(hk1, hk2));
                Mockito.when(classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(CLASS_ID, HK1_ID))
                                .thenReturn(List.of(hk1Subject));
                Mockito.when(classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(CLASS_ID, HK2_ID))
                                .thenReturn(List.of(hk2Subject));
                Mockito.when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
                Mockito.when(scorebookRepository.findByClassSubjectId(201L))
                                .thenReturn(Optional.of(scorebook(901L, 201L)));
                Mockito.when(scorebookRepository.findByClassSubjectId(202L))
                                .thenReturn(Optional.of(scorebook(902L, 202L)));
                Mockito.when(columnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(901L))
                                .thenReturn(List.of(column(101L)));
                Mockito.when(columnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(902L))
                                .thenReturn(List.of(column(102L)));
                Mockito.when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(
                                List.of(101L), List.of(STUDENT_ID)))
                                .thenReturn(List.of(score(101L, "7.0")));
                Mockito.when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(
                                List.of(102L), List.of(STUDENT_ID)))
                                .thenReturn(List.of(score(102L, "8.0")));
                Mockito.when(termTranscriptRepository.findByAnnualTranscriptIdAndSemesterId(ANNUAL_TRANSCRIPT_ID,
                                HK1_ID))
                                .thenReturn(Optional.empty());
                Mockito.when(termTranscriptRepository.findByAnnualTranscriptIdAndSemesterId(ANNUAL_TRANSCRIPT_ID,
                                HK2_ID))
                                .thenReturn(Optional.empty());
                Mockito.when(termResultRepository.findByTermTranscriptIdAndSubjectId(Mockito.anyLong(),
                                Mockito.eq(SUBJECT_ID)))
                                .thenReturn(Optional.empty());
                Mockito.when(annualResultRepository.findByAnnualTranscriptIdAndSubjectId(ANNUAL_TRANSCRIPT_ID,
                                SUBJECT_ID))
                                .thenReturn(Optional.empty());
                Mockito.when(termTranscriptRepository.save(Mockito.any(StudentTermTranscript.class)))
                                .thenAnswer(invocation -> {
                                        StudentTermTranscript term = invocation.getArgument(0);
                                        if (term.getId() == null) {
                                                ReflectionTestUtils.setField(term, "id", term.getSemesterId() * 10);
                                        }
                                        return term;
                                });
                Mockito.when(termResultRepository.save(Mockito.any(StudentSubjectTermResult.class)))
                                .thenAnswer(invocation -> {
                                        StudentSubjectTermResult result = invocation.getArgument(0);
                                        if (result.getId() == null) {
                                                ReflectionTestUtils.setField(result, "id",
                                                                result.getTermTranscriptId() + 1000);
                                        }
                                        return result;
                                });
                Mockito.when(annualResultRepository.save(Mockito.any(StudentSubjectAnnualResult.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                Mockito.when(annualTranscriptRepository.save(Mockito.any(StudentAnnualTranscript.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                return annual;
        }

        private StudentTermTranscript findTerm(Long semesterId) {
                org.mockito.ArgumentCaptor<StudentTermTranscript> captor = org.mockito.ArgumentCaptor
                                .forClass(StudentTermTranscript.class);
                Mockito.verify(termTranscriptRepository, Mockito.atLeastOnce()).save(captor.capture());
                return captor.getAllValues().stream()
                                .filter(term -> semesterId.equals(term.getSemesterId()))
                                .reduce((first, second) -> second)
                                .orElseThrow();
        }

        private List<StudentSubjectTermResult> savedTermResults() {
                org.mockito.ArgumentCaptor<StudentSubjectTermResult> captor = org.mockito.ArgumentCaptor
                                .forClass(StudentSubjectTermResult.class);
                Mockito.verify(termResultRepository, Mockito.atLeastOnce()).save(captor.capture());
                return captor.getAllValues();
        }

        private List<StudentSubjectAnnualResult> savedAnnualResults() {
                org.mockito.ArgumentCaptor<StudentSubjectAnnualResult> captor = org.mockito.ArgumentCaptor
                                .forClass(StudentSubjectAnnualResult.class);
                Mockito.verify(annualResultRepository, Mockito.atLeastOnce()).save(captor.capture());
                return captor.getAllValues();
        }

        private StudentSubjectAnnualResult savedAnnualResult() {
                org.mockito.ArgumentCaptor<StudentSubjectAnnualResult> captor = org.mockito.ArgumentCaptor
                                .forClass(StudentSubjectAnnualResult.class);
                Mockito.verify(annualResultRepository).save(captor.capture());
                return captor.getValue();
        }

        private StudentAnnualTranscript stubCalculationContextWithSkill(long sourceVersion, Long skillSubjectId) {
                StudentAnnualTranscript annual = stubCalculationContext(sourceVersion);
                ClassSubject skillClassSubject = new ClassSubject(CLASS_ID, skillSubjectId, HK1_ID,
                                ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(skillClassSubject, "id", 203L);
                Subject skillSubject = new Subject(
                                "MUSIC", "Âm nhạc", SubjectType.SKILL, ApplicationScope.CLASS, SubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(skillSubject, "id", skillSubjectId);

                Mockito.when(classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(CLASS_ID, HK1_ID))
                                .thenReturn(List.of(classSubject(201L, HK1_ID), skillClassSubject));
                Mockito.when(subjectRepository.findById(skillSubjectId)).thenReturn(Optional.of(skillSubject));
                Mockito.when(scorebookRepository.findByClassSubjectId(203L))
                                .thenReturn(Optional.of(scorebook(903L, 203L)));
                SkillWeightConfig config = new SkillWeightConfig(
                                903L, new BigDecimal("20"), new BigDecimal("30"), new BigDecimal("50"), 7L,
                                LocalDateTime.now());
                Mockito.when(skillWeightRepository.findByScorebookId(903L)).thenReturn(Optional.of(config));

                AssessmentColumn colKttt = new AssessmentColumn(
                                903L, AssessmentType.KTTT, 1, "KTTT", new BigDecimal("1.00"), false);
                ReflectionTestUtils.setField(colKttt, "id", 103L);
                colKttt.setStatus(AssessmentColumnStatus.ACTIVE);
                AssessmentColumn colKtdk = new AssessmentColumn(
                                903L, AssessmentType.KTDK, 1, "KTDK", new BigDecimal("1.00"), false);
                ReflectionTestUtils.setField(colKtdk, "id", 104L);
                colKtdk.setStatus(AssessmentColumnStatus.ACTIVE);
                AssessmentColumn colKtck = new AssessmentColumn(
                                903L, AssessmentType.KTCK, 1, "KTCK", new BigDecimal("1.00"), false);
                ReflectionTestUtils.setField(colKtck, "id", 105L);
                colKtck.setStatus(AssessmentColumnStatus.ACTIVE);

                Mockito.when(columnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(903L))
                                .thenReturn(List.of(colKttt, colKtdk, colKtck));
                Mockito.when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(
                                List.of(103L, 104L, 105L), List.of(STUDENT_ID)))
                                .thenReturn(List.of(
                                                score(103L, "7.0"),
                                                score(104L, "8.0"),
                                                score(105L, "9.0")));
                return annual;
        }

        private StudentAnnualTranscript stubCalculationContextWithSingleSemesterSubject(
                        long sourceVersion, Long singleSemSubjectId) {
                StudentAnnualTranscript annual = stubCalculationContext(sourceVersion);
                ClassSubject singleSubject = new ClassSubject(
                                CLASS_ID, singleSemSubjectId, HK1_ID, ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(singleSubject, "id", 204L);
                Subject physSubject = new Subject(
                                "PHYSICS", "Vật lý", SubjectType.ACADEMIC, ApplicationScope.CLASS,
                                SubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(physSubject, "id", singleSemSubjectId);

                Mockito.when(classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(CLASS_ID, HK1_ID))
                                .thenReturn(List.of(classSubject(201L, HK1_ID), singleSubject));
                Mockito.when(subjectRepository.findById(singleSemSubjectId)).thenReturn(Optional.of(physSubject));
                Mockito.when(scorebookRepository.findByClassSubjectId(204L))
                                .thenReturn(Optional.of(scorebook(904L, 204L)));
                Mockito.when(columnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(904L))
                                .thenReturn(List.of(column(106L)));
                Mockito.when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(
                                List.of(106L), List.of(STUDENT_ID)))
                                .thenReturn(List.of(score(106L, "9.0")));

                // Math in HK2 has no scores
                Mockito.when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(
                                List.of(102L), List.of(STUDENT_ID)))
                                .thenReturn(List.of());
                return annual;
        }

        private static Semester semester(Long id, String code, LocalDate endDate, int displayOrder) {
                Semester semester = new Semester(
                                ACADEMIC_YEAR_ID,
                                code,
                                code,
                                displayOrder,
                                endDate.minusMonths(4),
                                endDate,
                                null,
                                SemesterStatus.ACTIVE);
                ReflectionTestUtils.setField(semester, "id", id);
                return semester;
        }

        private static ClassSubject classSubject(Long id, Long semesterId) {
                ClassSubject classSubject = new ClassSubject(
                                CLASS_ID,
                                SUBJECT_ID,
                                semesterId,
                                ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(classSubject, "id", id);
                return classSubject;
        }

        private static Scorebook scorebook(Long id, Long classSubjectId) {
                Scorebook scorebook = new Scorebook(classSubjectId, ScorebookStatus.OPEN);
                ReflectionTestUtils.setField(scorebook, "id", id);
                return scorebook;
        }

        private static AssessmentColumn column(Long id) {
                AssessmentColumn column = new AssessmentColumn(
                                id,
                                AssessmentType.KTTT,
                                1,
                                "KTTT",
                                new BigDecimal("1.00"),
                                false);
                ReflectionTestUtils.setField(column, "id", id);
                column.setStatus(AssessmentColumnStatus.ACTIVE);
                return column;
        }

        private static StudentScore score(Long columnId, String value) {
                return new StudentScore(
                                columnId,
                                STUDENT_ID,
                                ScoreStatus.SCORED,
                                new BigDecimal(value),
                                null,
                                7L);
        }
}

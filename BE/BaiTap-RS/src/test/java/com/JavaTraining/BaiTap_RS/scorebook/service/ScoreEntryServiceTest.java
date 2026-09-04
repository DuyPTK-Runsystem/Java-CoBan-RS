package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqBulkScoreItemDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqBulkUpsertStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreGridDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
                "PMD.TooManyMethods",
                "PMD.UnitTestContainsTooManyAsserts",
                "PMD.ExcessiveImports",
                "PMD.CouplingBetweenObjects",
                "PMD.AvoidDuplicateLiterals"
})
class ScoreEntryServiceTest {

        private static final Long SCOREBOOK_ID = 1L;
        private static final Long CLASS_SUBJECT_ID = 10L;
        private static final Long CLASS_ID = 20L;
        private static final Long SUBJECT_ID = 30L;
        private static final Long SEMESTER_ID = 40L;
        private static final Long ACADEMIC_YEAR_ID = 50L;
        private static final Long COLUMN_ID = 100L;
        private static final Long STUDENT_ID = 200L;
        private static final BigDecimal VAL_8_0 = new BigDecimal("8.0");

        @Mock
        private AssessmentColumnRepository columnRepository;
        @Mock
        private ScorebookRepository scorebookRepository;
        @Mock
        private ClassSubjectRepository classSubjectRepository;
        @Mock
        private SemesterRepository semesterRepository;
        @Mock
        private StudentRepository studentRepository;
        @Mock
        private StudentYearEnrollmentRepository enrollmentRepository;
        @Mock
        private StudentScoreRepository scoreRepository;
        @Mock
        private ScorebookGuard scorebookGuard;
        @Mock
        private TranscriptStateService transcriptService;
        @Mock
        private CalculationTaskService taskService;
        @Mock
        private ScorebookAuditService auditService;
        @Mock
        private EnrollmentRosterRepository rosterRepository;
        @Mock
        private StudentLookupService studentLookupService;

        private ScoreEntryService scoreEntryService;
        private ScoreGridService scoreGridService;

        @BeforeEach
        void setUp() {
                ScoreEntryContext context = new ScoreEntryContext(
                                columnRepository,
                                scorebookRepository,
                                classSubjectRepository,
                                semesterRepository,
                                studentRepository,
                                enrollmentRepository);

                ScoreEntryValidator validator = new ScoreEntryValidator();
                ScoreAuditDataMapper auditMapper = new ScoreAuditDataMapper();
                ScoreResponseMapper responseMapper = new ScoreResponseMapper();

                ScoreEntryWriter scoreWriter = new ScoreEntryWriter(
                                scoreRepository,
                                auditService,
                                auditMapper,
                                responseMapper,
                                validator);

                EnrollmentRosterService rosterService = new EnrollmentRosterService(
                                rosterRepository,
                                studentRepository);

                ScoreGridLoader gridLoader = new ScoreGridLoader(
                                scoreRepository,
                                columnRepository,
                                rosterService,
                                responseMapper);

                scoreEntryService = new ScoreEntryService(
                                context,
                                validator,
                                scorebookGuard,
                                transcriptService,
                                taskService,
                                scoreWriter,
                                studentLookupService);

                scoreGridService = new ScoreGridService(
                                context,
                                scorebookGuard,
                                gridLoader);
        }

        private void mockValidContext() {
                Mockito.lenient().when(columnRepository.findById(COLUMN_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.column(
                                                COLUMN_ID, SCOREBOOK_ID, AssessmentType.KTTT,
                                                AssessmentColumnStatus.ACTIVE)));
                Mockito.lenient().when(scorebookRepository.findById(SCOREBOOK_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.scorebook(
                                                SCOREBOOK_ID, CLASS_SUBJECT_ID, ScorebookStatus.OPEN)));
                Mockito.lenient().when(classSubjectRepository.findById(CLASS_SUBJECT_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.classSubject(
                                                CLASS_SUBJECT_ID, CLASS_ID, SUBJECT_ID, SEMESTER_ID)));
                Mockito.lenient().when(semesterRepository.findById(SEMESTER_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.semester(
                                                SEMESTER_ID, ACADEMIC_YEAR_ID, SemesterStatus.ACTIVE)));
                Mockito.lenient().when(studentRepository.findById(STUDENT_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.student(
                                                STUDENT_ID, "HS001", StudentStatus.ACTIVE)));
                Mockito.lenient()
                                .when(enrollmentRepository.findByStudentIdAndAcademicYearId(STUDENT_ID,
                                                ACADEMIC_YEAR_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.enrollment(
                                                1L, STUDENT_ID, ACADEMIC_YEAR_ID, CLASS_ID, EnrollmentStatus.ACTIVE)));
        }

        @Test
        void createScoreSuccess() {
                mockValidContext();
                Mockito.when(scoreRepository.findByAssessmentColumnIdAndStudentId(COLUMN_ID, STUDENT_ID))
                                .thenReturn(Optional.empty());
                Mockito.when(scoreRepository.save(Mockito.any(StudentScore.class)))
                                .thenAnswer(invocation -> {
                                        StudentScore s = invocation.getArgument(0);
                                        org.springframework.test.util.ReflectionTestUtils.setField(s, "id", 999L);
                                        return s;
                                });
                Mockito.when(transcriptService.touchTranscripts(STUDENT_ID, ACADEMIC_YEAR_ID, SEMESTER_ID))
                                .thenReturn(1L);

                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, new BigDecimal("8.5"), "Điểm miệng", null);

                ResStudentScoreDTO result = scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request);

                Assertions.assertNotNull(result, "score response should not be null");
                Assertions.assertEquals(ScoreStatus.SCORED, result.scoreStatus(), "score status should be SCORED");
                Assertions.assertEquals(new BigDecimal("8.5"), result.scoreValue(), "score value should match");
                Mockito.verify(taskService).ensureRecalcTask(STUDENT_ID, ACADEMIC_YEAR_ID, 1L);
        }

        @Test
        void createScoreWithZeroSuccess() {
                mockValidContext();
                Mockito.when(scoreRepository.findByAssessmentColumnIdAndStudentId(COLUMN_ID, STUDENT_ID))
                                .thenReturn(Optional.empty());
                Mockito.when(scoreRepository.save(Mockito.any(StudentScore.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, BigDecimal.ZERO, "Điểm 0 hợp lệ", null);

                ResStudentScoreDTO result = scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request);

                Assertions.assertEquals(BigDecimal.ZERO, result.scoreValue(), "score 0.0 should be valid");
        }

        @Test
        void createScoreByStudentCodeSuccess() {
                mockValidContext();
                Student student = ScoreEntryTestFixtures.student(STUDENT_ID, "HS001", StudentStatus.ACTIVE);
                Mockito.when(studentLookupService.resolveStudent(null, "HS001")).thenReturn(student);
                Mockito.when(scoreRepository.findByAssessmentColumnIdAndStudentId(COLUMN_ID, STUDENT_ID))
                                .thenReturn(Optional.empty());
                Mockito.when(scoreRepository.save(Mockito.any(StudentScore.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                Mockito.when(transcriptService.touchTranscripts(STUDENT_ID, ACADEMIC_YEAR_ID, SEMESTER_ID))
                                .thenReturn(1L);

                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, VAL_8_0, "Nhập theo mã", null);

                ResStudentScoreDTO result = scoreEntryService.upsertSingleScoreByCode(COLUMN_ID, "HS001", request);

                Assertions.assertEquals("HS001", result.studentCode(), "student code should be returned");
                Assertions.assertEquals("Học sinh 200", result.studentName(), "student name should be returned");
        }

        @Test
        void createScoreAbsentNoValueSuccess() {
                mockValidContext();
                Mockito.when(scoreRepository.findByAssessmentColumnIdAndStudentId(COLUMN_ID, STUDENT_ID))
                                .thenReturn(Optional.empty());
                Mockito.when(scoreRepository.save(Mockito.any(StudentScore.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.ABSENT, null, "Vắng thi", null);

                ResStudentScoreDTO result = scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request);

                Assertions.assertEquals(ScoreStatus.ABSENT, result.scoreStatus(), "status should be ABSENT");
                Assertions.assertNull(result.scoreValue(), "ABSENT score should have null value");
        }

        @Test
        void createScoreRejectsScoredWithoutValue() {
                mockValidContext();
                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, null, "Thiếu điểm", null);

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(), "missing scored value should be 400");
        }

        @Test
        void createScoreRejectsOutOfRangeValue() {
                mockValidContext();
                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, new BigDecimal("10.5"), "Điểm vượt trần", null);

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(), "out of range score should be 400");
        }

        @Test
        void createScoreRejectsInvalidScale() {
                mockValidContext();
                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, new BigDecimal("8.55"), "Điểm 2 chữ số thập phân", null);

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(), "invalid scale score should be 400");
        }

        @Test
        void createScoreRejectsAbsentWithValue() {
                mockValidContext();
                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.ABSENT, new BigDecimal("5.0"), "Vắng nhưng có điểm", null);

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(), "absent with value should be 400");
        }

        @Test
        void createScoreRejectsWithExpectedVersion() {
                mockValidContext();
                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, new BigDecimal("7.0"), "Tạo mới có version", 1L);

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(), "create with version should be 400");
        }

        @Test
        void updateScoreVersionMismatchConflict() {
                mockValidContext();
                StudentScore existingScore = ScoreEntryTestFixtures.score(
                                1L, COLUMN_ID, STUDENT_ID, ScoreStatus.SCORED, new BigDecimal("7.0"), 2L,
                                LocalDateTime.now());
                Mockito.when(scoreRepository.findByAssessmentColumnIdAndStudentId(COLUMN_ID, STUDENT_ID))
                                .thenReturn(Optional.of(existingScore));

                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, VAL_8_0, "Sửa điểm", 1L);

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request));
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus(), "version mismatch should be 409");
        }

        @Test
        void updateScoreOverTenDaysConflict() {
                mockValidContext();
                StudentScore existingScore = ScoreEntryTestFixtures.score(
                                1L, COLUMN_ID, STUDENT_ID, ScoreStatus.SCORED,
                                new BigDecimal("7.0"), 1L, LocalDateTime.now().minusDays(15));
                Mockito.when(scoreRepository.findByAssessmentColumnIdAndStudentId(COLUMN_ID, STUDENT_ID))
                                .thenReturn(Optional.of(existingScore));

                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, VAL_8_0, "Sửa quá hạn", 1L);

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request));
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus(), "expired update should be 409");
        }

        @Test
        void updateScoreWhenSemesterLockedConflict() {
                Mockito.when(columnRepository.findById(COLUMN_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.column(
                                                COLUMN_ID, SCOREBOOK_ID, AssessmentType.KTTT,
                                                AssessmentColumnStatus.ACTIVE)));
                Mockito.when(scorebookRepository.findById(SCOREBOOK_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.scorebook(
                                                SCOREBOOK_ID, CLASS_SUBJECT_ID, ScorebookStatus.OPEN)));
                Mockito.when(classSubjectRepository.findById(CLASS_SUBJECT_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.classSubject(
                                                CLASS_SUBJECT_ID, CLASS_ID, SUBJECT_ID, SEMESTER_ID)));
                Mockito.when(semesterRepository.findById(SEMESTER_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.semester(
                                                SEMESTER_ID, ACADEMIC_YEAR_ID, SemesterStatus.LOCKED)));

                ReqUpsertStudentScoreDTO request = new ReqUpsertStudentScoreDTO(
                                ScoreStatus.SCORED, VAL_8_0, "Kỳ đã khóa", null);

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> scoreEntryService.upsertSingleScore(COLUMN_ID, STUDENT_ID, request));
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus(), "locked semester should be 409");
        }

        @Test
        void bulkUpsertRejectsDuplicateStudents() {
                mockValidContext();
                Student student = ScoreEntryTestFixtures.student(STUDENT_ID, "HS001", StudentStatus.ACTIVE);
                Mockito.when(studentLookupService.resolveStudent(STUDENT_ID, null)).thenReturn(student);
                ReqBulkUpsertStudentScoreDTO request = new ReqBulkUpsertStudentScoreDTO(List.of(
                                new ReqBulkScoreItemDTO(STUDENT_ID, null, ScoreStatus.SCORED, VAL_8_0, null, null),
                                new ReqBulkScoreItemDTO(STUDENT_ID, null, ScoreStatus.SCORED, new BigDecimal("9.0"),
                                                null,
                                                null)));

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> scoreEntryService.bulkUpsertScores(COLUMN_ID, request));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(),
                                "duplicate student in bulk should be 400");
        }

        @Test
        void bulkUpsertAcceptsStudentCode() {
                mockValidContext();
                Student student = ScoreEntryTestFixtures.student(STUDENT_ID, "HS001", StudentStatus.ACTIVE);
                Mockito.when(studentLookupService.resolveStudent(null, "HS001")).thenReturn(student);
                Mockito.when(scoreRepository.findByAssessmentColumnIdAndStudentId(COLUMN_ID, STUDENT_ID))
                                .thenReturn(Optional.empty());
                Mockito.when(scoreRepository.save(Mockito.any(StudentScore.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                Mockito.when(transcriptService.touchTranscripts(STUDENT_ID, ACADEMIC_YEAR_ID, SEMESTER_ID))
                                .thenReturn(1L);

                ReqBulkUpsertStudentScoreDTO request = new ReqBulkUpsertStudentScoreDTO(List.of(
                                new ReqBulkScoreItemDTO(null, "HS001", ScoreStatus.SCORED, VAL_8_0, null, null)));

                List<ResStudentScoreDTO> result = scoreEntryService.bulkUpsertScores(COLUMN_ID, request);

                Assertions.assertEquals(1, result.size(), "one score should be upserted");
                Assertions.assertEquals("HS001", result.get(0).studentCode(), "student code should be returned");
        }

        @Test
        void scoreGridReturnsCorrectGrid() {
                Mockito.when(scorebookRepository.findById(SCOREBOOK_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.scorebook(
                                                SCOREBOOK_ID, CLASS_SUBJECT_ID, ScorebookStatus.OPEN)));
                Mockito.when(classSubjectRepository.findById(CLASS_SUBJECT_ID))
                                .thenReturn(Optional.of(ScoreEntryTestFixtures.classSubject(
                                                CLASS_SUBJECT_ID, CLASS_ID, SUBJECT_ID, SEMESTER_ID)));
                Mockito.when(columnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(SCOREBOOK_ID))
                                .thenReturn(List.of(
                                                ScoreEntryTestFixtures.column(
                                                                COLUMN_ID, SCOREBOOK_ID, AssessmentType.KTTT,
                                                                AssessmentColumnStatus.ACTIVE),
                                                ScoreEntryTestFixtures.column(
                                                                101L, SCOREBOOK_ID, AssessmentType.KTDK,
                                                                AssessmentColumnStatus.INACTIVE)));

                Mockito.when(rosterRepository.findActiveByClassId(Mockito.eq(CLASS_ID), Mockito.any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(ScoreEntryTestFixtures.enrollment(
                                                1L, STUDENT_ID, ACADEMIC_YEAR_ID, CLASS_ID, EnrollmentStatus.ACTIVE))));
                Mockito.when(studentRepository.findAllById(List.of(STUDENT_ID)))
                                .thenReturn(List.of(ScoreEntryTestFixtures.student(
                                                STUDENT_ID, "HS001", StudentStatus.ACTIVE)));
                Mockito.when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(
                                List.of(COLUMN_ID), List.of(STUDENT_ID)))
                                .thenReturn(List.of(ScoreEntryTestFixtures.score(
                                                1L, COLUMN_ID, STUDENT_ID, ScoreStatus.SCORED,
                                                new BigDecimal("9.0"), 1L, LocalDateTime.now())));

                ResStudentScoreGridDTO grid = scoreGridService.getScoreGrid(SCOREBOOK_ID, 0, 10);

                Assertions.assertNotNull(grid, "grid should not be null");
                Assertions.assertEquals(1, grid.columns().size(), "only active columns returned");
                Assertions.assertEquals(1, grid.students().size(), "student count should match roster");
                Assertions.assertEquals("HS001", grid.students().get(0).studentCode(), "student code matches");
                Assertions.assertEquals(
                                new BigDecimal("9.0"),
                                grid.students().get(0).scores().get(COLUMN_ID).scoreValue(),
                                "score value matches");
        }
}

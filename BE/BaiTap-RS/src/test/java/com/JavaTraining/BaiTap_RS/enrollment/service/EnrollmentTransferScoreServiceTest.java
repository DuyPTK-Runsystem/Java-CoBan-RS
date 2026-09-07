package com.JavaTraining.BaiTap_RS.enrollment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferTargetScoreDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferWithScoresDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResEnrollmentMutationDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.scorebook.service.CalculationTaskService;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScorebookGuard;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScoreEntryValidator;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScoreEntryWriter;
import com.JavaTraining.BaiTap_RS.scorebook.service.TranscriptStateService;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EnrollmentTransferScoreServiceTest {

    @Mock private EnrollmentLookupService lookupService;
    @Mock private EnrollmentService enrollmentService;
    @Mock private ClassSubjectRepository classSubjectRepository;
    @Mock private SemesterRepository semesterRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ScorebookRepository scorebookRepository;
    @Mock private AssessmentColumnRepository columnRepository;
    @Mock private StudentScoreRepository scoreRepository;
    @Mock private ScorebookGuard scorebookGuard;
    @Mock private ScoreEntryWriter scoreWriter;
    @Mock private TranscriptStateService transcriptService;
    @Mock private CalculationTaskService taskService;

    private EnrollmentTransferScoreService service;
    private AcademicYear year;
    private SchoolClass sourceClass;
    private SchoolClass targetClass;
    private Semester semester;
    private Student student;
    private StudentYearEnrollment enrollment;
    private AssessmentColumn sourceColumn;
    private AssessmentColumn targetColumn;

    @BeforeEach
    void setUp() {
        EnrollmentTransferScoreDataLoader dataLoader = new EnrollmentTransferScoreDataLoader(
                classSubjectRepository, subjectRepository, scorebookRepository,
                columnRepository, scoreRepository);
        EnrollmentTransferScoreReferenceService referenceService = new EnrollmentTransferScoreReferenceService(
                semesterRepository, columnRepository, scorebookRepository, classSubjectRepository);
        EnrollmentTransferScoreContextService contextService = new EnrollmentTransferScoreContextService(
                lookupService, dataLoader, referenceService);
        EnrollmentTransferScoreAssistService assistService = new EnrollmentTransferScoreAssistService();
        EnrollmentTransferScoreValidationService validationService =
                new EnrollmentTransferScoreValidationService(scorebookGuard, new ScoreEntryValidator());
        service = new EnrollmentTransferScoreService(
                enrollmentService, contextService, assistService, validationService,
                scoreWriter, transcriptService, taskService);
        year = year(10L);
        sourceClass = schoolClass(20L, "6A");
        targetClass = schoolClass(21L, "6B");
        semester = semester(30L);
        student = new Student("Học sinh 40", "HS0040");
        ReflectionTestUtils.setField(student, "id", 40L);
        enrollment = new StudentYearEnrollment(40L, 10L, 20L, EnrollmentStatus.ACTIVE,
                LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(enrollment, "id", 50L);
        sourceColumn = column(500L, 1000L);
        targetColumn = column(600L, 1001L);
        stubCommonContext();
    }

    @Test
    void assistReturnsSourceEvidenceAndStableTargetMapping() {
        stubScoreData();
        StudentScore sourceScore = new StudentScore(500L, 40L, ScoreStatus.SCORED,
                BigDecimal.valueOf(8.5), "source", 1L);
        when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(List.of(500L), List.of(40L)))
                .thenReturn(List.of(sourceScore));
        when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(List.of(600L), List.of(40L)))
                .thenReturn(List.of());

        var result = service.getAssist(50L, 21L, 30L);

        assertTrue(result.hasExistingScores());
        assertEquals(1, result.subjects().size());
        assertEquals(500L, result.subjects().get(0).sourceEvidence().get(0).assessmentColumnId());
        assertEquals(600L, result.subjects().get(0).targetColumns().get(0).assessmentColumnId());
        assertEquals(500L, result.subjects().get(0).targetColumns().get(0).suggestedSourceColumnId());
    }

    @Test
    void mutationRejectsDuplicateTargetColumnsBeforeTransfer() {
        stubScoreData();
        when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(List.of(600L), List.of(40L)))
                .thenReturn(List.of());
        ReqTransferTargetScoreDTO item = new ReqTransferTargetScoreDTO(
                600L, ScoreStatus.SCORED, BigDecimal.valueOf(8.5), null, null);
        ReqTransferWithScoresDTO request = new ReqTransferWithScoresDTO(
                21L, 30L, LocalDateTime.now().minusMinutes(1), "reason", List.of(item, item));

        AppException exception = assertThrows(AppException.class, () -> service.transferWithScores(50L, request));

        assertEquals(400, exception.getStatus().value());
        verify(enrollmentService, never()).transferEnrollment(any(Long.class), any());
    }

    private void stubCommonContext() {
        when(lookupService.findEnrollment(50L)).thenReturn(enrollment);
        when(lookupService.findAcademicYear(10L)).thenReturn(year);
        when(lookupService.findSchoolClass(20L)).thenReturn(sourceClass);
        when(lookupService.findSchoolClass(21L)).thenReturn(targetClass);
        when(lookupService.findStudent(40L)).thenReturn(student);
        when(semesterRepository.findById(30L)).thenReturn(java.util.Optional.of(semester));
    }

    private void stubScoreData() {
        ClassSubject sourceSubject = classSubject(100L, 20L);
        ClassSubject targetSubject = classSubject(101L, 21L);
        when(classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(20L, 30L))
                .thenReturn(List.of(sourceSubject));
        when(classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(21L, 30L))
                .thenReturn(List.of(targetSubject));
        Scorebook sourceBook = new Scorebook(100L, ScorebookStatus.OPEN);
        Scorebook targetBook = new Scorebook(101L, ScorebookStatus.OPEN);
        ReflectionTestUtils.setField(sourceBook, "id", 1000L);
        ReflectionTestUtils.setField(targetBook, "id", 1001L);
        lenient().when(columnRepository.findById(600L)).thenReturn(java.util.Optional.of(targetColumn));
        lenient().when(scorebookRepository.findById(1001L)).thenReturn(java.util.Optional.of(targetBook));
        lenient().when(classSubjectRepository.findById(101L)).thenReturn(java.util.Optional.of(targetSubject));
        when(scorebookRepository.findAllByClassSubjectIdIn(any())).thenAnswer(invocation -> {
            java.util.Collection<Long> ids = invocation.getArgument(0);
            return ids.contains(100L) ? List.of(sourceBook) : List.of(targetBook);
        });
        when(columnRepository.findAllByScorebookIdInOrderByScorebookIdAscAssessmentTypeAscColumnNoAsc(any())
                ).thenAnswer(invocation -> {
            java.util.Collection<Long> ids = invocation.getArgument(0);
            return ids.contains(1000L) ? List.of(sourceColumn) : List.of(targetColumn);
        });
        Subject subject = new Subject("MATH", "Toán", SubjectType.ACADEMIC,
                ApplicationScope.CLASS, SubjectStatus.ACTIVE);
        ReflectionTestUtils.setField(subject, "id", 200L);
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject));
        when(scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(any(), any())).thenAnswer(invocation -> {
            java.util.Collection<Long> ids = invocation.getArgument(0);
            return ids.contains(500L) ? List.of() : List.of();
        });
    }

    private ClassSubject classSubject(Long id, Long classId) {
        ClassSubject result = new ClassSubject(classId, 200L, 30L,
                com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus.ACTIVE);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private AssessmentColumn column(Long id, Long scorebookId) {
        AssessmentColumn result = new AssessmentColumn(scorebookId, AssessmentType.KTTT, 1,
                "KTTT 1", BigDecimal.ONE, false);
        ReflectionTestUtils.setField(result, "id", id);
        result.setStatus(AssessmentColumnStatus.ACTIVE);
        return result;
    }

    private AcademicYear year(Long id) {
        AcademicYear result = new AcademicYear("2026-2027", LocalDate.of(2026, 8, 1),
                LocalDate.of(2027, 5, 31), AcademicYearStatus.ACTIVE, null);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private SchoolClass schoolClass(Long id, String code) {
        SchoolClass result = new SchoolClass(10L, 6L, code, code, 40, SchoolClassStatus.ACTIVE);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private Semester semester(Long id) {
        Semester result = new Semester(10L, "HK1", "Học kỳ 1", 1,
                LocalDate.of(2026, 8, 15), LocalDate.of(2026, 12, 31), null, SemesterStatus.ACTIVE);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}

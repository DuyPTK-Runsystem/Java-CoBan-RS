package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqRejectScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequest;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequestStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreSnapshotStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScoreChangeRequestRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
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
        "PMD.AvoidDuplicateLiterals",
        "PMD.CouplingBetweenObjects",
        "PMD.TooManyMethods"
})
class ScoreChangeRequestServiceTest {

    private static final Long COLUMN_ID = 10L;
    private static final Long STUDENT_ID = 20L;
    private static final Long SCOREBOOK_ID = 30L;
    private static final Long CLASS_SUBJECT_ID = 40L;
    private static final Long SEMESTER_ID = 50L;
    private static final Long YEAR_ID = 60L;

    @Mock
    private ScoreChangeRequestRepository requestRepository;
    @Mock
    private StudentScoreRepository scoreRepository;
    @Mock
    private ScoreChangeRequestContext context;
    @Mock
    private ScoreChangeRequestValidator validator;
    @Mock
    private ScorebookGuard scorebookGuard;
    @Mock
    private TranscriptStateService transcriptService;
    @Mock
    private CalculationTaskService taskService;
    @Mock
    private ScorebookAuditService auditService;
    @Mock
    private StudentLookupService studentLookupService;

    private ScoreChangeRequestService service;

    @BeforeEach
    void setUp() {
        service = new ScoreChangeRequestService(
                requestRepository,
                scoreRepository,
                context,
                validator,
                new ScoreChangeRequestMapper(),
                scorebookGuard,
                transcriptService,
                taskService,
                auditService,
                studentLookupService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRequestStoresSnapshotAndWritesAudit() {
        authenticate(100L, "TEACHER");
        AssessmentColumn column = column();
        Scorebook scorebook = new Scorebook(CLASS_SUBJECT_ID, ScorebookStatus.OPEN);
        ClassSubject classSubject = new ClassSubject(70L, 80L, SEMESTER_ID,
                com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus.ACTIVE);
        Semester semester = semester();
        StudentScore current = ScoreChangeRequestTestFixtures.score(
                300L, COLUMN_ID, STUDENT_ID, ScoreStatus.SCORED, new BigDecimal("7.0"));
        ReqCreateScoreChangeRequestDTO input = new ReqCreateScoreChangeRequestDTO(
                COLUMN_ID, STUDENT_ID, null, ScoreStatus.SCORED, new BigDecimal("8.0"), "Biên bản điều chỉnh");

        Mockito.when(context.findActiveColumn(COLUMN_ID)).thenReturn(column);
        Mockito.when(context.findScorebook(SCOREBOOK_ID)).thenReturn(scorebook);
        Mockito.when(context.findActiveClassSubject(CLASS_SUBJECT_ID)).thenReturn(classSubject);
        Mockito.when(context.findSemester(SEMESTER_ID)).thenReturn(semester);
        Mockito.when(studentLookupService.resolveStudent(STUDENT_ID, null)).thenReturn(student());
        Mockito.when(context.findScore(COLUMN_ID, STUDENT_ID)).thenReturn(Optional.of(current));
        Mockito.when(requestRepository.save(Mockito.any(ScoreChangeRequest.class))).thenAnswer(invocation -> {
            ScoreChangeRequest request = invocation.getArgument(0);
            ReflectionTestUtils.setField(request, "id", 900L);
            return request;
        });

        com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDetailDTO result =
                service.createRequest(input);

        Assertions.assertEquals(900L, result.requestId(), "request id should be returned");
        Assertions.assertEquals(
                ScoreSnapshotStatus.SCORED, result.beforeStatus(), "before status should be snapshotted");
        Assertions.assertEquals(new BigDecimal("7.0"), result.beforeValue(), "before value should be snapshotted");
        Assertions.assertEquals(ScoreChangeRequestStatus.PENDING, result.status(), "new request should be pending");
        Mockito.verify(auditService).writeAudit(
                Mockito.eq("CREATE_SCORE_CHANGE_REQUEST"), Mockito.eq("ScoreChangeRequest"),
                Mockito.eq(900L), Mockito.isNull(), Mockito.anyMap());
    }

    @Test
    void createRequestAcceptsStudentCodeAndReturnsMetadata() {
        authenticate(100L, "TEACHER");
        AssessmentColumn column = column();
        Scorebook scorebook = new Scorebook(CLASS_SUBJECT_ID, ScorebookStatus.OPEN);
        ClassSubject classSubject = new ClassSubject(70L, 80L, SEMESTER_ID,
                com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus.ACTIVE);
        Semester semester = semester();
        Student student = student();
        ReqCreateScoreChangeRequestDTO input = new ReqCreateScoreChangeRequestDTO(
                COLUMN_ID, null, "STU0000020", ScoreStatus.SCORED, new BigDecimal("8.0"),
                "Biên bản điều chỉnh");

        Mockito.when(context.findActiveColumn(COLUMN_ID)).thenReturn(column);
        Mockito.when(context.findScorebook(SCOREBOOK_ID)).thenReturn(scorebook);
        Mockito.when(context.findActiveClassSubject(CLASS_SUBJECT_ID)).thenReturn(classSubject);
        Mockito.when(context.findSemester(SEMESTER_ID)).thenReturn(semester);
        Mockito.when(studentLookupService.resolveStudent(null, "STU0000020")).thenReturn(student);
        Mockito.when(context.findScore(COLUMN_ID, STUDENT_ID)).thenReturn(Optional.empty());
        Mockito.when(requestRepository.save(Mockito.any(ScoreChangeRequest.class))).thenAnswer(invocation -> {
            ScoreChangeRequest request = invocation.getArgument(0);
            ReflectionTestUtils.setField(request, "id", 901L);
            return request;
        });

        com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDetailDTO result =
                service.createRequest(input);

        Assertions.assertEquals("STU0000020", result.studentCode(), "student code should be returned");
        Assertions.assertEquals("Học sinh 20", result.studentName(), "student name should be returned");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findRequestsResolvesStudentCodeFilterAndReturnsMetadata() {
        authenticate(200L, "ACADEMIC_OFFICE");
        ScoreChangeRequest request = ScoreChangeRequestTestFixtures.request(
                900L, COLUMN_ID, STUDENT_ID, 100L,
                ScoreSnapshotStatus.UNSCORED, null,
                ScoreStatus.SCORED, new BigDecimal("8.0"));
        ReqFilterScoreChangeRequestDTO filter = new ReqFilterScoreChangeRequestDTO();
        filter.setStudentCode("STU0000020");
        Mockito.when(studentLookupService.resolveStudent(null, "STU0000020")).thenReturn(student());
        Mockito.when(requestRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(request)));
        Mockito.when(studentLookupService.resolveStudents(List.of(STUDENT_ID), List.of()))
                .thenReturn(List.of(student()));

        Page<com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDTO> result =
                service.findRequests(filter);

        Assertions.assertEquals(STUDENT_ID, filter.getStudentId(), "filter studentId should be resolved");
        Assertions.assertEquals("STU0000020", result.getContent().get(0).studentCode(),
                "student code should be returned in list response");
    }

    @Test
    void approveAndApplyUpdatesScoreAndCreatesCalculationTask() {
        authenticate(200L, "ACADEMIC_OFFICE");
        ScoreChangeRequest request = ScoreChangeRequestTestFixtures.request(
                900L, COLUMN_ID, STUDENT_ID, 100L,
                ScoreSnapshotStatus.SCORED, new BigDecimal("7.0"),
                ScoreStatus.SCORED, new BigDecimal("8.0"));
        StudentScore current = ScoreChangeRequestTestFixtures.score(
                300L, COLUMN_ID, STUDENT_ID, ScoreStatus.SCORED, new BigDecimal("7.0"));
        AssessmentColumn column = column();
        Scorebook scorebook = new Scorebook(CLASS_SUBJECT_ID, ScorebookStatus.PUBLISHED);
        ClassSubject classSubject = new ClassSubject(70L, 80L, SEMESTER_ID,
                com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus.ACTIVE);
        Semester semester = semester();

        Mockito.when(requestRepository.findForUpdate(900L)).thenReturn(Optional.of(request));
        Mockito.when(studentLookupService.resolveStudent(STUDENT_ID, null)).thenReturn(student());
        Mockito.when(context.findColumn(COLUMN_ID)).thenReturn(column);
        Mockito.when(context.findScorebook(SCOREBOOK_ID)).thenReturn(scorebook);
        Mockito.when(context.findActiveClassSubject(CLASS_SUBJECT_ID)).thenReturn(classSubject);
        Mockito.when(context.findSemester(SEMESTER_ID)).thenReturn(semester);
        Mockito.when(context.findScore(COLUMN_ID, STUDENT_ID)).thenReturn(Optional.of(current));
        Mockito.when(scoreRepository.save(current)).thenReturn(current);
        Mockito.when(requestRepository.save(request)).thenReturn(request);
        Mockito.when(transcriptService.touchTranscripts(STUDENT_ID, YEAR_ID, SEMESTER_ID)).thenReturn(2L);

        com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDetailDTO result =
                service.approveAndApply(900L);

        Assertions.assertEquals(
                ScoreChangeRequestStatus.APPLIED, result.status(), "approved request should be applied");
        Assertions.assertEquals(ScoreStatus.SCORED, current.getScoreStatus(), "score status should be updated");
        Assertions.assertEquals(new BigDecimal("8.0"), current.getScoreValue(), "score value should be updated");
        Assertions.assertEquals(200L, request.getReviewedBy(), "reviewer should be stored");
        Mockito.verify(taskService).ensureRecalcTask(STUDENT_ID, YEAR_ID, 2L);
        Mockito.verify(auditService).writeAudit(
                Mockito.eq("APPROVE_AND_APPLY_SCORE_CHANGE_REQUEST"), Mockito.eq("ScoreChangeRequest"),
                Mockito.eq(900L), Mockito.anyMap(), Mockito.anyMap());
    }

    @Test
    void rejectRequestStoresReason() {
        authenticate(200L, "ACADEMIC_OFFICE");
        ScoreChangeRequest request = ScoreChangeRequestTestFixtures.request(
                900L, COLUMN_ID, STUDENT_ID, 100L,
                ScoreSnapshotStatus.SCORED, new BigDecimal("7.0"),
                ScoreStatus.SCORED, new BigDecimal("8.0"));
        Mockito.when(requestRepository.findForUpdate(900L)).thenReturn(Optional.of(request));
        Mockito.when(studentLookupService.resolveStudent(STUDENT_ID, null)).thenReturn(student());
        Mockito.when(requestRepository.save(request)).thenReturn(request);

        com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDetailDTO result =
                service.rejectRequest(900L, new ReqRejectScoreChangeRequestDTO("Không đủ hồ sơ"));

        Assertions.assertEquals(ScoreChangeRequestStatus.REJECTED, result.status(), "request should be rejected");
        Assertions.assertEquals("Không đủ hồ sơ", result.rejectionReason(), "rejection reason should be stored");
        Mockito.verify(auditService).writeAudit(
                Mockito.eq("REJECT_SCORE_CHANGE_REQUEST"), Mockito.eq("ScoreChangeRequest"),
                Mockito.eq(900L), Mockito.anyMap(), Mockito.anyMap());
    }

    @Test
    void requesterCanCancelPendingRequest() {
        authenticate(100L, "TEACHER");
        ScoreChangeRequest request = ScoreChangeRequestTestFixtures.request(
                900L, COLUMN_ID, STUDENT_ID, 100L,
                ScoreSnapshotStatus.UNSCORED, null,
                ScoreStatus.SCORED, new BigDecimal("8.0"));
        Mockito.when(requestRepository.findForUpdate(900L)).thenReturn(Optional.of(request));
        Mockito.when(studentLookupService.resolveStudent(STUDENT_ID, null)).thenReturn(student());
        Mockito.when(requestRepository.save(request)).thenReturn(request);

        com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDetailDTO result =
                service.cancelRequest(900L);

        Assertions.assertEquals(ScoreChangeRequestStatus.CANCELLED, result.status(), "request should be cancelled");
    }

    private AssessmentColumn column() {
        AssessmentColumn column = new AssessmentColumn(
                SCOREBOOK_ID, AssessmentType.KTTT, 1, "KTTT", new BigDecimal("1.0"), true);
        ReflectionTestUtils.setField(column, "id", COLUMN_ID);
        column.setStatus(AssessmentColumnStatus.ACTIVE);
        return column;
    }

    private Semester semester() {
        Semester semester = new Semester(
                YEAR_ID, "HK1", "Học kỳ 1", 1,
                java.time.LocalDate.of(2026, 8, 1), java.time.LocalDate.of(2026, 12, 31),
                null, SemesterStatus.LOCKED);
        ReflectionTestUtils.setField(semester, "id", SEMESTER_ID);
        return semester;
    }

    private Student student() {
        Student student = new Student("Học sinh 20", "STU0000020");
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);
        return student;
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

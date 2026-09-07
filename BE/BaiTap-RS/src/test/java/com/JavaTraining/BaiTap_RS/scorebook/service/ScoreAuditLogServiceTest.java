package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterScoreAuditLogDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreAuditLogDTO;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals",
        "PMD.AvoidUsingHardCodedIP",
        "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.UnitTestContainsTooManyAsserts"
})
class ScoreAuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentLookupService studentLookupService;
    @Mock
    private TranscriptAccessGuard accessGuard;
    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private ClassSubjectRepository classSubjectRepository;
    @Mock
    private ScorebookRepository scorebookRepository;
    @Mock
    private AssessmentColumnRepository assessmentColumnRepository;

    private ScoreAuditLogService service;

    @BeforeEach
    void setUp() {
        service = new ScoreAuditLogService(
                auditLogRepository,
                userRepository,
                new ObjectMapper(),
                studentLookupService,
                accessGuard,
                semesterRepository,
                classSubjectRepository,
                scorebookRepository,
                assessmentColumnRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filtersAndParsesAuditDataAndResolvesActorName() {
        AuditLog log = new AuditLog(
                7L,
                "STUDENT_SCORE_UPDATED",
                "student_score",
                "99",
                "{\"studentId\":200,\"scoreValue\":7.0}",
                "{\"studentId\":200,\"scoreValue\":8.0}",
                "request-1",
                "127.0.0.1");
        ReflectionTestUtils.setField(log, "id", 99L);
        ReflectionTestUtils.setField(log, "occurredAt", LocalDateTime.of(2026, 8, 26, 10, 0));
        User actor = Mockito.mock(User.class);
        Mockito.when(actor.getId()).thenReturn(7L);
        Mockito.when(actor.getUsername()).thenReturn("academic-office");
        Mockito.when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(actor));
        Mockito.when(auditLogRepository.findAll(Mockito.<Specification<AuditLog>>any(), Mockito.any(Sort.class)))
                .thenReturn(List.of(log));

        ReqFilterScoreAuditLogDTO filter = new ReqFilterScoreAuditLogDTO();
        filter.setStudentId(200L);
        filter.setAction("STUDENT_SCORE_UPDATED");

        List<ResScoreAuditLogDTO> response = service.findLogs(filter).getContent();

        Assertions.assertEquals(1, response.size());
        Assertions.assertEquals("academic-office", response.get(0).actorUsername());
        Assertions.assertEquals(8.0, response.get(0).afterData().get("scoreValue").doubleValue());
        Assertions.assertEquals("request-1", response.get(0).requestId());
    }

    @Test
    void resolvesStudentCodeBeforeQueryingLogs() {
        Student student = new Student("Học sinh", "HS200");
        ReflectionTestUtils.setField(student, "id", 200L);
        Mockito.when(studentLookupService.resolveStudent(null, "HS200")).thenReturn(student);
        Mockito.when(auditLogRepository.findAll(Mockito.<Specification<AuditLog>>any(), Mockito.any(Sort.class)))
                .thenReturn(List.of());

        ReqFilterScoreAuditLogDTO filter = new ReqFilterScoreAuditLogDTO();
        filter.setStudentCode("HS200");

        Assertions.assertTrue(service.findLogs(filter).isEmpty());
        Mockito.verify(studentLookupService).resolveStudent(null, "HS200");
    }
}

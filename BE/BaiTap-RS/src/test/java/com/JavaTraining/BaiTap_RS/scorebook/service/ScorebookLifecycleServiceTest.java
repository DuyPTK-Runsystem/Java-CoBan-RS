package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.SkillWeightConfigRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScorebookLifecycleServiceTest {

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private ScorebookRepository scorebookRepository;

    @Mock
    private AssessmentColumnRepository columnRepository;

    @Mock
    private SkillWeightConfigRepository weightRepository;

    @Mock
    private ScorebookGuard scorebookGuard;

    @Mock
    private ScorebookAuditService auditService;

    private ScorebookLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        ScorebookContext context = new ScorebookContext(
                classSubjectRepository,
                subjectRepository,
                semesterRepository,
                scorebookRepository);
        ScorebookResponseService responseService = new ScorebookResponseService(
                columnRepository,
                weightRepository,
                new ScorebookMapper());
        lifecycleService = new ScorebookLifecycleService(
                context,
                scorebookRepository,
                columnRepository,
                weightRepository,
                scorebookGuard,
                auditService,
                new ScorebookAuditDataMapper(),
                responseService,
                new ScorebookConfigurationValidator());
    }

    @Test
    void createScorebookDefaultsToDraft() {
        Mockito.when(classSubjectRepository.findById(20L)).thenReturn(Optional.of(
                ScorebookTestFixtures.classSubject(ClassSubjectStatus.ACTIVE)));
        Mockito.when(subjectRepository.findById(70L)).thenReturn(Optional.of(
                ScorebookTestFixtures.subject(SubjectType.ACADEMIC)));
        Mockito.when(semesterRepository.findById(80L)).thenReturn(Optional.of(
                ScorebookTestFixtures.semester(SemesterStatus.ACTIVE)));
        Mockito.when(scorebookRepository.existsByClassSubjectId(20L)).thenReturn(false);
        Mockito.when(scorebookRepository.save(Mockito.any(Scorebook.class))).thenAnswer(invocation -> {
            Scorebook scorebook = invocation.getArgument(0);
            ReflectionTestUtils.setField(scorebook, "id", 90L);
            return scorebook;
        });
        Mockito.when(columnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(90L))
                .thenReturn(List.of());
        Mockito.when(weightRepository.findByScorebookId(90L)).thenReturn(Optional.empty());

        com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO response =
                lifecycleService.createScorebook(new ReqCreateScorebookDTO(20L));

        Assertions.assertEquals(ScorebookStatus.DRAFT, response.status(), "new scorebook should start as draft");
    }

    @Test
    void createScorebookRejectsDuplicateClassSubject() {
        Mockito.when(classSubjectRepository.findById(20L)).thenReturn(Optional.of(
                ScorebookTestFixtures.classSubject(ClassSubjectStatus.ACTIVE)));
        Mockito.when(subjectRepository.findById(70L)).thenReturn(Optional.of(
                ScorebookTestFixtures.subject(SubjectType.ACADEMIC)));
        Mockito.when(semesterRepository.findById(80L)).thenReturn(Optional.of(
                ScorebookTestFixtures.semester(SemesterStatus.ACTIVE)));
        Mockito.when(scorebookRepository.existsByClassSubjectId(20L)).thenReturn(true);

        assertConflict(() -> lifecycleService.createScorebook(new ReqCreateScorebookDTO(20L)));
    }

    @Test
    void publishRegularScorebookRequiresPeriodicAndFinalColumns() {
        Scorebook scorebook = ScorebookTestFixtures.scorebook(ScorebookStatus.OPEN);
        Mockito.when(scorebookRepository.findById(90L)).thenReturn(Optional.of(scorebook));
        Mockito.when(classSubjectRepository.findById(20L)).thenReturn(Optional.of(
                ScorebookTestFixtures.classSubject(ClassSubjectStatus.ACTIVE)));
        Mockito.when(subjectRepository.findById(70L)).thenReturn(Optional.of(
                ScorebookTestFixtures.subject(SubjectType.ACADEMIC)));
        Mockito.when(columnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(90L))
                .thenReturn(List.of(
                        ScorebookTestFixtures.column(91L, AssessmentType.KTDK, AssessmentColumnStatus.ACTIVE),
                        ScorebookTestFixtures.column(92L, AssessmentType.KTCK, AssessmentColumnStatus.ACTIVE)));
        Mockito.when(weightRepository.findByScorebookId(90L)).thenReturn(Optional.empty());

        com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO response =
                lifecycleService.publishScorebook(90L);

        Assertions.assertEquals(
                ScorebookStatus.PUBLISHED,
                response.status(),
                "valid columns should publish the scorebook");
    }

    private void assertConflict(Runnable action) {
        com.JavaTraining.BaiTap_RS.common.error.AppException exception = Assertions.assertThrows(
                com.JavaTraining.BaiTap_RS.common.error.AppException.class,
                action::run);
        Assertions.assertEquals(
                org.springframework.http.HttpStatus.CONFLICT,
                exception.getStatus(),
                "duplicate scorebook should be a conflict");
    }
}

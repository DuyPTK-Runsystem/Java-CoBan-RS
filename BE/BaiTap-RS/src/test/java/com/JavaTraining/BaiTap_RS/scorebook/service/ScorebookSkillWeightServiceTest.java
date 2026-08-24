package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertSkillWeightConfigDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.SkillWeightConfigRepository;
import static com.JavaTraining.BaiTap_RS.scorebook.service.ScorebookTestFixtures.classSubject;
import static com.JavaTraining.BaiTap_RS.scorebook.service.ScorebookTestFixtures.scorebook;
import static com.JavaTraining.BaiTap_RS.scorebook.service.ScorebookTestFixtures.subject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScorebookSkillWeightServiceTest {

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

    private ScorebookSkillWeightService skillWeightService;

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
        skillWeightService = new ScorebookSkillWeightService(
                context,
                weightRepository,
                scorebookGuard,
                new ScorebookConfigurationValidator(),
                auditService,
                new ScorebookAuditDataMapper(),
                responseService);
    }

    @Test
    void skillWeightRejectsTotalDifferentFromOneHundred() {
        Mockito.when(scorebookRepository.findById(90L))
                .thenReturn(Optional.of(scorebook(ScorebookStatus.OPEN)));
        Mockito.when(classSubjectRepository.findById(20L))
                .thenReturn(Optional.of(classSubject(ClassSubjectStatus.ACTIVE)));
        Mockito.when(subjectRepository.findById(70L))
                .thenReturn(Optional.of(subject(SubjectType.SKILL)));

        assertConflict(() -> skillWeightService.upsertSkillWeight(
                90L,
                new ReqUpsertSkillWeightConfigDTO(
                        new BigDecimal("20"),
                        new BigDecimal("20"),
                        new BigDecimal("20"))));
    }

    private void assertConflict(Runnable action) {
        com.JavaTraining.BaiTap_RS.common.error.AppException exception = Assertions.assertThrows(
                com.JavaTraining.BaiTap_RS.common.error.AppException.class,
                action::run);
        Assertions.assertEquals(
                org.springframework.http.HttpStatus.CONFLICT,
                exception.getStatus(),
                "invalid skill weights should be rejected");
    }
}

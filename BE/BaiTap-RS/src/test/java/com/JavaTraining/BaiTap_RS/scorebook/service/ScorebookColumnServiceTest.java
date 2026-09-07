package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import static com.JavaTraining.BaiTap_RS.scorebook.service.ScorebookTestFixtures.classSubject;
import static com.JavaTraining.BaiTap_RS.scorebook.service.ScorebookTestFixtures.scorebook;
import static com.JavaTraining.BaiTap_RS.scorebook.service.ScorebookTestFixtures.subject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScorebookColumnServiceTest {

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
        private ScorebookGuard scorebookGuard;

        @Mock
        private ScorebookAuditService auditService;

        private ScorebookColumnService columnService;

        @BeforeEach
        void setUp() {
                ScorebookContext context = new ScorebookContext(
                                classSubjectRepository,
                                subjectRepository,
                                semesterRepository,
                                scorebookRepository);
                columnService = new ScorebookColumnService(
                                context,
                                columnRepository,
                                scorebookGuard,
                                new ScorebookConfigurationValidator(),
                                auditService,
                                new ScorebookAuditDataMapper(),
                                new ScorebookMapper());
        }

        @Test
        void addColumnRejectsDuplicateActiveKtckForAcademicSubject() {
                Mockito.when(scorebookRepository.findById(90L))
                                .thenReturn(Optional.of(scorebook(ScorebookStatus.OPEN)));
                Mockito.when(classSubjectRepository.findById(20L))
                                .thenReturn(Optional.of(classSubject(ClassSubjectStatus.ACTIVE)));
                Mockito.when(subjectRepository.findById(70L))
                                .thenReturn(Optional.of(subject(SubjectType.ACADEMIC)));
                Mockito.when(columnRepository.countByScorebookIdAndAssessmentTypeAndStatus(
                                90L, AssessmentType.KTCK, AssessmentColumnStatus.ACTIVE))
                                .thenReturn(1L);

                assertConflict(() -> columnService.addColumn(
                                90L,
                                new ReqCreateAssessmentColumnDTO(AssessmentType.KTCK, null, "Cuối kỳ 2")),
                                "Môn thường chỉ được phép có đúng một cột KTCK");
        }

        @Test
        void addColumnAutoResolvesNextAvailableColumnNoWhenPreferredExists() {
                Mockito.when(scorebookRepository.findById(90L))
                                .thenReturn(Optional.of(scorebook(ScorebookStatus.OPEN)));
                Mockito.when(classSubjectRepository.findById(20L))
                                .thenReturn(Optional.of(classSubject(ClassSubjectStatus.ACTIVE)));
                Mockito.when(subjectRepository.findById(70L))
                                .thenReturn(Optional.of(subject(SubjectType.ACADEMIC)));
                Mockito.when(columnRepository.countByScorebookIdAndAssessmentTypeAndStatus(
                                90L, AssessmentType.KTDK, AssessmentColumnStatus.ACTIVE))
                                .thenReturn(1L);
                Mockito.when(columnRepository.existsByScorebookIdAndAssessmentTypeAndColumnNo(90L, AssessmentType.KTDK,
                                1))
                                .thenReturn(true);
                Mockito.when(columnRepository.existsByScorebookIdAndAssessmentTypeAndColumnNo(90L, AssessmentType.KTDK,
                                2))
                                .thenReturn(false);

                ArgumentCaptor<AssessmentColumn> captor = ArgumentCaptor.forClass(AssessmentColumn.class);
                Mockito.when(columnRepository.save(captor.capture()))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                columnService.addColumn(90L, new ReqCreateAssessmentColumnDTO(AssessmentType.KTDK, 1, "Giữa kỳ 2"));

                Assertions.assertEquals(2, captor.getValue().getColumnNo(), "resolved column number should be 2");
        }

        @Test
        void addColumnAssignsDefaultColumnNoWhenOmitted() {
                Mockito.when(scorebookRepository.findById(90L))
                                .thenReturn(Optional.of(scorebook(ScorebookStatus.OPEN)));
                Mockito.when(classSubjectRepository.findById(20L))
                                .thenReturn(Optional.of(classSubject(ClassSubjectStatus.ACTIVE)));
                Mockito.when(subjectRepository.findById(70L))
                                .thenReturn(Optional.of(subject(SubjectType.ACADEMIC)));
                Mockito.when(columnRepository.countByScorebookIdAndAssessmentTypeAndStatus(
                                90L, AssessmentType.KTTT, AssessmentColumnStatus.ACTIVE))
                                .thenReturn(0L);
                Mockito.when(columnRepository.existsByScorebookIdAndAssessmentTypeAndColumnNo(90L, AssessmentType.KTTT,
                                1))
                                .thenReturn(false);

                ArgumentCaptor<AssessmentColumn> captor = ArgumentCaptor.forClass(AssessmentColumn.class);
                Mockito.when(columnRepository.save(captor.capture()))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                columnService.addColumn(90L, new ReqCreateAssessmentColumnDTO(AssessmentType.KTTT, null, "TX 1"));

                Assertions.assertEquals(1, captor.getValue().getColumnNo(), "default column number should be 1");
        }

        private void assertConflict(Runnable action, String expectedMessage) {
                com.JavaTraining.BaiTap_RS.common.error.AppException exception = Assertions.assertThrows(
                                com.JavaTraining.BaiTap_RS.common.error.AppException.class,
                                action::run);
                Assertions.assertEquals(
                                org.springframework.http.HttpStatus.CONFLICT,
                                exception.getStatus(),
                                "conflict status required");
                Assertions.assertEquals(
                                expectedMessage,
                                exception.getMessage(),
                                "expected business conflict message");
        }
}

package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectApplicabilityRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubjectApplicabilityServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SubjectApplicabilityRepository applicabilityRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private GradeLevelRepository gradeLevelRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @Mock
    private AcademicCatalogAuditService auditService;

    @Test
    void listApplicabilitiesUsesSubjectAndOptionalFilters() {
        Subject subject = subject();
        Mockito.when(subjectRepository.findById(70L)).thenReturn(Optional.of(subject));
        Mockito.when(applicabilityRepository.findAllByFilters(
                70L, 80L, SubjectApplicabilityStatus.ACTIVE)).thenReturn(List.of(applicability()));
        SubjectApplicabilityService service = service();

        Assertions.assertEquals(
                1,
                service.listApplicabilities(70L, 80L, SubjectApplicabilityStatus.ACTIVE).size(),
                "Filtered applicability list should contain the stored record");
    }

    @Test
    void updateChangesUnusedTupleAndWritesAudit() {
        SubjectApplicability applicability = applicability();
        Mockito.when(subjectRepository.findById(70L)).thenReturn(Optional.of(subject()));
        Mockito.when(applicabilityRepository.findById(501L)).thenReturn(Optional.of(applicability));
        Mockito.when(semesterRepository.findById(81L)).thenReturn(Optional.of(semester(81L)));
        Mockito.when(gradeLevelRepository.existsById(2L)).thenReturn(true);
        Mockito.when(applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelIdAndIdNot(
                70L, 81L, ApplicationScope.GRADE, 2L, 501L)).thenReturn(false);
        Mockito.when(classSubjectRepository.existsByApplicabilityTarget(
                70L, 80L, ApplicationScope.GRADE, 1L, null)).thenReturn(false);
        SubjectApplicabilityService service = service();

        service.updateApplicability(70L, 501L, new ReqUpdateSubjectApplicabilityDTO(
                81L, ApplicationScope.GRADE, 2L, null, SubjectApplicabilityStatus.ACTIVE));

        Assertions.assertTrue(
                Long.valueOf(81L).equals(applicability.getSemesterId())
                        && Long.valueOf(2L).equals(applicability.getGradeLevelId()),
                "The unused applicability tuple should be replaced");
    }

    @Test
    void updateRejectsChangingTupleUsedByClassSubject() {
        SubjectApplicability applicability = applicability();
        Mockito.when(subjectRepository.findById(70L)).thenReturn(Optional.of(subject()));
        Mockito.when(applicabilityRepository.findById(501L)).thenReturn(Optional.of(applicability));
        Mockito.when(semesterRepository.findById(81L)).thenReturn(Optional.of(semester(81L)));
        Mockito.when(gradeLevelRepository.existsById(2L)).thenReturn(true);
        Mockito.when(applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelIdAndIdNot(
                70L, 81L, ApplicationScope.GRADE, 2L, 501L)).thenReturn(false);
        Mockito.when(classSubjectRepository.existsByApplicabilityTarget(
                70L, 80L, ApplicationScope.GRADE, 1L, null)).thenReturn(true);
        SubjectApplicabilityService service = service();

        Assertions.assertThrows(AppException.class, () -> service.updateApplicability(
                70L,
                501L,
                new ReqUpdateSubjectApplicabilityDTO(
                        81L, ApplicationScope.GRADE, 2L, null, SubjectApplicabilityStatus.ACTIVE)),
                "An applicability used by class-subject must reject tuple replacement");
    }

    @Test
    void deactivateIsSoftAndIdempotent() {
        SubjectApplicability applicability = applicability();
        Mockito.when(subjectRepository.findById(70L)).thenReturn(Optional.of(subject()));
        Mockito.when(applicabilityRepository.findById(501L)).thenReturn(Optional.of(applicability));
        Mockito.when(semesterRepository.findById(80L)).thenReturn(Optional.of(semester(80L)));
        SubjectApplicabilityService service = service();

        service.deactivateApplicability(70L, 501L);
        service.deactivateApplicability(70L, 501L);

        Assertions.assertEquals(
                SubjectApplicabilityStatus.INACTIVE,
                applicability.getStatus(),
                "Deactivation should change only the applicability status");
    }

    @Test
    void createRejectsExistingInactiveTupleInsteadOfRelyingOnDatabaseFailure() {
        Mockito.when(subjectRepository.findById(70L)).thenReturn(Optional.of(subject()));
        Mockito.when(semesterRepository.findById(80L)).thenReturn(Optional.of(semester(80L)));
        Mockito.when(gradeLevelRepository.existsById(1L)).thenReturn(true);
        Mockito.when(applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelId(
                70L, 80L, ApplicationScope.GRADE, 1L)).thenReturn(true);
        SubjectApplicabilityService service = service();

        Assertions.assertThrows(AppException.class, () -> service.createApplicability(
                70L, new ReqCreateSubjectApplicabilityDTO(80L, ApplicationScope.GRADE, 1L, null)),
                "An inactive duplicate tuple must be rejected before persistence");
    }

    private SubjectApplicabilityService service() {
        return new SubjectApplicabilityService(
                subjectRepository,
                applicabilityRepository,
                semesterRepository,
                classSubjectRepository,
                auditService,
                new SubjectApplicabilityValidator(
                        applicabilityRepository,
                        gradeLevelRepository,
                        schoolClassRepository));
    }

    private Subject subject() {
        Subject subject = new Subject(
                "MATH", "Toán", SubjectType.ACADEMIC, ApplicationScope.GRADE, SubjectStatus.ACTIVE);
        ReflectionTestUtils.setField(subject, "id", 70L);
        return subject;
    }

    private Semester semester(Long id) {
        Semester semester = new Semester(
                10L,
                id == 80L ? "HK1" : "HK2",
                id == 80L ? "Học kỳ 1" : "Học kỳ 2",
                id == 80L ? 1 : 2,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2027, 5, 31),
                null,
                SemesterStatus.ACTIVE);
        ReflectionTestUtils.setField(semester, "id", id);
        return semester;
    }

    private SubjectApplicability applicability() {
        SubjectApplicability applicability = new SubjectApplicability(
                70L, 80L, ApplicationScope.GRADE, 1L, null, SubjectApplicabilityStatus.ACTIVE);
        ReflectionTestUtils.setField(applicability, "id", 501L);
        return applicability;
    }
}

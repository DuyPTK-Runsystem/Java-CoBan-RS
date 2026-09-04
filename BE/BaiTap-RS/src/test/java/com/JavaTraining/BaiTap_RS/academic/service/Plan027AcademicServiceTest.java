package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateClassSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterCompletenessDecisionDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.ExcessiveImports")
class Plan027AcademicServiceTest {

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private SubjectApplicabilityRepository applicabilityRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private SemesterLockService semesterLockService;

    @Mock
    private SemesterCompletenessService completenessService;

    @Test
    void createClassSubjectRejectsMissingApplicability() {
        ClassSubjectService service = new ClassSubjectService(
                classSubjectRepository,
                schoolClassRepository,
                subjectRepository,
                semesterRepository,
                applicabilityRepository);
        Mockito.when(schoolClassRepository.findById(20L)).thenReturn(Optional.of(schoolClass()));
        Mockito.when(subjectRepository.findById(70L)).thenReturn(Optional.of(subject()));
        Mockito.when(semesterRepository.findById(80L)).thenReturn(Optional.of(semester()));
        Mockito.when(classSubjectRepository.existsByClassIdAndSubjectIdAndSemesterId(20L, 70L, 80L))
                .thenReturn(false);
        Mockito.when(applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelIdAndStatus(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any())).thenReturn(false);

        assertConflict(() -> service.createClassSubject(new ReqCreateClassSubjectDTO(
                20L,
                70L,
                80L,
                ClassSubjectStatus.ACTIVE)));
    }

    @Test
    void semesterCompletenessDecisionTriggersListedCheckpoint() {
        SemesterService service = new SemesterService(
                semesterRepository,
                academicYearRepository,
                new SemesterMapper(),
                semesterLockService,
                completenessService);
        Semester semester = semester();
        semester.setAutomaticLockAt(LocalDateTime.of(2027, 2, 14, 0, 0));
        Mockito.when(semesterRepository.findById(80L)).thenReturn(Optional.of(semester));
        Mockito.when(completenessService.evaluateCompleteness(80L))
                .thenReturn(new SemesterCompletenessSummaryDTO(
                        false, 1, 0, 0, 0, 0, 0, 0, List.of("Thiếu cột")));

        ResSemesterCompletenessDecisionDTO checkpoint = service.evaluateCompletenessCheckpoint(
                80L,
                LocalDate.of(2027, 2, 7));

        Assertions.assertEquals("NEEDS_NOTIFICATION", checkpoint.decision(), "listed checkpoint should notify");
    }

    @Test
    void semesterCompletenessDecisionSkipsOffScheduleDate() {
        SemesterService service = new SemesterService(
                semesterRepository,
                academicYearRepository,
                new SemesterMapper(),
                semesterLockService,
                completenessService);
        Semester semester = semester();
        semester.setAutomaticLockAt(LocalDateTime.of(2027, 2, 14, 0, 0));
        Mockito.when(semesterRepository.findById(80L)).thenReturn(Optional.of(semester));

        ResSemesterCompletenessDecisionDTO offSchedule = service.evaluateCompletenessCheckpoint(
                80L,
                LocalDate.of(2027, 2, 10));

        Assertions.assertEquals("NO_NOTIFICATION", offSchedule.decision(), "off schedule date should not notify");
    }

    private void assertConflict(Runnable action) {
        AppException exception = Assertions.assertThrows(AppException.class, action::run, "expected conflict");
        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus(), "business guard should return conflict");
    }

    private SchoolClass schoolClass() {
        SchoolClass schoolClass = new SchoolClass(
                10L,
                6L,
                "6A",
                "6A",
                40,
                SchoolClassStatus.ACTIVE);
        ReflectionTestUtils.setField(schoolClass, "id", 20L);
        return schoolClass;
    }

    private Subject subject() {
        Subject subject = new Subject(
                "MATH",
                "Toán",
                SubjectType.ACADEMIC,
                ApplicationScope.GRADE,
                SubjectStatus.ACTIVE);
        ReflectionTestUtils.setField(subject, "id", 70L);
        return subject;
    }

    private Semester semester() {
        Semester semester = new Semester(
                10L,
                "HK1",
                "Học kỳ 1",
                1,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 12, 31),
                null,
                SemesterStatus.ACTIVE);
        ReflectionTestUtils.setField(semester, "id", 80L);
        return semester;
    }
}

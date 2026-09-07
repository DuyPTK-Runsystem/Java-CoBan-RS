package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResTranscriptCalculationStatusDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.UnitTestContainsTooManyAsserts"
})
class TranscriptCalculationStatusServiceTest {

    private static final Long STUDENT_ID = 200L;
    private static final Long ACADEMIC_YEAR_ID = 10L;

    @Mock
    private StudentAnnualTranscriptRepository annualTranscriptRepository;
    @Mock
    private StudentTermTranscriptRepository termTranscriptRepository;
    @Mock
    private StudentSubjectTermResultRepository termResultRepository;
    @Mock
    private StudentSubjectAnnualResultRepository annualResultRepository;
    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private TranscriptAccessGuard accessGuard;
    @Mock
    private TranscriptTermResponseMapper termResponseMapper;
    @Mock
    private TranscriptResponseSupport responseSupport;
    @Mock
    private TranscriptCurrentStudentResolver currentStudentResolver;
    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private TranscriptQueryService service;

    @Test
    void reportsInProgressAsNotUpToDate() {
        StudentAnnualTranscript annual = annual(CalculationStatus.IN_PROGRESS, 5L, 4L);
        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.of(annual));
        Mockito.when(semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(ACADEMIC_YEAR_ID))
                .thenReturn(List.of());
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));

        ResTranscriptCalculationStatusDTO response = service.getAnnualCalculationStatus(STUDENT_ID, ACADEMIC_YEAR_ID);

        Assertions.assertEquals(CalculationStatus.IN_PROGRESS, response.calculationStatus());
        Assertions.assertFalse(response.isUpToDate());
        Assertions.assertEquals(5L, response.sourceVersion());
        Assertions.assertEquals(4L, response.calculatedVersion());
    }

    @Test
    void reportsFinishAsUpToDateOnlyWhenVersionsMatch() {
        StudentAnnualTranscript annual = annual(CalculationStatus.FINISH, 4L, 4L);
        Mockito.when(annualTranscriptRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, ACADEMIC_YEAR_ID))
                .thenReturn(Optional.of(annual));
        Mockito.when(semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(ACADEMIC_YEAR_ID))
                .thenReturn(List.of(Mockito.mock(Semester.class)));
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));

        ResTranscriptCalculationStatusDTO response = service.getAnnualCalculationStatus(STUDENT_ID, ACADEMIC_YEAR_ID);

        Assertions.assertTrue(response.isUpToDate());
        Assertions.assertEquals("HS200", response.studentCode());
    }

    private static StudentAnnualTranscript annual(CalculationStatus status, long source, long calculated) {
        StudentAnnualTranscript annual = new StudentAnnualTranscript(STUDENT_ID, ACADEMIC_YEAR_ID);
        annual.setCalculationStatus(status);
        annual.setSourceVersion(source);
        annual.setCalculatedVersion(calculated);
        return annual;
    }

    private static Student student() {
        Student student = new Student("Học sinh", "HS200");
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);
        return student;
    }
}

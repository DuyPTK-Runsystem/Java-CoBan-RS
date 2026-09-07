package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
                "PMD.UnitTestAssertionsShouldIncludeMessage",
                "PMD.UnitTestContainsTooManyAsserts"
})
class ClassTranscriptQueryServiceTest {

        private static final Long CLASS_ID = 1L;
        private static final Long YEAR_ID = 10L;
        private static final Long SEMESTER_ID = 100L;
        private static final Long STUDENT_ID = 1000L;

        @Mock
        private SchoolClassRepository schoolClassRepository;
        @Mock
        private SemesterRepository semesterRepository;
        @Mock
        private StudentYearEnrollmentRepository enrollmentRepository;
        @Mock
        private StudentRepository studentRepository;
        @Mock
        private StudentTermTranscriptRepository termTranscriptRepository;
        @Mock
        private StudentAnnualTranscriptRepository annualTranscriptRepository;
        @Mock
        private StudentSubjectTermResultRepository termResultRepository;
        @Mock
        private StudentSubjectAnnualResultRepository annualResultRepository;
        @Mock
        private TranscriptAccessGuard accessGuard;
        @Mock
        private TranscriptTermResponseMapper termMapper;
        @Mock
        private TranscriptResponseSupport responseSupport;

        private ClassTranscriptQueryService service;

        @BeforeEach
        void setUp() {
                service = new ClassTranscriptQueryService(
                                schoolClassRepository, semesterRepository, enrollmentRepository,
                                studentRepository, termTranscriptRepository, annualTranscriptRepository,
                                termResultRepository, annualResultRepository, accessGuard,
                                termMapper, responseSupport);
        }

        @Test
        void getClassTermTranscriptThrowsWhenClassNotFound() {
                Mockito.when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> service.getClassTermTranscript(CLASS_ID, SEMESTER_ID));
                Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }

        @Test
        void getClassTermTranscriptThrowsWhenSemesterYearMismatch() {
                SchoolClass schoolClass = Mockito.mock(SchoolClass.class);
                Mockito.when(schoolClass.getAcademicYearId()).thenReturn(YEAR_ID);
                Mockito.when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));

                Semester semester = Mockito.mock(Semester.class);
                Mockito.when(semester.getAcademicYearId()).thenReturn(999L);
                Mockito.when(semesterRepository.findById(SEMESTER_ID)).thenReturn(Optional.of(semester));

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> service.getClassTermTranscript(CLASS_ID, SEMESTER_ID));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        void getClassTermTranscriptReturnsEmptyWhenNoStudents() {
                SchoolClass schoolClass = Mockito.mock(SchoolClass.class);
                Mockito.when(schoolClass.getAcademicYearId()).thenReturn(YEAR_ID);
                Mockito.when(schoolClass.getClassCode()).thenReturn("10A1");
                Mockito.when(schoolClass.getClassName()).thenReturn("Lớp 10A1");
                Mockito.when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));

                Semester semester = Mockito.mock(Semester.class);
                Mockito.when(semester.getAcademicYearId()).thenReturn(YEAR_ID);
                Mockito.when(semesterRepository.findById(SEMESTER_ID)).thenReturn(Optional.of(semester));

                Mockito.when(enrollmentRepository.findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                                CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(List.of());

                ResClassTermTranscriptDTO result = service.getClassTermTranscript(CLASS_ID, SEMESTER_ID);
                Assertions.assertEquals("10A1", result.classCode());
                Assertions.assertTrue(result.students().isEmpty());
        }

        @Test
        void getClassTermTranscriptPopulatesStudents() {
                SchoolClass schoolClass = Mockito.mock(SchoolClass.class);
                Mockito.when(schoolClass.getAcademicYearId()).thenReturn(YEAR_ID);
                Mockito.when(schoolClass.getClassCode()).thenReturn("10A1");
                Mockito.when(schoolClass.getClassName()).thenReturn("Lớp 10A1");
                Mockito.when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));

                Semester semester = Mockito.mock(Semester.class);
                Mockito.when(semester.getAcademicYearId()).thenReturn(YEAR_ID);
                Mockito.when(semesterRepository.findById(SEMESTER_ID)).thenReturn(Optional.of(semester));

                StudentYearEnrollment enrollment = Mockito.mock(StudentYearEnrollment.class);
                Mockito.when(enrollment.getStudentId()).thenReturn(STUDENT_ID);
                Mockito.when(enrollmentRepository.findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                                CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(List.of(enrollment));

                Student student = Mockito.mock(Student.class);
                Mockito.when(student.getId()).thenReturn(STUDENT_ID);
                Mockito.when(student.getStudentCode()).thenReturn("HS001");
                Mockito.when(student.getStudentName()).thenReturn("Nguyễn Văn A");
                Mockito.when(studentRepository.findAllById(List.of(STUDENT_ID))).thenReturn(List.of(student));

                StudentTermTranscript transcript = Mockito.mock(StudentTermTranscript.class);
                Mockito.when(transcript.getId()).thenReturn(50L);
                Mockito.when(transcript.getStudentId()).thenReturn(STUDENT_ID);
                Mockito.when(transcript.getCalculationStatus()).thenReturn(CalculationStatus.FINISH);
                Mockito.when(transcript.getDtbhk()).thenReturn(BigDecimal.valueOf(8.5));
                Mockito.when(termTranscriptRepository.findAllBySemesterIdAndStudentIdIn(
                                SEMESTER_ID, List.of(STUDENT_ID))).thenReturn(List.of(transcript));

                StudentSubjectTermResult subjectResult = Mockito.mock(StudentSubjectTermResult.class);
                Mockito.when(subjectResult.getTermTranscriptId()).thenReturn(50L);
                Mockito.when(subjectResult.getClassSubjectId()).thenReturn(60L);
                Mockito.when(termResultRepository.findAllByTermTranscriptIdInOrderBySubjectIdAsc(List.of(50L)))
                                .thenReturn(List.of(subjectResult));

                Mockito.when(responseSupport.findClassSubjects(List.of(60L))).thenReturn(Map.of());
                Mockito.when(termMapper.map(STUDENT_ID, List.of(subjectResult), Map.of())).thenReturn(List.of());

                ResClassTermTranscriptDTO result = service.getClassTermTranscript(CLASS_ID, SEMESTER_ID);
                Assertions.assertEquals(1, result.students().size());
                Assertions.assertEquals("Nguyễn Văn A", result.students().get(0).fullName());
                Assertions.assertEquals(BigDecimal.valueOf(8.5), result.students().get(0).dtbhk());
        }

        @Test
        void getClassAnnualTranscriptThrowsWhenYearMismatch() {
                SchoolClass schoolClass = Mockito.mock(SchoolClass.class);
                Mockito.when(schoolClass.getAcademicYearId()).thenReturn(YEAR_ID);
                Mockito.when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> service.getClassAnnualTranscript(CLASS_ID, 999L));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        void getClassAnnualTranscriptPopulatesStudents() {
                SchoolClass schoolClass = Mockito.mock(SchoolClass.class);
                Mockito.when(schoolClass.getAcademicYearId()).thenReturn(YEAR_ID);
                Mockito.when(schoolClass.getClassCode()).thenReturn("10A1");
                Mockito.when(schoolClass.getClassName()).thenReturn("Lớp 10A1");
                Mockito.when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass));

                Semester semester = Mockito.mock(Semester.class);
                Mockito.when(semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(YEAR_ID))
                                .thenReturn(List.of(semester));

                StudentYearEnrollment enrollment = Mockito.mock(StudentYearEnrollment.class);
                Mockito.when(enrollment.getStudentId()).thenReturn(STUDENT_ID);
                Mockito.when(enrollmentRepository.findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                                CLASS_ID, EnrollmentStatus.ACTIVE)).thenReturn(List.of(enrollment));

                Student student = Mockito.mock(Student.class);
                Mockito.when(student.getId()).thenReturn(STUDENT_ID);
                Mockito.when(student.getStudentCode()).thenReturn("HS001");
                Mockito.when(student.getStudentName()).thenReturn("Nguyễn Văn A");
                Mockito.when(studentRepository.findAllById(List.of(STUDENT_ID))).thenReturn(List.of(student));

                StudentAnnualTranscript transcript = Mockito.mock(StudentAnnualTranscript.class);
                Mockito.when(transcript.getId()).thenReturn(70L);
                Mockito.when(transcript.getStudentId()).thenReturn(STUDENT_ID);
                Mockito.when(transcript.getCalculationStatus()).thenReturn(CalculationStatus.FINISH);
                Mockito.when(transcript.getRegularDtbcn()).thenReturn(BigDecimal.valueOf(7.8));
                Mockito.when(transcript.getFinalDtbcn()).thenReturn(BigDecimal.valueOf(8.2));
                Mockito.when(annualTranscriptRepository.findAllByAcademicYearIdAndStudentIdIn(
                                YEAR_ID, List.of(STUDENT_ID))).thenReturn(List.of(transcript));

                StudentSubjectAnnualResult annualResult = Mockito.mock(StudentSubjectAnnualResult.class);
                Mockito.when(annualResult.getAnnualTranscriptId()).thenReturn(70L);
                Mockito.when(annualResult.getHk1TermResultId()).thenReturn(80L);
                Mockito.when(annualResult.getHk2TermResultId()).thenReturn(null);
                Mockito.when(annualResultRepository.findAllByAnnualTranscriptIdInOrderBySubjectIdAsc(List.of(70L)))
                                .thenReturn(List.of(annualResult));

                StudentSubjectTermResult hk1Result = Mockito.mock(StudentSubjectTermResult.class);
                Mockito.when(hk1Result.getId()).thenReturn(80L);
                Mockito.when(termResultRepository.findAllById(List.of(80L))).thenReturn(List.of(hk1Result));

                Mockito.when(responseSupport.mapAnnualResults(List.of(annualResult), Map.of(80L, hk1Result)))
                                .thenReturn(List.of());

                ResClassAnnualTranscriptDTO result = service.getClassAnnualTranscript(CLASS_ID, YEAR_ID);
                Assertions.assertEquals(1, result.students().size());
                Assertions.assertEquals("HS001", result.students().get(0).studentCode());
                Assertions.assertEquals(BigDecimal.valueOf(8.2), result.students().get(0).finalDtbcn());
        }
}

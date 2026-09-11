package com.JavaTraining.BaiTap_RS.student.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpdateStudentStatusDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpsertStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentV2PageDTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import com.JavaTraining.BaiTap_RS.student.repository.StudentClassLookupRepository;
import com.JavaTraining.BaiTap_RS.student.repository.StudentDeletionGuardRepository;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentV2ServiceTest {

    private static final Long STUDENT_ID = 1L;
    private static final Long CLASS_ID = 7L;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentClassLookupRepository classLookupRepository;

    @Mock
    private StudentDeletionGuardRepository deletionGuardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentV2AccessService accessService;

    private StudentV2Service studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentV2Service(
                studentRepository,
                deletionGuardRepository,
                accessService,
                new StudentV2ResponseMapper(classLookupRepository, userRepository));
    }

    @Test
    void fetchStudentsReturnsPersistedStatusAndCurrentClass() {
        Student student = student();
        Mockito.when(accessService.accessScope())
                .thenReturn(new StudentV2AccessService.AccessScope(false, Set.of(CLASS_ID)));
        Mockito.when(studentRepository.findAll(
                        Mockito.<Specification<Student>>any(),
                        Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student), PageRequest.of(0, 10), 1));
        Mockito.when(classLookupRepository.findActiveClassesByStudentIds(List.of(STUDENT_ID)))
                .thenReturn(List.<Object[]>of(new Object[] {STUDENT_ID, CLASS_ID, "6A1"}));

        ResStudentV2PageDTO response = studentService.fetchStudents(
                new ReqFetchStudentV2DTO(null, null, null, StudentStatus.ACTIVE, null, 0, 10, null, null));

        Assertions.assertEquals(
                "ACTIVE|7|6A1",
                response.content().getFirst().status()
                        + "|"
                        + response.content().getFirst().currentClassId()
                        + "|"
                        + response.content().getFirst().currentClassCode(),
                "v2 list must use persisted lifecycle and current-class data");
    }

    @Test
    void transitionStatusRejectsReactivationOutsideApprovedLifecycleFlow() {
        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> studentService.transitionStatus(STUDENT_ID, new ReqUpdateStudentStatusDTO(StudentStatus.ACTIVE)));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        Mockito.verifyNoInteractions(studentRepository);
    }

    @Test
    void createStudentPersistsV2FieldsWithoutLegacyAverageScore() {
        Mockito.when(studentRepository.existsByStudentCode("STU1234567")).thenReturn(false);
        Mockito.when(studentRepository.save(Mockito.any(Student.class))).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", STUDENT_ID);
            return saved;
        });

        ResStudentV2DTO response = studentService.createStudent(v2Request("STU1234567"));

        Assertions.assertEquals(StudentStatus.ACTIVE, response.status());
        Mockito.verify(studentRepository).save(Mockito.argThat(saved ->
                saved.getStudentInfo().getAverageScore() == null));
    }

    @Test
    void updateStudentRejectsStudentCodeMutation() {
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));

        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> studentService.updateStudent(STUDENT_ID, v2Request("STU7654321")));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void updateStudentClearsExistingGenderWhenRequestSendsNull() {
        Student student = student();
        student.getStudentInfo().setGender(com.JavaTraining.BaiTap_RS.student.domain.entity.StudentGender.FEMALE);
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        studentService.updateStudent(STUDENT_ID, v2Request("STU1234567"));

        Assertions.assertNull(student.getStudentInfo().getGender(), "explicit null gender must clear stored gender");
    }

    @Test
    void transitionStatusChangesStudentLifecycleState() {
        Student student = student();
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        Mockito.when(classLookupRepository.findActiveClassesByStudentIds(List.of(STUDENT_ID)))
                .thenReturn(List.<Object[]>of(new Object[] {STUDENT_ID, CLASS_ID, "6A1"}));

        ResStudentV2DTO response = studentService.transitionStatus(
                STUDENT_ID, new ReqUpdateStudentStatusDTO(StudentStatus.GRADUATED));

        Assertions.assertEquals(StudentStatus.GRADUATED, response.status());
        Assertions.assertEquals(StudentStatus.GRADUATED, student.getStatus());
    }

    @Test
    void deleteStudentRejectsExistingAcademicHistory() {
        Student student = student();
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        Mockito.when(deletionGuardRepository.existsByStudentId(STUDENT_ID)).thenReturn(true);

        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> studentService.deleteStudent(STUDENT_ID));

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        Mockito.verify(studentRepository, Mockito.never()).delete(Mockito.any(Student.class));
    }

    private Student student() {
        Student student = new Student("Nguyen Van A", "STU1234567");
        student.assignInfo(new StudentInfo(LocalDate.of(2012, 4, 22), "Ho Chi Minh City", 8.5));
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);
        return student;
    }

    private ReqUpsertStudentV2DTO v2Request(String studentCode) {
        return new ReqUpsertStudentV2DTO(
                studentCode,
                "Nguyen Van A",
                LocalDate.of(2012, 4, 22),
                "Ho Chi Minh City");
    }
}

package com.JavaTraining.BaiTap_RS.student.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.UnitTestContainsTooManyAsserts",
        "PMD.AvoidDuplicateLiterals"
})
class StudentLookupServiceTest {

    @Mock
    private StudentRepository studentRepository;

    private StudentLookupService lookupService;

    @BeforeEach
    void setUp() {
        lookupService = new StudentLookupService(studentRepository);
    }

    @Test
    void resolveStudentWithValidIdReturnsStudent() {
        Student student = student(1L, "STU0000001");
        Mockito.when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        Student result = lookupService.resolveStudent(1L, null);

        Assertions.assertSame(student, result, "student resolved by id should be returned");
    }

    @Test
    void resolveStudentWithValidCodeReturnsStudent() {
        Student student = student(1L, "STU0000001");
        Mockito.when(studentRepository.findByStudentCode("STU0000001")).thenReturn(Optional.of(student));

        Student result = lookupService.resolveStudent(null, " STU0000001 ");

        Assertions.assertSame(student, result, "student resolved by code should be returned");
    }

    @Test
    void resolveStudentWithMatchingIdAndCodeReturnsStudent() {
        Student student = student(1L, "STU0000001");
        Mockito.when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        Mockito.when(studentRepository.findByStudentCode("STU0000001")).thenReturn(Optional.of(student));

        Student result = lookupService.resolveStudent(1L, "STU0000001");

        Assertions.assertSame(student, result, "matching id and code should resolve to one student");
    }

    @Test
    void resolveStudentWithMismatchedIdAndCodeThrowsBadRequest() {
        Mockito.when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "STU0000001")));
        Mockito.when(studentRepository.findByStudentCode("STU0000002"))
                .thenReturn(Optional.of(student(2L, "STU0000002")));

        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> lookupService.resolveStudent(1L, "STU0000002"));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(), "mismatch should be 400");
    }

    @Test
    void resolveStudentWithBothNullThrowsBadRequest() {
        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> lookupService.resolveStudent(null, " "));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(), "missing identifier should be 400");
    }

    @Test
    void resolveStudentNotFoundThrowsNotFound() {
        Mockito.when(studentRepository.findByStudentCode("STU404")).thenReturn(Optional.empty());

        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> lookupService.resolveStudent(null, "STU404"));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(), "missing student should be 404");
    }

    @Test
    void resolveStudentsWithMixedCodesAndIdsReturnsAll() {
        Student idStudent = student(1L, "STU0000001");
        Student codeStudent = student(2L, "STU0000002");
        Set<Long> ids = new LinkedHashSet<>(List.of(1L));
        Set<String> codes = new LinkedHashSet<>(List.of("STU0000002"));
        Mockito.when(studentRepository.findAllById(ids)).thenReturn(List.of(idStudent));
        Mockito.when(studentRepository.findAllByStudentCodeIn(codes)).thenReturn(List.of(codeStudent));

        List<Student> students = lookupService.resolveStudents(List.of(1L), List.of("STU0000002"));

        Assertions.assertEquals(2, students.size(), "mixed identifiers should return two students");
        Assertions.assertTrue(students.contains(idStudent), "id student should be included");
        Assertions.assertTrue(students.contains(codeStudent), "code student should be included");
    }

    private Student student(Long id, String code) {
        Student student = new Student("Học sinh " + id, code);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}

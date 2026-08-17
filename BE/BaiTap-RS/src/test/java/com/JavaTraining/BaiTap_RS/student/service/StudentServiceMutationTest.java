package com.JavaTraining.BaiTap_RS.student.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqCreateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpdateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
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
class StudentServiceMutationTest {

    private static final Long STUDENT_ID = 1L;
    private static final String STUDENT_CODE = "STU1234567";
    private static final String STUDENT_NAME = "Nguyen Van A";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(2012, 4, 22);
    private static final String ADDRESS = "Ho Chi Minh City";
    private static final Double AVERAGE_SCORE = 8.5;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentCodeGenerator studentCodeGenerator;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, studentCodeGenerator);
    }

    @Test
    void createStudentCreatesStudentAndInfoWhenCodeIsUnique() {
        AtomicReference<Student> savedStudent = new AtomicReference<>();
        Mockito.when(studentRepository.existsByStudentCode(STUDENT_CODE)).thenReturn(false);
        Mockito.when(studentRepository.save(Mockito.any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            ReflectionTestUtils.setField(student, "id", STUDENT_ID);
            savedStudent.set(student);
            return student;
        });

        ResStudentDTO response = studentService.createStudent(createRequest());

        Assertions.assertEquals(
                "1|STU1234567|Nguyen Van A|2012-04-22|Ho Chi Minh City|8.5|STU1234567",
                response.getStudentId()
                        + "|"
                        + savedStudent.get().getStudentCode()
                        + "|"
                        + savedStudent.get().getStudentName()
                        + "|"
                        + savedStudent.get().getStudentInfo().getDateOfBirth()
                        + "|"
                        + savedStudent.get().getStudentInfo().getAddress()
                        + "|"
                        + savedStudent.get().getStudentInfo().getAverageScore()
                        + "|"
                        + savedStudent.get().getStudentInfo().getStudent().getStudentCode(),
                "create should save linked Student and StudentInfo aggregate");
    }

    @Test
    void createStudentRejectsDuplicateStudentCode() {
        Mockito.when(studentRepository.existsByStudentCode(STUDENT_CODE)).thenReturn(true);

        AppException exception = captureAppException(() -> studentService.createStudent(createRequest()));

        Assertions.assertEquals(
                HttpStatus.CONFLICT.value() + "|Mã sinh viên đã tồn tại",
                exception.getStatus().value() + "|" + exception.getMessage(),
                "duplicate student code should return conflict");
    }

    @Test
    void updateStudentUpdatesMutableFieldsOnly() {
        Student student = studentWithId();
        student.assignInfo(new StudentInfo(DATE_OF_BIRTH, ADDRESS, AVERAGE_SCORE));
        ReqUpdateStudentDTO request = new ReqUpdateStudentDTO(
                "Tran Van B",
                LocalDate.of(2013, 5, 23),
                "Da Nang",
                9.0);
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        Mockito.when(studentRepository.save(Mockito.any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResStudentDTO response = studentService.updateStudent(STUDENT_ID, request);

        Assertions.assertEquals(
                "1|STU1234567|Tran Van B|2013-05-23|Da Nang|9.0",
                response.getStudentId()
                        + "|"
                        + response.getStudentCode()
                        + "|"
                        + response.getStudentName()
                        + "|"
                        + response.getDateOfBirth()
                        + "|"
                        + response.getAddress()
                        + "|"
                        + response.getAverageScore(),
                "update should keep student code and update mutable fields only");
    }

    @Test
    void updateStudentRejectsMissingStudent() {
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        AppException exception = captureAppException(() -> studentService.updateStudent(
                STUDENT_ID,
                new ReqUpdateStudentDTO(STUDENT_NAME, DATE_OF_BIRTH, ADDRESS, AVERAGE_SCORE)));

        Assertions.assertEquals(
                HttpStatus.NOT_FOUND.value() + "|Không tìm thấy sinh viên",
                exception.getStatus().value() + "|" + exception.getMessage(),
                "missing student should return not found");
    }

    @Test
    void deleteStudentDeletesExistingStudent() {
        Student student = studentWithId();
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        studentService.deleteStudent(STUDENT_ID);

        Mockito.verify(studentRepository).delete(student);
    }

    @Test
    void deleteStudentRejectsMissingStudent() {
        Mockito.when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        AppException exception = captureAppException(() -> studentService.deleteStudent(STUDENT_ID));

        Assertions.assertEquals(
                HttpStatus.NOT_FOUND.value() + "|Không tìm thấy sinh viên",
                exception.getStatus().value() + "|" + exception.getMessage(),
                "missing student should return not found");
    }

    private ReqCreateStudentDTO createRequest() {
        return new ReqCreateStudentDTO(STUDENT_CODE, STUDENT_NAME, DATE_OF_BIRTH, ADDRESS, AVERAGE_SCORE);
    }

    private Student studentWithId() {
        Student student = new Student(STUDENT_NAME, STUDENT_CODE);
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);
        return student;
    }

    private AppException captureAppException(Runnable action) {
        try {
            action.run();
            return new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_EXCEPTION");
        } catch (AppException exception) {
            return exception;
        }
    }
}

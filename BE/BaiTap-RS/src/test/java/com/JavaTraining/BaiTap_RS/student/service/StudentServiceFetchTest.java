package com.JavaTraining.BaiTap_RS.student.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentPageDTO;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentServiceFetchTest {

    private static final Long STUDENT_ID = 1L;
    private static final String STUDENT_CODE = "STU1234567";
    private static final String STUDENT_NAME = "Nguyen Van A";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(2012, 4, 22);

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
    void fetchStudentsReturnsPagedResultWithDefaultSizeTen() {
        Student student = studentWithId(STUDENT_ID, STUDENT_CODE, STUDENT_NAME);
        student.assignInfo(new StudentInfo(DATE_OF_BIRTH, "Ho Chi Minh City", 8.5));
        Mockito.when(studentRepository.findAll(
                        Mockito.<Specification<Student>>any(),
                        Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student), PageRequest.of(0, 10), 1));
        ReqFetchStudentDTO request = new ReqFetchStudentDTO(null, null, null, 0, 0, null, null);

        ResStudentPageDTO response = studentService.fetchStudents(request);

        Assertions.assertEquals(
                "0|10|1|1|STU1234567",
                response.getPage()
                        + "|"
                        + response.getSize()
                        + "|"
                        + response.getTotalElements()
                        + "|"
                        + response.getContent().size()
                        + "|"
                        + response.getContent().get(0).getStudentCode(),
                "fetch should return stable page metadata and content");
    }

    @Test
    void fetchStudentsRejectsUnsupportedSortField() {
        ReqFetchStudentDTO request = new ReqFetchStudentDTO(null, null, null, 0, 10, "unsupported", "asc");

        AppException exception = captureAppException(() -> studentService.fetchStudents(request));

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST.value() + "|Trường sắp xếp không được hỗ trợ",
                exception.getStatus().value() + "|" + exception.getMessage(),
                "unsupported sort field should return bad request");
    }

    @Test
    void fetchStudentsRejectsUnsupportedSortDirection() {
        ReqFetchStudentDTO request = new ReqFetchStudentDTO(null, null, null, 0, 10, "studentCode", "sideways");

        AppException exception = captureAppException(() -> studentService.fetchStudents(request));

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST.value() + "|Chiều sắp xếp không được hỗ trợ",
                exception.getStatus().value() + "|" + exception.getMessage(),
                "unsupported sort direction should return bad request");
    }

    @Test
    void fetchStudentsUsesJpaSpecificationExecutorRepository() {
        Mockito.when(studentRepository.findAll(
                        Mockito.<Specification<Student>>any(),
                        Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        ReqFetchStudentDTO request = new ReqFetchStudentDTO(
                STUDENT_CODE,
                STUDENT_NAME,
                DATE_OF_BIRTH,
                0,
                10,
                "dateOfBirth",
                "desc");

        studentService.fetchStudents(request);

        Mockito.verify(studentRepository).findAll(
                Mockito.<Specification<Student>>any(),
                Mockito.<Pageable>argThat(pageable ->
                        "studentInfo.dateOfBirth: DESC".equals(pageable.getSort().toString())));
    }

    private Student studentWithId(Long id, String studentCode, String studentName) {
        Student student = new Student(studentName, studentCode);
        ReflectionTestUtils.setField(student, "id", id);
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

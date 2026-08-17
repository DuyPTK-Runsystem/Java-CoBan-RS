package com.JavaTraining.BaiTap_RS.student.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentCodeDTO;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceCodeTest {

    private static final String STUDENT_CODE = "STU1234567";
    private static final Set<String> FIRST_BATCH = orderedSet(
            "STU1234567",
            "STU7654321",
            "STU0000001");

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
    void generateStudentCodeReturnsFirstCandidateNotFoundInDbBatch() {
        Mockito.doReturn(FIRST_BATCH).when(studentCodeGenerator).generateCandidates(20);
        Mockito.when(studentRepository.findExistingStudentCodes(FIRST_BATCH))
                .thenReturn(List.of(STUDENT_CODE));

        ResStudentCodeDTO response = studentService.generateStudentCode();

        Assertions.assertEquals("STU7654321", response.getStudentCode(), "generate should return first available code");
    }

    @Test
    void generateStudentCodeRetriesBatchWhenAllCandidatesExist() {
        Set<String> secondBatch = orderedSet("STU0000002", "STU0000003");
        Mockito.doReturn(FIRST_BATCH).doReturn(secondBatch).when(studentCodeGenerator).generateCandidates(20);
        Mockito.when(studentRepository.findExistingStudentCodes(FIRST_BATCH))
                .thenReturn(List.copyOf(FIRST_BATCH));
        Mockito.when(studentRepository.findExistingStudentCodes(secondBatch))
                .thenReturn(List.of("STU0000002"));

        ResStudentCodeDTO response = studentService.generateStudentCode();

        Assertions.assertEquals("STU0000003", response.getStudentCode(), "generate should retry next batch");
    }

    private static Set<String> orderedSet(String... values) {
        return new LinkedHashSet<>(List.of(values));
    }
}

package com.JavaTraining.BaiTap_RS.student.controller;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:student-detail-integration;MODE=MySQL;"
                + "DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class StudentDetailControllerIntegrationTest {

    private static final String STUDENTS_PATH = "/api/v1/students";
    private static final String STUDENT_NAME_JSON_FIELD = "\"studentName\":\"";
    private static final String STUDENT_CODE = "STU1234567";
    private static final String STUDENT_NAME = "Nguyen Van A";
    private static final String MAX_LENGTH_STUDENT_NAME = "A".repeat(35);
    private static final String TOO_LONG_STUDENT_NAME = "A".repeat(36);
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(2012, 4, 22);
    private static final String ADDRESS = "Ho Chi Minh City";
    private static final Double AVERAGE_SCORE = 8.5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAll();
    }

    @Test
    void studentDetailReturnsDocumentedResponseContract() throws Exception {
        Student student = persistStudent(STUDENT_CODE, STUDENT_NAME);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(STUDENTS_PATH + "/" + student.getId()))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 200
                        && response.contains("\"statusCode\":200")
                        && response.contains("\"message\":\"Lấy thông tin sinh viên\"")
                        && response.contains("\"studentId\":" + student.getId())
                        && response.contains("\"studentCode\":\"" + STUDENT_CODE + "\"")
                        && response.contains(STUDENT_NAME_JSON_FIELD + STUDENT_NAME + "\"")
                        && response.contains("\"dateOfBirth\":\"" + DATE_OF_BIRTH + "\"")
                        && response.contains("\"address\":\"" + ADDRESS + "\"")
                        && response.contains("\"averageScore\":" + AVERAGE_SCORE),
                "detail should return the documented Student response fields");
    }

    @Test
    void studentDetailReturnsNotFoundForMissingStudent() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(STUDENTS_PATH + "/999999"))
                .andReturn();

        Assertions.assertTrue(
                result.getResponse().getStatus() == 404
                        && result.getResponse().getContentAsString().contains("Không tìm thấy sinh viên"),
                "detail should return the standard not found response");
    }

    @Test
    void studentDetailRequiresAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(STUDENTS_PATH + "/999999")
                        .with(SecurityMockMvcRequestPostProcessors.anonymous()))
                .andReturn();

        Assertions.assertTrue(
                result.getResponse().getStatus() == 401,
                "detail should not be accessible without authentication");
    }

    @Test
    void studentCreateAndUpdateSupportNullableInfoAndNameLengthBoundary() throws Exception {
        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(STUDENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createNullableStudentJson(MAX_LENGTH_STUDENT_NAME)))
                .andReturn();

        Student student = studentRepository.findAll().getFirst();
        StudentInfo info = student.getStudentInfo();
        String validRequest = "{" + STUDENT_NAME_JSON_FIELD + MAX_LENGTH_STUDENT_NAME + "\"}";
        String invalidRequest = "{" + STUDENT_NAME_JSON_FIELD + TOO_LONG_STUDENT_NAME + "\"}";
        MvcResult validUpdateResult = mockMvc.perform(MockMvcRequestBuilders
                        .put(STUDENTS_PATH + "/" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andReturn();
        MvcResult invalidUpdateResult = mockMvc.perform(MockMvcRequestBuilders
                        .put(STUDENTS_PATH + "/" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Assertions.assertTrue(
                createResult.getResponse().getStatus() == 201
                        && response.contains(STUDENT_NAME_JSON_FIELD + MAX_LENGTH_STUDENT_NAME + "\"")
                        && response.contains("\"dateOfBirth\":null")
                        && response.contains("\"address\":null")
                        && response.contains("\"averageScore\":null")
                        && info != null
                        && info.getDateOfBirth() == null
                        && info.getAddress() == null
                        && info.getAverageScore() == null
                        && validUpdateResult.getResponse().getStatus() == 200
                        && invalidUpdateResult.getResponse().getStatus() == 400
                        && invalidUpdateResult.getResponse().getContentAsString().contains("studentName"),
                "the aggregate should keep nullable info fields and enforce the 35 character name limit");
    }

    private Student persistStudent(String studentCode, String studentName) {
        Student student = new Student(studentName, studentCode);
        student.assignInfo(new StudentInfo(DATE_OF_BIRTH, ADDRESS, AVERAGE_SCORE));
        return studentRepository.saveAndFlush(student);
    }

    private String createNullableStudentJson(String studentName) {
        return """
                {
                  "studentCode": "%s",
                  "studentName": "%s",
                  "dateOfBirth": null,
                  "address": null,
                  "averageScore": null
                }
                """.formatted(STUDENT_CODE, studentName);
    }
}

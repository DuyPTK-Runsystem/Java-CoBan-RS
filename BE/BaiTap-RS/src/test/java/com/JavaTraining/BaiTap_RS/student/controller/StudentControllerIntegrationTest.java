package com.JavaTraining.BaiTap_RS.student.controller;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import com.JavaTraining.BaiTap_RS.student.repository.StudentInfoRepository;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:student-integration;MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class StudentControllerIntegrationTest {

    private static final String STUDENTS_PATH = "/api/v1/students";
    private static final String STUDENT_CODE = "STU1234567";
    private static final String STUDENT_NAME = "Nguyen Van A";
    private static final String UPDATED_STUDENT_NAME = "Tran Van B";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(2012, 4, 22);
    private static final LocalDate UPDATED_DATE_OF_BIRTH = LocalDate.of(2013, 5, 23);
    private static final String ADDRESS = "Ho Chi Minh City";
    private static final String UPDATED_ADDRESS = "Da Nang";
    private static final Double AVERAGE_SCORE = 8.5;
    private static final Double UPDATED_AVERAGE_SCORE = 9.0;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentInfoRepository studentInfoRepository;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAll();
    }

    @Test
    void createStudentReturnsCreatedResponseAndPersistsAggregate() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(STUDENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createStudentJson(STUDENT_CODE, STUDENT_NAME)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Student persistedStudent = studentRepository.findAll().getFirst();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 201
                        && response.contains("\"statusCode\":201")
                        && response.contains("\"message\":\"Tạo sinh viên\"")
                        && response.contains("\"studentCode\":\"" + STUDENT_CODE + "\"")
                        && response.contains("\"studentName\":\"" + STUDENT_NAME + "\"")
                        && response.contains("\"dateOfBirth\":\"" + DATE_OF_BIRTH + "\"")
                        && response.contains("\"address\":\"" + ADDRESS + "\"")
                        && response.contains("\"averageScore\":" + AVERAGE_SCORE)
                        && studentRepository.count() == 1
                        && studentInfoRepository.count() == 1
                        && STUDENT_CODE.equals(persistedStudent.getStudentCode())
                        && persistedStudent.equals(persistedStudent.getStudentInfo().getStudent()),
                "create should return the response contract and persist the linked aggregate");
    }

    @Test
    void fetchStudentsReturnsSortedPagedResponse() throws Exception {
        persistStudent("STU1234567", "Nguyen Van A");
        persistStudent("STU1234568", "Tran Van B");
        persistStudent("STU1234569", "Le Van C");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(STUDENTS_PATH)
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortField", "studentCode")
                        .param("sortDirection", "desc"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 200
                        && response.contains("\"statusCode\":200")
                        && response.contains("\"message\":\"Lấy danh sách sinh viên\"")
                        && response.contains("\"page\":0")
                        && response.contains("\"size\":2")
                        && response.contains("\"totalElements\":3")
                        && response.contains("\"totalPages\":2")
                        && response.indexOf("STU1234569") < response.indexOf("STU1234568"),
                "fetch should return sorted page metadata and content");
    }

    @Test
    void updateStudentReturnsUpdatedResponseAndKeepsStudentCode() throws Exception {
        Student student = persistStudent(STUDENT_CODE, STUDENT_NAME);
        String request = """
                {
                  "studentName": "%s",
                  "dateOfBirth": "%s",
                  "address": "%s",
                  "averageScore": %s
                }
                """.formatted(
                UPDATED_STUDENT_NAME,
                UPDATED_DATE_OF_BIRTH,
                UPDATED_ADDRESS,
                UPDATED_AVERAGE_SCORE);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .put(STUDENTS_PATH + "/" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Student updatedStudent = studentRepository.findById(student.getId()).orElseThrow();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 200
                        && response.contains("\"statusCode\":200")
                        && response.contains("\"studentCode\":\"" + STUDENT_CODE + "\"")
                        && response.contains("\"studentName\":\"" + UPDATED_STUDENT_NAME + "\"")
                        && response.contains("\"dateOfBirth\":\"" + UPDATED_DATE_OF_BIRTH + "\"")
                        && response.contains("\"address\":\"" + UPDATED_ADDRESS + "\"")
                        && response.contains("\"averageScore\":" + UPDATED_AVERAGE_SCORE)
                        && STUDENT_CODE.equals(updatedStudent.getStudentCode())
                        && UPDATED_STUDENT_NAME.equals(updatedStudent.getStudentName())
                        && UPDATED_DATE_OF_BIRTH.equals(updatedStudent.getStudentInfo().getDateOfBirth()),
                "update should keep the student code and update mutable fields");
    }

    @Test
    void deleteStudentReturnsNoContentAndDeletesAggregate() throws Exception {
        Student student = persistStudent(STUDENT_CODE, STUDENT_NAME);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .delete(STUDENTS_PATH + "/" + student.getId()))
                .andReturn();

        Assertions.assertTrue(
                result.getResponse().getStatus() == 204
                        && studentRepository.count() == 0
                        && studentInfoRepository.count() == 0,
                "delete should return no content and remove the linked aggregate");
    }

    @Test
    void createStudentRejectsDuplicateCode() throws Exception {
        persistStudent(STUDENT_CODE, STUDENT_NAME);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(STUDENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createStudentJson(STUDENT_CODE, "Student Duplicate")))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 409
                        && response.contains("\"statusCode\":409")
                        && response.contains("\"error\":\"Conflict\"")
                        && response.contains("\"message\":\"Mã sinh viên đã tồn tại\""),
                "duplicate student code should return conflict");
    }

    @Test
    void createStudentRejectsInvalidCodeAndBlankName() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(STUDENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createStudentJson("INVALID", "")))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 400
                        && response.contains("\"statusCode\":400")
                        && response.contains("studentCode")
                        && response.contains("studentName"),
                "invalid student code and blank name should return validation errors");
    }

    @Test
    void missingStudentMutationReturnsNotFound() throws Exception {
        String request = "{\"studentName\":\"Tran Van B\"}";

        MvcResult updateResult = mockMvc.perform(MockMvcRequestBuilders
                        .put(STUDENTS_PATH + "/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn();
        MvcResult deleteResult = mockMvc.perform(MockMvcRequestBuilders
                        .delete(STUDENTS_PATH + "/999999"))
                .andReturn();

        String expectedMessage = "\"message\":\"Không tìm thấy sinh viên\"";
        Assertions.assertTrue(
                updateResult.getResponse().getStatus() == 404
                        && updateResult.getResponse().getContentAsString().contains(expectedMessage)
                        && deleteResult.getResponse().getStatus() == 404
                        && deleteResult.getResponse().getContentAsString().contains(expectedMessage),
                "update and delete should return not found for a missing student");
    }

    private Student persistStudent(String studentCode, String studentName) {
        Student student = new Student(studentName, studentCode);
        student.assignInfo(new StudentInfo(DATE_OF_BIRTH, ADDRESS, AVERAGE_SCORE));
        return studentRepository.saveAndFlush(student);
    }

    private String createStudentJson(String studentCode, String studentName) {
        return """
                {
                  "studentCode": "%s",
                  "studentName": "%s",
                  "dateOfBirth": "%s",
                  "address": "%s",
                  "averageScore": %s
                }
                """.formatted(studentCode, studentName, DATE_OF_BIRTH, ADDRESS, AVERAGE_SCORE);
    }
}

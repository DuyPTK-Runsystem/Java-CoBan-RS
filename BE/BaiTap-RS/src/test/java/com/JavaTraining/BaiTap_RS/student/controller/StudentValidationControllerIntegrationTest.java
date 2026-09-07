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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:student-validation;MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Transactional
class StudentValidationControllerIntegrationTest {

    private static final String STUDENTS_PATH = "/api/v1/students";
    private static final String STUDENT_CODE = "STU1234567";
    private static final String STUDENT_NAME = "Nguyen Van A";
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
    void createStudentRejectsAverageScoreOutsideZeroToTen() throws Exception {
        MvcResult negativeResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(STUDENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createStudentJson(STUDENT_CODE, STUDENT_NAME, DATE_OF_BIRTH, -0.01)))
                .andReturn();
        MvcResult overLimitResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(STUDENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createStudentJson("STU1234568", STUDENT_NAME, DATE_OF_BIRTH, 10.01)))
                .andReturn();

        Assertions.assertTrue(
                negativeResult.getResponse().getStatus() == 400
                        && negativeResult.getResponse().getContentAsString().contains("averageScore")
                        && overLimitResult.getResponse().getStatus() == 400
                        && overLimitResult.getResponse().getContentAsString().contains("averageScore")
                        && studentRepository.count() == 0,
                "out-of-range average scores should be rejected before persistence");
    }

    @Test
    void createStudentRejectsFutureAndMalformedBirthdays() throws Exception {
        LocalDate futureBirthday = LocalDate.now().plusDays(1);
        MvcResult futureResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(STUDENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createStudentJson(STUDENT_CODE, STUDENT_NAME, futureBirthday, AVERAGE_SCORE)))
                .andReturn();
        MvcResult malformedResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(STUDENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentCode": "STU1234567",
                                  "studentName": "Nguyen Van A",
                                  "dateOfBirth": "not-a-date"
                                }
                                """))
                .andReturn();

        Assertions.assertTrue(
                futureResult.getResponse().getStatus() == 400
                        && futureResult.getResponse().getContentAsString().contains("dateOfBirth")
                        && malformedResult.getResponse().getStatus() == 400
                        && malformedResult.getResponse().getContentAsString().contains("statusCode")
                        && studentRepository.count() == 0,
                "future and malformed birthdays should return a bad-request envelope");
    }

    @Test
    void updateStudentRejectsOutOfRangeAverageScoreWithoutChangingStudent() throws Exception {
        Student student = persistStudent();
        String request = """
                {
                  "studentName": "Tran Van B",
                  "dateOfBirth": "2013-05-23",
                  "address": "Da Nang",
                  "averageScore": 10.01
                }
                """;

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .put(STUDENTS_PATH + "/" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn();

        Student unchangedStudent = studentRepository.findById(student.getId()).orElseThrow();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 400
                        && result.getResponse().getContentAsString().contains("averageScore")
                        && AVERAGE_SCORE.equals(unchangedStudent.getStudentInfo().getAverageScore()),
                "invalid update score should not mutate the aggregate");
    }

    @Test
    void fetchAndPathInputsRejectInvalidValues() throws Exception {
        MvcResult negativePageResult = mockMvc.perform(MockMvcRequestBuilders
                        .get(STUDENTS_PATH)
                        .param("page", "-1"))
                .andReturn();
        MvcResult zeroIdResult = mockMvc.perform(MockMvcRequestBuilders
                        .get(STUDENTS_PATH + "/0"))
                .andReturn();

        Assertions.assertTrue(
                negativePageResult.getResponse().getStatus() == 400
                        && negativePageResult.getResponse().getContentAsString().contains("page")
                        && zeroIdResult.getResponse().getStatus() == 400
                        && zeroIdResult.getResponse().getContentAsString().contains("lớn hơn 0"),
                "invalid query and path values should return validation errors");
    }

    private Student persistStudent() {
        Student student = new Student(STUDENT_NAME, STUDENT_CODE);
        student.assignInfo(new StudentInfo(DATE_OF_BIRTH, ADDRESS, AVERAGE_SCORE));
        return studentRepository.saveAndFlush(student);
    }

    private String createStudentJson(
            String studentCode,
            String studentName,
            LocalDate dateOfBirth,
            Double averageScore) {
        return """
                {
                  "studentCode": "%s",
                  "studentName": "%s",
                  "dateOfBirth": "%s",
                  "address": "%s",
                  "averageScore": %s
                }
                """.formatted(studentCode, studentName, dateOfBirth, ADDRESS, averageScore);
    }
}

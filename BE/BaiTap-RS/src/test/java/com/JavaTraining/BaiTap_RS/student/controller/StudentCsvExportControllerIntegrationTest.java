package com.JavaTraining.BaiTap_RS.student.controller;

import java.nio.charset.StandardCharsets;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:student-csv-export;MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class StudentCsvExportControllerIntegrationTest {

    private static final String EXPORT_PATH = "/api/v1/students/export";
    private static final String CSV_HEADER = "student_id,student_name,student_code,"
            + "address,average_score,date_of_birth\r\n";
    private static final String QUOTED_STUDENT_ROW = "\"Nguyen, Van \"\"A\"\"\",STU1234567,"
            + "Da Nang,8.5,2012-04-22";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAll();
    }

    @Test
    void exportStudentsReturnsRawCsvForValidJoinedRows() throws Exception {
        persistStudent(
                "Nguyen, Van \"A\"",
                "STU1234567",
                "Da Nang",
                8.5,
                LocalDate.of(2012, 4, 22));
        persistStudent("Tran Van B", "STU1234568", null, null, null);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(EXPORT_PATH)).andReturn();
        String csv = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        Assertions.assertTrue(
                result.getResponse().getStatus() == 200
                        && result.getResponse().getContentType().startsWith("text/csv")
                        && result.getResponse().getHeader("Content-Disposition").contains("students.csv")
                        && csv.startsWith(CSV_HEADER)
                        && csv.contains(QUOTED_STUDENT_ROW)
                        && csv.contains("Tran Van B,STU1234568,,,")
                        && !csv.contains("\"statusCode\":"),
                "export should return the raw CSV contract without the JSON response envelope");
    }

    @Test
    void exportStudentsSkipsInvalidRowAndKeepsOtherRows() throws Exception {
        persistStudent("", "STU1234567", "Da Nang", 8.5, LocalDate.of(2012, 4, 22));
        persistStudent("Tran Van B", "STU1234568", "Ho Chi Minh City", 9.0, LocalDate.of(2013, 5, 23));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(EXPORT_PATH)).andReturn();
        String csv = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        Assertions.assertTrue(
                result.getResponse().getStatus() == 200
                        && !csv.contains("STU1234567")
                        && csv.contains("Tran Van B,STU1234568,Ho Chi Minh City,9.0,2013-05-23"),
                "an item-level failure should not fail the complete CSV export");
    }

    @Test
    void exportStudentsReturnsHeaderOnlyWhenNoRowsExist() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(EXPORT_PATH)).andReturn();

        Assertions.assertEquals(
                CSV_HEADER,
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "empty joined data should produce the CSV header only");
    }

    @Test
    @WithAnonymousUser
    void exportStudentsRequiresAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(EXPORT_PATH)).andReturn();

        Assertions.assertEquals(401, result.getResponse().getStatus(), "export should require authentication");
    }

    private void persistStudent(
            String studentName,
            String studentCode,
            String address,
            Double averageScore,
            LocalDate dateOfBirth) {
        Student student = new Student(studentName, studentCode);
        student.assignInfo(new StudentInfo(dateOfBirth, address, averageScore));
        studentRepository.saveAndFlush(student);
    }
}

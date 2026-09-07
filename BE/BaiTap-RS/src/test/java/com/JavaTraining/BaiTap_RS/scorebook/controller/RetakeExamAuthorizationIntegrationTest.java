package com.JavaTraining.BaiTap_RS.scorebook.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:retake-exam-authorization;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class RetakeExamAuthorizationIntegrationTest {

    private static final String RETAKE_URL = "/api/v2/retake-exams";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousCannotListRetakeExams() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(RETAKE_URL))
                .andReturn().getResponse().getStatus();

        Assertions.assertEquals(401, status, "anonymous user should be unauthorized");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotListRetakeExams() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(RETAKE_URL))
                .andReturn().getResponse().getStatus();

        Assertions.assertEquals(403, status, "student should be forbidden");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotListRetakeExams() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(RETAKE_URL))
                .andReturn().getResponse().getStatus();

        Assertions.assertEquals(403, status, "teacher should be forbidden");
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void academicOfficeCanListRetakeExams() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(RETAKE_URL))
                .andReturn().getResponse().getStatus();

        Assertions.assertEquals(200, status, "academic office should list retake exams");
    }
}

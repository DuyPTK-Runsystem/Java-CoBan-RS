package com.JavaTraining.BaiTap_RS.student.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:student-authorization;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class StudentAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "TEACHER")
    void fetchStudentsAllowsTeacher() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/students"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(200, status, "teacher should access the current Student API");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void fetchStudentsRejectsStudent() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/students"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not access the current Student API");
    }
}

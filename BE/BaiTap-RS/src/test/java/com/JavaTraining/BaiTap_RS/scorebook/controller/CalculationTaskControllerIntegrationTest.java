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
        "spring.datasource.url=jdbc:h2:mem:calculation-task-controller;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CalculationTaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousCannotListCalculationTasks() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders
                .get("/api/v2/scorebooks/calculation-tasks"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(401, status, "anonymous user should be unauthorized");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotListCalculationTasks() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders
                .get("/api/v2/scorebooks/calculation-tasks"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should be forbidden");
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void academicOfficeCanListCalculationTasks() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders
                .get("/api/v2/scorebooks/calculation-tasks"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(200, status, "academic office should list calculation tasks");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotRetryCalculationTask() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders
                .post("/api/v2/scorebooks/calculation-tasks/1/retry"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not retry calculation tasks");
    }
}

package com.JavaTraining.BaiTap_RS.student.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

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

    private static final String V2_STUDENTS_PATH = "/api/v2/students";

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

    @Test
    @WithMockUser(roles = "TEACHER")
    void fetchV2StudentsAllowsTeacherWithOnlyAssignedScope() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(V2_STUDENTS_PATH))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(200, status, "teacher v2 list should be server-scoped, not denied by route");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void fetchV2StudentsRejectsStudent() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(V2_STUDENTS_PATH))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not access the v2 student workspace API");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotMutateV2StudentLifecycle() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders
                        .patch(V2_STUDENTS_PATH + "/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"INACTIVE\"}"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "teacher should remain read-only in student v2");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void generateV2StudentCodeAllowsTeacher() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(V2_STUDENTS_PATH + "/code"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.studentCode").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void generateV2StudentCodeRejectsStudent() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.post(V2_STUDENTS_PATH + "/code"))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not generate codes through the v2 workspace API");
    }
}

package com.JavaTraining.BaiTap_RS.academic.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
                "spring.datasource.url=jdbc:h2:mem:semester-lock-auth;MODE=MySQL;DATABASE_TO_UPPER=false;"
                                + "NON_KEYWORDS=USER,ROLE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SemesterLockAuthorizationIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @WithMockUser(roles = "TEACHER")
        void teacherForbiddenForCompletenessReport() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/semesters/1/completeness-report"))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                Assertions.assertEquals(403, status, "teacher should not access completeness report");
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        void studentForbiddenForCompletenessReport() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/semesters/1/completeness-report"))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                Assertions.assertEquals(403, status, "student should not access completeness report");
        }

        @Test
        @WithMockUser(roles = "TEACHER")
        void teacherForbiddenForLock() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/semesters/1/lock"))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                Assertions.assertEquals(403, status, "teacher should not lock semester");
        }

        @Test
        @WithMockUser(roles = "TEACHER")
        void teacherForbiddenForReopen() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/semesters/1/reopen")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Lý do\"}"))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                Assertions.assertEquals(403, status, "teacher should not reopen semester");
        }

        @Test
        @WithMockUser(roles = "ACADEMIC_OFFICE")
        void academicOfficePassesAuthorizationOnReport() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/semesters/1/completeness-report"))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                // Since no report exists and semester 1 is absent, it returns 200 with preview
                // evaluation
                Assertions.assertEquals(200, status, "office should pass authorization");
        }
}

package com.JavaTraining.BaiTap_RS.attendance.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:class-attendance-summary-auth;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ClassAttendanceSummaryAuthorizationIntegrationTest {

    private static final String SUMMARY_ENDPOINT =
            "/api/v2/attendance/classes/1/summary?semesterId=1&from=2026-09-01&to=2026-09-30";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessClassSummary() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SUMMARY_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not access teacher class summary endpoint");
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void academicOfficePassesClassSummaryAuthorization() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SUMMARY_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(404, status,
                "academic office should pass authorization before missing class validation");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPassesClassSummaryAuthorization() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SUMMARY_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(404, status,
                "admin should pass authorization before missing class validation");
    }

    @Test
    void anonymousCannotAccessClassSummary() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SUMMARY_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(401, status, "anonymous request should be unauthorized");
    }
}

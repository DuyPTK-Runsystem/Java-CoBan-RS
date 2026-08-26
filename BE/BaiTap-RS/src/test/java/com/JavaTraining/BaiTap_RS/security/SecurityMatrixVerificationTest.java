package com.JavaTraining.BaiTap_RS.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-matrix-047;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals",
        "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.UnitTestContainsTooManyAsserts"
})
class SecurityMatrixVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousCannotAccessOperationalEndpoints() throws Exception {
        Assertions.assertEquals(401, get("/api/v2/scorebooks/audit-logs"));
        Assertions.assertEquals(401, get("/api/v2/scorebooks/calculation-tasks/failed"));
        Assertions.assertEquals(401, post("/api/v2/scorebooks/calculation-tasks/retry-all-failed"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessStaffOperationalEndpoints() throws Exception {
        Assertions.assertEquals(403, get("/api/v2/scorebooks/audit-logs"));
        Assertions.assertEquals(403, get("/api/v2/scorebooks/calculation-tasks/failed"));
        Assertions.assertEquals(403, post("/api/v2/scorebooks/calculation-tasks/retry-all-failed"));
        Assertions.assertEquals(403, post("/api/v2/students/1/transcripts/recalculate?academicYearId=1"));
        Assertions.assertEquals(403, get("/api/v2/transcripts/students/1/academic-years/1/status"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotUseCalculationAdministrationEndpoints() throws Exception {
        Assertions.assertEquals(403, get("/api/v2/scorebooks/calculation-tasks/failed"));
        Assertions.assertEquals(403, post("/api/v2/scorebooks/calculation-tasks/retry-all-failed"));
        Assertions.assertEquals(403, post("/api/v2/students/1/transcripts/recalculate?academicYearId=1"));
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void academicOfficePassesCoarseAuthorizationForOperationalEndpoints() throws Exception {
        Assertions.assertEquals(200, get("/api/v2/scorebooks/audit-logs"));
        Assertions.assertEquals(200, get("/api/v2/scorebooks/calculation-tasks/failed"));
        Assertions.assertEquals(200, post("/api/v2/scorebooks/calculation-tasks/retry-all-failed"));
    }

    private int get(String path) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(path)).andReturn().getResponse().getStatus();
    }

    private int post(String path) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)).andReturn().getResponse().getStatus();
    }
}

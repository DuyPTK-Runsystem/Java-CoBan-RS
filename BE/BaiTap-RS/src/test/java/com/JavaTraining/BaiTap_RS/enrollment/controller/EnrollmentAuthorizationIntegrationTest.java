package com.JavaTraining.BaiTap_RS.enrollment.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:enrollment-authorization;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class EnrollmentAuthorizationIntegrationTest {

    private static final String CLASSES_ENDPOINT = "/api/v2/classes";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReadClasses() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(CLASSES_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(200, status, "teacher should read class metadata");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotCreateClass() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.post(CLASSES_ENDPOINT)
                        .contentType("application/json")
                        .content("""
                                {
                                  "academicYearId": 1,
                                  "gradeLevelId": 1,
                                  "classCode": "6A",
                                  "status": "ACTIVE"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "teacher should not mutate class metadata");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotReadEnrollment() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(CLASSES_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not access enrollment APIs");
    }

    @Test
    void anonymousCannotReadEnrollment() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(CLASSES_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(401, status, "anonymous requests should be unauthorized");
    }
}

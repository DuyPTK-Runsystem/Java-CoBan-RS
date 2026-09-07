package com.JavaTraining.BaiTap_RS.attendance.controller;

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
        "spring.datasource.url=jdbc:h2:mem:office-attendance-auth;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AcademicOfficeAttendanceAuthorizationIntegrationTest {

    private static final String SESSIONS_ENDPOINT = "/api/v2/office/attendance-sessions";
    private static final String STUDENTS_ENDPOINT = "/api/v2/office/attendance-sessions/1/students";
    private static final String EXCEPTIONS_ENDPOINT = "/api/v2/office/attendance-sessions/1/exceptions/1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessOfficeAttendance() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not access office attendance endpoint");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotAccessOfficeAttendance() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "teacher should not access office attendance endpoint");
    }

    @Test
    void anonymousCannotAccessOfficeAttendance() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(401, status, "anonymous request should be unauthorized");
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void academicOfficeCanAccessEndpoint() throws Exception {
        // Will fail with 404 because session does not exist, but MUST NOT be 401 or 403
        int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(404, status, "academic office should pass authorization and fail with not found");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessEndpoint() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(404, status, "admin should pass authorization and fail with not found");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotMutateExceptionsViaOfficeApi() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.put(EXCEPTIONS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "LATE",
                                  "note": "Đi trễ"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "teacher should not mutate exceptions via office API");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotCreateSessionViaOfficeApi() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.post(SESSIONS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": 1,
                                  "semesterId": 1,
                                  "attendanceDate": "2026-09-05",
                                  "sessionPeriod": "MORNING"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "teacher should not create session via office API");
    }
}

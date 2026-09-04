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
                "spring.datasource.url=jdbc:h2:mem:attendance-history-auth;MODE=MySQL;DATABASE_TO_UPPER=false;"
                                + "NON_KEYWORDS=USER,ROLE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AttendanceHistoryAuthorizationIntegrationTest {

        private static final String HISTORY_ENDPOINT = "/api/v2/attendance/students/me/history";
        private static final String STUDENT_ID_HISTORY_ENDPOINT = "/api/v2/attendance/students/1/history";

        @Autowired
        private MockMvc mockMvc;

        @Test
        @WithMockUser(roles = "TEACHER")
        void teacherCannotAccessStudentHistory() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get(HISTORY_ENDPOINT))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                Assertions.assertEquals(403, status, "teacher should not access student-only history endpoint");
        }

        @Test
        @WithMockUser(roles = "ACADEMIC_OFFICE")
        void academicOfficeCannotAccessStudentHistory() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get(HISTORY_ENDPOINT))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                Assertions.assertEquals(403, status, "academic office should not access student-only history endpoint");
        }

        @Test
        void anonymousCannotAccessStudentHistory() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get(HISTORY_ENDPOINT))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                Assertions.assertEquals(401, status, "anonymous request should be unauthorized");
        }

        @Test
        @WithMockUser(roles = "ACADEMIC_OFFICE")
        void academicOfficeCanAccessStudentIdHistory() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENT_ID_HISTORY_ENDPOINT))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                Assertions.assertNotEquals(403, status, "academic office should have access to student-id history");
        }

        @Test
        @WithMockUser(roles = "TEACHER")
        void teacherCanAccessStudentIdHistory() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENT_ID_HISTORY_ENDPOINT))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                Assertions.assertNotEquals(403, status, "teacher should have access to student-id history");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminCanAccessStudentIdHistory() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENT_ID_HISTORY_ENDPOINT))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                Assertions.assertNotEquals(403, status, "admin should have access to student-id history");
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        void studentCannotAccessStudentIdHistory() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENT_ID_HISTORY_ENDPOINT))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                Assertions.assertEquals(403, status, "student should not access staff-only history endpoint");
        }

        @Test
        void anonymousCannotAccessStudentIdHistory() throws Exception {
                int status = mockMvc.perform(MockMvcRequestBuilders.get(STUDENT_ID_HISTORY_ENDPOINT))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                Assertions.assertEquals(401, status, "anonymous request should be unauthorized");
        }
}

package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scorebook-authorization;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ScorebookAuthorizationIntegrationTest {

    private static final String SCOREBOOK_ENDPOINT = "/api/v2/scorebooks/1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScorebookRepository scorebookRepository;

    @BeforeEach
    void clearScorebooks() {
        scorebookRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessScorebookMetadata() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SCOREBOOK_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "student should not access scorebook foundation endpoint");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherWithoutApplicationTeacherContextCannotAccessScorebook() throws Exception {
        Scorebook scorebook = scorebookRepository.save(new Scorebook(20L, ScorebookStatus.OPEN));
        int status = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/scorebooks/" + scorebook.getId()))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(403, status, "teacher without mapped teacher context should be forbidden");
    }

    @Test
    void anonymousCannotAccessScorebookMetadata() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SCOREBOOK_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(401, status, "anonymous request should be unauthorized");
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void academicOfficePassesAuthorizationBeforeNotFound() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(SCOREBOOK_ENDPOINT))
                .andReturn()
                .getResponse()
                .getStatus();

        Assertions.assertEquals(
                404,
                status,
                "office should pass authorization and fail only because scorebook is absent");
    }
}

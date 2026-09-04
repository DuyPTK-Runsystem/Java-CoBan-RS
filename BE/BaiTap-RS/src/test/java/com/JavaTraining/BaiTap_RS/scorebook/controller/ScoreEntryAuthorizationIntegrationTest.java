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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:score-entry-authorization;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ScoreEntryAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScorebookRepository scorebookRepository;

    @BeforeEach
    void clearDatabase() {
        scorebookRepository.deleteAll();
    }

    @Test
    void anonymousCannotAccessGridOrScoreMutation() throws Exception {
        int gridStatus = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/scorebooks/1/score-entries"))
                .andReturn().getResponse().getStatus();
        Assertions.assertEquals(401, gridStatus, "anonymous should be unauthorized on score grid");

        int putStatus = mockMvc.perform(MockMvcRequestBuilders.put("/api/v2/assessment-columns/1/students/1/score")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"scoreStatus":"SCORED","scoreValue":8.0}
                        """))
                .andReturn().getResponse().getStatus();
        Assertions.assertEquals(401, putStatus, "anonymous should be unauthorized on single score");

        int bulkStatus = mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/assessment-columns/1/scores/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"items":[{"studentId":1,"scoreStatus":"SCORED","scoreValue":8.0}]}
                        """))
                .andReturn().getResponse().getStatus();
        Assertions.assertEquals(401, bulkStatus, "anonymous should be unauthorized on bulk score");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessScoreEntryEndpoints() throws Exception {
        int gridStatus = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/scorebooks/1/score-entries"))
                .andReturn().getResponse().getStatus();
        Assertions.assertEquals(403, gridStatus, "student role should be forbidden on score grid");

        int putStatus = mockMvc.perform(MockMvcRequestBuilders.put("/api/v2/assessment-columns/1/students/1/score")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"scoreStatus":"SCORED","scoreValue":8.0}
                        """))
                .andReturn().getResponse().getStatus();
        Assertions.assertEquals(403, putStatus, "student role should be forbidden on single score");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherWithoutMappedProfileIsForbidden() throws Exception {
        Scorebook scorebook = scorebookRepository.save(new Scorebook(20L, ScorebookStatus.OPEN));

        int status = mockMvc.perform(MockMvcRequestBuilders.get(
                "/api/v2/scorebooks/" + scorebook.getId() + "/score-entries"))
                .andReturn().getResponse().getStatus();
        Assertions.assertEquals(403, status, "teacher without profile context should be forbidden");
    }

    @Test
    @WithMockUser(roles = "ACADEMIC_OFFICE")
    void academicOfficePassesAuthorization() throws Exception {
        int gridStatus = mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/scorebooks/1/score-entries"))
                .andReturn().getResponse().getStatus();
        Assertions.assertEquals(404, gridStatus, "office should pass auth and return 404 for missing scorebook");
    }
}

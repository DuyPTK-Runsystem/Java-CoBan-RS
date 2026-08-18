package com.JavaTraining.BaiTap_RS.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:expired-jwt-integration;MODE=MySQL;"
                + "DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.access-token-validity-in-seconds=0"
})
@AutoConfigureMockMvc
class ExpiredJwtIntegrationTest {

    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String ACCOUNT_PATH = "/api/v1/auth/account";
    private static final String USERNAME = "expired01";
    private static final String PASSWORD = "secret1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson()))
                .andReturn();

        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andReturn();
        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginResponse.path("data").path("access_token").asText();

        MvcResult accountResult = mockMvc.perform(MockMvcRequestBuilders
                        .get(ACCOUNT_PATH)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        Assertions.assertTrue(
                loginResult.getResponse().getStatus() == 200
                        && !accessToken.isBlank()
                        && accountResult.getResponse().getStatus() == 401
                        && accountResult.getResponse().getContentAsString().contains("\"statusCode\":401"),
                "expired tokens should return unauthorized");
    }

    private String registerJson() {
        return """
                {
                  "username": "%s",
                  "password": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(USERNAME, PASSWORD, PASSWORD);
    }

    private String loginJson() {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(USERNAME, PASSWORD);
    }
}

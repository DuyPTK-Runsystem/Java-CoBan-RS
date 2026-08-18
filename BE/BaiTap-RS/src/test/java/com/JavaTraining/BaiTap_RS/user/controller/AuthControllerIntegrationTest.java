package com.JavaTraining.BaiTap_RS.user.controller;

import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-integration;MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String ACCOUNT_PATH = "/api/v1/auth/account";
    private static final String LOGOUT_PATH = "/api/v1/auth/logout";
    private static final String USERNAME = "student01";
    private static final String PASSWORD = "secret1";
    private static final String DUPLICATE_MESSAGE = "Tên đăng nhập đã tồn tại";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Thông tin đăng nhập không hợp lệ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registerCreatesHashedUserAndReturnsSafeResponse() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(USERNAME, PASSWORD, PASSWORD)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        User user = userRepository.findByUsername(USERNAME).orElseThrow();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 201
                        && response.contains("\"statusCode\":201")
                        && response.contains("\"username\":\"" + USERNAME + "\"")
                        && !response.contains("\"password\"")
                        && userRepository.count() == 1
                        && !PASSWORD.equals(user.getPassword())
                        && user.getCreatedAt() != null
                        && user.getUpdatedAt() != null,
                "register should persist a hashed password and return a safe user response");
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(USERNAME, PASSWORD, PASSWORD)))
                .andReturn();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(USERNAME, PASSWORD, PASSWORD)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 409
                        && response.contains("\"statusCode\":409")
                        && response.contains("\"message\":\"" + DUPLICATE_MESSAGE + "\"")
                        && userRepository.count() == 1,
                "duplicate username should return conflict without creating another user");
    }

    @Test
    void registerRejectsMismatchedPasswordConfirmation() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(USERNAME, PASSWORD, "secret2")))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Assertions.assertTrue(
                result.getResponse().getStatus() == 400
                        && response.contains("\"statusCode\":400")
                        && response.contains("Mật khẩu xác nhận không khớp")
                        && userRepository.count() == 0,
                "mismatched password confirmation should not create a user");
    }

    @Test
    void registerRejectsInvalidBoundaryAndNonAsciiFields() throws Exception {
        MvcResult blankResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("", PASSWORD, PASSWORD)))
                .andReturn();
        MvcResult longUsernameResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("abcdefghijklmnopqrstu", PASSWORD, PASSWORD)))
                .andReturn();
        MvcResult nonAsciiResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(USERNAME, "mậtkhẩu", "mậtkhẩu")))
                .andReturn();

        Assertions.assertTrue(
                blankResult.getResponse().getStatus() == 400
                        && longUsernameResult.getResponse().getStatus() == 400
                        && nonAsciiResult.getResponse().getStatus() == 400
                        && userRepository.count() == 0,
                "blank, overlong and non-ASCII fields should fail validation");
    }

    @Test
    void loginAccountAndLogoutWorkWithJwt() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(USERNAME, PASSWORD, PASSWORD)))
                .andReturn();

        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USERNAME, PASSWORD)))
                .andReturn();
        String loginResponse = loginResult.getResponse().getContentAsString();
        String accessToken = extractAccessToken(loginResult);

        MvcResult accountResult = mockMvc.perform(MockMvcRequestBuilders
                        .get(ACCOUNT_PATH)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
        String accountResponse = accountResult.getResponse().getContentAsString();

        MvcResult logoutResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(LOGOUT_PATH)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        Assertions.assertTrue(
                loginResult.getResponse().getStatus() == 200
                        && accessToken != null
                        && !accessToken.isBlank()
                        && loginResponse.contains("\"access_token\"")
                        && loginResponse.contains("\"username\":\"" + USERNAME + "\"")
                        && !loginResponse.contains("\"password\"")
                        && accountResult.getResponse().getStatus() == 200
                        && accountResponse.contains("\"statusCode\":200")
                        && accountResponse.contains("\"username\":\"" + USERNAME + "\"")
                        && !accountResponse.contains("\"password\"")
                        && logoutResult.getResponse().getStatus() == 204,
                "JWT login should authenticate account and logout requests");
    }

    @Test
    void loginRejectsInvalidCredentialsAndAnonymousProtectedRequests() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(USERNAME, PASSWORD, PASSWORD)))
                .andReturn();

        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USERNAME, "wrong1")))
                .andReturn();
        MvcResult accountResult = mockMvc.perform(MockMvcRequestBuilders
                        .get(ACCOUNT_PATH))
                .andReturn();
        MvcResult logoutResult = mockMvc.perform(MockMvcRequestBuilders
                        .post(LOGOUT_PATH))
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        Assertions.assertTrue(
                loginResult.getResponse().getStatus() == 401
                        && loginResponse.contains("\"statusCode\":401")
                        && loginResponse.contains(INVALID_CREDENTIALS_MESSAGE)
                        && accountResult.getResponse().getStatus() == 403
                        && logoutResult.getResponse().getStatus() == 403,
                "invalid credentials and anonymous protected requests should be rejected");
    }

    private String registerJson(String username, String password, String confirmPassword) {
        return """
                {
                  "username": "%s",
                  "password": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(username, password, confirmPassword);
    }

    private String loginJson(String username, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("access_token").asText();
    }
}

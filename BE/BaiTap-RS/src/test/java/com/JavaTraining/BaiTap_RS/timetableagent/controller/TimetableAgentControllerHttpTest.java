package com.JavaTraining.BaiTap_RS.timetableagent.controller;

import com.JavaTraining.BaiTap_RS.common.error.GlobalExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.error.ValidationExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.util.FormatRestResponse;
import com.JavaTraining.BaiTap_RS.security.RestAccessDeniedHandler;
import com.JavaTraining.BaiTap_RS.security.RestAuthenticationEntryPoint;
import com.JavaTraining.BaiTap_RS.security.JwtTokenService;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentActionStateService;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentApprovalService;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentExecutionService;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentOrchestrator;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = TimetableAgentController.class)
@Import({
        TimetableAgentControllerHttpTest.SecurityTestConfiguration.class,
        GlobalExceptionHandler.class,
        ValidationExceptionHandler.class,
        FormatRestResponse.class
})
class TimetableAgentControllerHttpTest {

    private static final String PROPOSAL_URL = "/api/v4/timetable-agent/proposals/1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TimetableAgentOrchestrator orchestrator;

    @BeforeEach
    void resetSharedMockState() {
        reset(orchestrator);
    }

    @Test
    void anonymousRequestReturnsUnauthorized() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(PROPOSAL_URL))
                .andReturn().getResponse().getStatus();

        Assertions.assertEquals(401, status, "anonymous request must be rejected before controller execution");
    }

    @Test
    void teacherRequestReturnsForbidden() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(PROPOSAL_URL)
                        .with(user(principal("TEACHER"))))
                .andReturn().getResponse().getStatus();

        Assertions.assertEquals(403, status, "teacher role is outside the timetable-agent role allowlist");
    }

    @Test
    void administratorPassesAuthorizationAndResolvesActor() throws Exception {
        when(orchestrator.get(41L, 1L)).thenReturn(emptyProposal());

        int status = mockMvc.perform(MockMvcRequestBuilders.get(PROPOSAL_URL)
                        .with(user(principal("ADMIN"))))
                .andReturn().getResponse().getStatus();

        Assertions.assertEquals(200, status, "authorized admin request should reach the proposal service");
    }

    @Test
    void academicOfficePassesAuthorization() throws Exception {
        when(orchestrator.get(41L, 1L)).thenReturn(emptyProposal());

        int status = mockMvc.perform(MockMvcRequestBuilders.get(PROPOSAL_URL)
                        .with(user(principal("ACADEMIC_OFFICE"))))
                .andReturn().getResponse().getStatus();

        Assertions.assertEquals(200, status, "authorized academic-office request should reach the proposal service");
    }

    @Test
    void nonPositiveProposalIdReturnsValidationErrorEnvelope() throws Exception {
        var response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v4/timetable-agent/proposals/0")
                        .with(user(principal("ADMIN"))))
                .andReturn().getResponse();
        String body = response.getContentAsString();

        Assertions.assertTrue(response.getStatus() == 400 && body.contains("\"statusCode\":400"),
                "invalid path id must produce the standard 400 error envelope");
    }

    @Test
    void applicationConflictUsesStandardErrorEnvelope() throws Exception {
        when(orchestrator.get(41L, 1L)).thenThrow(new com.JavaTraining.BaiTap_RS.common.error.AppException(
                HttpStatus.CONFLICT, "stale proposal"));

        var response = mockMvc.perform(MockMvcRequestBuilders.get(PROPOSAL_URL)
                        .with(user(principal("ADMIN"))))
                .andReturn().getResponse();
        String body = response.getContentAsString();

        Assertions.assertTrue(response.getStatus() == 409 && body.contains("\"statusCode\":409")
                        && body.contains("stale proposal"),
                "conflict must preserve its status and message in the common error envelope");
    }

    @ParameterizedTest
    @CsvSource({
            "404,missing proposal",
            "422,invalid proposal",
            "502,provider rejected",
            "503,provider unavailable",
            "504,provider timed out"
    })
    void applicationErrorsPreserveHttpStatusAndEnvelope(int statusCode, String message) throws Exception {
        when(orchestrator.get(41L, 1L)).thenThrow(new com.JavaTraining.BaiTap_RS.common.error.AppException(
                HttpStatus.valueOf(statusCode), message));

        var response = mockMvc.perform(MockMvcRequestBuilders.get(PROPOSAL_URL)
                        .with(user(principal("ADMIN"))))
                .andReturn().getResponse();
        String body = response.getContentAsString();

        Assertions.assertTrue(response.getStatus() == statusCode
                        && body.contains("\"statusCode\":" + statusCode)
                        && body.contains(message),
                "application error must preserve HTTP status and common envelope for " + statusCode);
    }

    private static UserPrincipal principal(String roleCode) {
        User account = new User("agent-test", "not-used");
        org.springframework.test.util.ReflectionTestUtils.setField(account, "id", 41L);
        account.addRole(new Role(roleCode, roleCode, "test role"));
        return new UserPrincipal(account);
    }

    private static ResTimetableAgentProposalDTO emptyProposal() {
        return new ResTimetableAgentProposalDTO("proposal-1", 1, "hash", 9L, 2L,
                com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus.READY_FOR_REVIEW,
                null, "snapshot-1", java.util.List.of(), java.util.List.of(), "ready", null, null);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class SecurityTestConfiguration {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http,
                AuthenticationEntryPoint authenticationEntryPoint,
                AccessDeniedHandler accessDeniedHandler) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .build();
        }

        @Bean
        TimetableAgentOrchestrator orchestrator() {
            return mock(TimetableAgentOrchestrator.class);
        }

        @Bean
        TimetableAgentApprovalService approvalService() {
            return mock(TimetableAgentApprovalService.class);
        }

        @Bean
        TimetableAgentExecutionService executionService() {
            return mock(TimetableAgentExecutionService.class);
        }

        @Bean
        TimetableAgentActionStateService actionStateService() {
            return mock(TimetableAgentActionStateService.class);
        }

        @Bean
        JwtTokenService jwtTokenService() {
            return mock(JwtTokenService.class);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> {
                throw new UsernameNotFoundException("No test user: " + username);
            };
        }

        @Bean
        AuthenticationEntryPoint authenticationEntryPoint() {
            return new RestAuthenticationEntryPoint(new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Bean
        AccessDeniedHandler accessDeniedHandler() {
            return new RestAccessDeniedHandler(new com.fasterxml.jackson.databind.ObjectMapper());
        }
    }
}

package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import com.JavaTraining.BaiTap_RS.common.error.GlobalExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.error.ValidationExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.util.FormatRestResponse;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationExceptionHandler;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryPolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LibraryPolicyController.class)
@Import({ LibraryPolicyControllerHttpTest.SecurityTestConfiguration.class, GlobalExceptionHandler.class,
        ValidationExceptionHandler.class, LibraryCirculationExceptionHandler.class, FormatRestResponse.class })
class LibraryPolicyControllerHttpTest {

    private static final String POLICY_PATH = "/api/v2/library/policies/circulation";
    private static final String POLICY_REQUEST_JSON = "{\"expectedVersion\":\"LIB-POL-1\","
            + "\"effectiveAt\":\"2099-01-01T00:00:00\",\"maxActiveLoans\":5,"
            + "\"loanDurationDays\":14,\"maxRenewals\":2,\"renewalDurationDays\":7,"
            + "\"reservationPickupDays\":3,\"fineTiers\":[{\"throughDay\":7,"
            + "\"dailyRate\":5000},{\"throughDay\":null,\"dailyRate\":10000}],"
            + "\"fineCapPerLoan\":500000,\"fineSuspensionThreshold\":500000}";
    private static final String JSON_CONTENT_TYPE = "application/json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryPolicyService policyService;

    @MockitoBean
    private com.JavaTraining.BaiTap_RS.security.JwtTokenService jwtTokenService;

    @Test
    void allowsAdminAndLibrarianToReadPolicy() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(POLICY_PATH)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get(POLICY_PATH)
                        .with(SecurityMockMvcRequestPostProcessors.user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());

        verify(policyService, Mockito.times(2)).current();
    }

    @Test
    void allowsAdminAndLibrarianToUpdatePolicy() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(POLICY_PATH)
                        .contentType(JSON_CONTENT_TYPE)
                        .content(POLICY_REQUEST_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.put(POLICY_PATH)
                        .contentType(JSON_CONTENT_TYPE)
                        .content(POLICY_REQUEST_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());

        verify(policyService, Mockito.times(2)).update(Mockito.any());
    }

    @Test
    void deniesStudentAndAnonymousAccessBeforeCallingPolicyService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(POLICY_PATH)
                        .with(SecurityMockMvcRequestPostProcessors.user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get(POLICY_PATH))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(MockMvcRequestBuilders.put(POLICY_PATH)
                        .contentType(JSON_CONTENT_TYPE)
                        .content(POLICY_REQUEST_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        verify(policyService, never()).current();
        verify(policyService, never()).update(Mockito.any());
    }

    @TestConfiguration
    @EnableMethodSecurity
    /* package */ static class SecurityTestConfiguration {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http,
                com.JavaTraining.BaiTap_RS.security.RestAuthenticationEntryPoint authenticationEntryPoint,
                com.JavaTraining.BaiTap_RS.security.RestAccessDeniedHandler accessDeniedHandler) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .build();
        }

        @Bean
        LibraryPolicyService libraryPolicyService() {
            return Mockito.mock(LibraryPolicyService.class);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> {
                throw new UsernameNotFoundException("No test user: " + username);
            };
        }

        @Bean
        com.JavaTraining.BaiTap_RS.security.RestAuthenticationEntryPoint authenticationEntryPoint() {
            return new com.JavaTraining.BaiTap_RS.security.RestAuthenticationEntryPoint(new ObjectMapper());
        }

        @Bean
        com.JavaTraining.BaiTap_RS.security.RestAccessDeniedHandler accessDeniedHandler() {
            return new com.JavaTraining.BaiTap_RS.security.RestAccessDeniedHandler(new ObjectMapper());
        }
    }
}

package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import com.JavaTraining.BaiTap_RS.common.error.GlobalExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.error.ValidationExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.util.FormatRestResponse;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationExceptionHandler;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryFineService;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LibraryFineController.class)
@Import({ LibraryFineControllerHttpTest.SecurityTestConfiguration.class, GlobalExceptionHandler.class,
        ValidationExceptionHandler.class, LibraryCirculationExceptionHandler.class, FormatRestResponse.class })
class LibraryFineControllerHttpTest {

    private static final String PAY_PATH = "/api/v2/fines/42/pay";
    private static final String WAIVE_PATH = "/api/v2/fines/42/waive";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryFineService fineService;

    @MockitoBean
    private com.JavaTraining.BaiTap_RS.security.JwtTokenService jwtTokenService;

    @Test
    void allowsAdminAndLibrarianToRecordPayment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(PAY_PATH)
                        .contentType("application/json")
                        .content("{\"reference\":\"RCPT-42\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.post(PAY_PATH)
                        .contentType("application/json")
                        .content("{\"reference\":\"RCPT-43\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());

        Mockito.verify(fineService, Mockito.times(2)).pay(Mockito.eq(42L), Mockito.anyString());
    }

    @Test
    void restrictsWaiverToAdminAndBlocksStudentPayment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(WAIVE_PATH)
                        .contentType("application/json")
                        .content("{\"reason\":\"approved correction\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.post(WAIVE_PATH)
                        .contentType("application/json")
                        .content("{\"reason\":\"requested\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.post(PAY_PATH)
                        .contentType("application/json")
                        .content("{\"reference\":\"RCPT-44\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        Mockito.verify(fineService).waive(42L, "approved correction");
        Mockito.verify(fineService, Mockito.never()).waive(42L, "requested");
        Mockito.verify(fineService, Mockito.never()).pay(42L, "RCPT-44");
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
        LibraryFineService libraryFineService() {
            return Mockito.mock(LibraryFineService.class);
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

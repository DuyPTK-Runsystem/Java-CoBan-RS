package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.JavaTraining.BaiTap_RS.common.error.GlobalExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.error.ValidationExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.util.FormatRestResponse;
import com.JavaTraining.BaiTap_RS.library.circulation.batch.LibraryBatchService;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryBatchRunDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LibraryBatchController.class)
@Import({ LibraryBatchControllerHttpTest.SecurityTestConfiguration.class, GlobalExceptionHandler.class,
        ValidationExceptionHandler.class, LibraryCirculationExceptionHandler.class, FormatRestResponse.class })
class LibraryBatchControllerHttpTest {

    private static final String BATCH_RUN_PATH = "/api/v2/library/batch-jobs/overdue-fine";
    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryBatchService batchService;

    @MockitoBean
    private com.JavaTraining.BaiTap_RS.security.JwtTokenService jwtTokenService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        Mockito.reset(batchService);
    }

    @Test
    void allowsLibrarianToRunBatchForPastOrCurrentDate() throws Exception {
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        LibraryBatchRunDTO runDto = new LibraryBatchRunDTO(1L, "libraryOverdueFineJob", today,
                "COMPLETED", 5, 0, OffsetDateTime.now(LIBRARY_ZONE), OffsetDateTime.now(LIBRARY_ZONE));
        when(batchService.run(eq(today))).thenReturn(runDto);

        mockMvc.perform(MockMvcRequestBuilders.post(BATCH_RUN_PATH)
                        .contentType("application/json")
                        .content("{\"runDate\":\"" + today + "\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId").value(1))
                .andExpect(jsonPath("$.data.runDate").value(today.toString()));

        verify(batchService).run(eq(today));
    }

    @Test
    void rejectsFutureRunDateWithBadRequestAndMeaningfulCode() throws Exception {
        LocalDate futureDate = LocalDate.now(LIBRARY_ZONE).plusDays(5);
        when(batchService.run(eq(futureDate))).thenThrow(new LibraryCirculationException(
                HttpStatus.BAD_REQUEST, "INVALID_BATCH_RUN_DATE", "Ngày chạy tính phí không được ở tương lai"));

        mockMvc.perform(MockMvcRequestBuilders.post(BATCH_RUN_PATH)
                        .contentType("application/json")
                        .content("{\"runDate\":\"" + futureDate + "\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BATCH_RUN_DATE"))
                .andExpect(jsonPath("$.message").value("Ngày chạy tính phí không được ở tương lai"));

        verify(batchService).run(eq(futureDate));
    }

    @Test
    void deniesUnauthorizedUsers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BATCH_RUN_PATH)
                        .contentType("application/json")
                        .content("{\"runDate\":\"2026-10-09\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(MockMvcRequestBuilders.post(BATCH_RUN_PATH)
                        .contentType("application/json")
                        .content("{\"runDate\":\"2026-10-09\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        verify(batchService, never()).run(Mockito.any());
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
        LibraryBatchService libraryBatchService() {
            return Mockito.mock(LibraryBatchService.class);
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


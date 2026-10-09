package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.common.error.GlobalExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.error.ValidationExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.util.FormatRestResponse;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryFineDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLoanDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLostResultDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineType;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationExceptionHandler;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryLostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LibraryLostController.class)
@Import({ LibraryLostControllerHttpTest.SecurityTestConfiguration.class, GlobalExceptionHandler.class,
        ValidationExceptionHandler.class, LibraryCirculationExceptionHandler.class, FormatRestResponse.class })
class LibraryLostControllerHttpTest {

    @Autowired private MockMvc mockMvc;
    @Autowired @Qualifier("libraryLostService") private LibraryLostService lostService;
    @MockitoBean private com.JavaTraining.BaiTap_RS.security.JwtTokenService jwtTokenService;

    @Test
    void returnsLoanAndLostFineInApprovedResponseShape() throws Exception {
        java.time.OffsetDateTime timestamp = java.time.OffsetDateTime.of(2026, 10, 9, 10, 0, 0, 0,
                java.time.ZoneOffset.ofHours(7));
        LibraryLoanDTO loan = new LibraryLoanDTO(22L, 7L, 31L, "LOST-31", 12L, "Lost book", "CARD-51",
                LoanStatus.LOST, timestamp.minusDays(25), timestamp.minusDays(5), null, timestamp, 0, "LIB-POL-1");
        LibraryFineDTO fine = new LibraryFineDTO(88L, 22L, FineType.LOST_ITEM, FineStatus.UNPAID,
                new BigDecimal("173456.78"), "VND", LocalDate.of(2026, 10, 9), "LIB-POL-1",
                null, null, null, null, false);
        when(lostService.markLost("LOST-31", "confirmed missing"))
                .thenReturn(new LibraryLostResultDTO(loan, fine));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/book-copies/LOST-31/lost")
                        .contentType("application/json")
                        .content("{\"reason\":\"confirmed missing\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loan.loanId").value(22))
                .andExpect(jsonPath("$.data.loan.status").value("LOST"))
                .andExpect(jsonPath("$.data.loan.lostAt").value("2026-10-09T10:00:00+07:00"))
                .andExpect(jsonPath("$.data.fine.fineId").value(88))
                .andExpect(jsonPath("$.data.fine.type").value("LOST_ITEM"))
                .andExpect(jsonPath("$.data.fine.amount").isString())
                .andExpect(jsonPath("$.data.fine.amount").value("173456.78"));

        verify(lostService).markLost("LOST-31", "confirmed missing");
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

        @Bean("libraryLostService")
        LibraryLostService libraryLostService() {
            return Mockito.mock(LibraryLostService.class);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> { throw new UsernameNotFoundException("No test user: " + username); };
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

package com.JavaTraining.BaiTap_RS.library.circulation.controller;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.GlobalExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.error.ValidationExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.util.FormatRestResponse;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.ReservationStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationExceptionHandler;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LibraryReservationController.class)
@Import({ LibraryReservationControllerHttpTest.SecurityTestConfiguration.class, GlobalExceptionHandler.class,
        ValidationExceptionHandler.class, LibraryCirculationExceptionHandler.class, FormatRestResponse.class })
class LibraryReservationControllerHttpTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private LibraryReservationService reservationService;
    @MockitoBean private com.JavaTraining.BaiTap_RS.security.JwtTokenService jwtTokenService;

    @Test
    void bindsBookFilterAlongsideExistingQueryParameters() throws Exception {
        when(reservationService.page(101L, 12L, ReservationStatus.WAITING, 1, 10))
                .thenReturn(new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(1, 10, 0, 0), List.of()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/reservations")
                        .param("bookId", "101").param("patronId", "12").param("status", "WAITING")
                        .param("page", "1").param("pageSize", "10")
                        .with(SecurityMockMvcRequestPostProcessors.user("staff").roles("LIBRARIAN")))
                .andExpect(status().isOk());

        verify(reservationService).page(101L, 12L, ReservationStatus.WAITING, 1, 10);
    }

    @Test
    void rejectsNonPositiveBookFilterBeforeCallingService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/reservations")
                        .param("bookId", "0")
                        .with(SecurityMockMvcRequestPostProcessors.user("student").roles("STUDENT")))
                .andExpect(status().isBadRequest());

        verify(reservationService, never()).page(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void keepsEndpointAuthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/reservations")).andExpect(status().isUnauthorized());
        verify(reservationService, never()).page(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyInt(), Mockito.anyInt());
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
        LibraryReservationService libraryReservationService() {
            return Mockito.mock(LibraryReservationService.class);
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

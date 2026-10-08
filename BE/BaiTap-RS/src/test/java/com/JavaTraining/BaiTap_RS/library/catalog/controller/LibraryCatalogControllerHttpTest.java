package com.JavaTraining.BaiTap_RS.library.catalog.controller;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.GlobalExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.error.ValidationExceptionHandler;
import com.JavaTraining.BaiTap_RS.common.util.FormatRestResponse;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogException;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryCatalogExceptionHandler;
import com.JavaTraining.BaiTap_RS.library.catalog.service.BookCopyService;
import com.JavaTraining.BaiTap_RS.library.catalog.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(controllers = { BookController.class, BookCopyController.class, BookCopyBatchController.class,
        BookBarcodeController.class })
@Import({ LibraryCatalogControllerHttpTest.SecurityTestConfiguration.class, GlobalExceptionHandler.class,
        ValidationExceptionHandler.class, LibraryCatalogExceptionHandler.class, FormatRestResponse.class })
class LibraryCatalogControllerHttpTest {

    private static final String BOOK_SORT = "title,asc";
    private static final String COPY_BARCODE_SORT = "barcode,asc";
    private static final String BOOK_TITLE = "Book";
    private static final String BOOK_AUTHOR = "Author";
    private static final String LIBRARIAN = "catalog-librarian";
    private static final String STUDENT = "student-reader";
    private static final String INVALID_CATALOG_REQUEST = "INVALID_CATALOG_REQUEST";
    private static final String STUDENT_ROLE = "STUDENT";
    private static final String LIBRARIAN_ROLE = "LIBRARIAN";
    private static final String BOOKS_PATH = "/api/v2/books";
    private static final String BOOK_COPIES_PATH = "/api/v2/books/7/copies";
    private static final String BOOK_DETAIL_PATH = "/api/v2/books/7";
    private static final String SORT_PARAMETER = "sort";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String BOOK_REQUEST_JSON = "{\"title\":\"" + BOOK_TITLE + "\",\"author\":\""
            + BOOK_AUTHOR + "\"}";
    private static final String ERROR_CODE_JSON_PATH = "$.code";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookService bookService;

    @Autowired
    private BookCopyService bookCopyService;

    @MockitoBean
    private com.JavaTraining.BaiTap_RS.security.JwtTokenService jwtTokenService;

    @org.junit.jupiter.api.BeforeEach
    void clearServiceInteractions() {
        Mockito.clearInvocations(bookService, bookCopyService);
    }

    @Nested
    /* default */ class SortingTests {

    @Test
    void commaDelimitedBookSortReachesServiceAsOneSortExpression() throws Exception {
        Mockito.when(bookService.search(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyList()))
                .thenReturn(emptyBookPage());

        mockMvc.perform(MockMvcRequestBuilders.get(BOOKS_PATH)
                        .param(SORT_PARAMETER, BOOK_SORT)
                        .with(SecurityMockMvcRequestPostProcessors.user(STUDENT).roles(STUDENT_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(bookService).search(ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(0), ArgumentMatchers.eq(20), ArgumentMatchers.eq(java.util.List.of(BOOK_SORT)));
    }

    @Test
    void repeatedBookSortParametersRemainSeparateExpressions() throws Exception {
        Mockito.when(bookService.search(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyList()))
                .thenReturn(emptyBookPage());

        mockMvc.perform(MockMvcRequestBuilders.get(BOOKS_PATH)
                        .param(SORT_PARAMETER, BOOK_SORT, "author,desc")
                        .with(SecurityMockMvcRequestPostProcessors.user(STUDENT).roles(STUDENT_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(bookService).search(ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(0), ArgumentMatchers.eq(20),
                ArgumentMatchers.eq(java.util.List.of(BOOK_SORT, "author,desc")));
    }

    @Test
    void commaDelimitedCopySortReachesServiceAsOneSortExpression() throws Exception {
        Mockito.when(bookCopyService.list(ArgumentMatchers.eq(7L), ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(0), ArgumentMatchers.eq(20), ArgumentMatchers.anyList()))
                .thenReturn(emptyCopyPage());

        mockMvc.perform(MockMvcRequestBuilders.get(BOOK_COPIES_PATH)
                        .param(SORT_PARAMETER, COPY_BARCODE_SORT)
                        .with(SecurityMockMvcRequestPostProcessors.user(STUDENT).roles(STUDENT_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(bookCopyService).list(ArgumentMatchers.eq(7L), ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(0), ArgumentMatchers.eq(20), ArgumentMatchers.eq(java.util.List.of(COPY_BARCODE_SORT)));
    }

    @Test
    void repeatedCopySortParametersRemainSeparateExpressions() throws Exception {
        Mockito.when(bookCopyService.list(ArgumentMatchers.eq(7L), ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(0), ArgumentMatchers.eq(20), ArgumentMatchers.anyList()))
                .thenReturn(emptyCopyPage());

        mockMvc.perform(MockMvcRequestBuilders.get(BOOK_COPIES_PATH)
                        .param(SORT_PARAMETER, COPY_BARCODE_SORT, "status,desc")
                        .with(SecurityMockMvcRequestPostProcessors.user(STUDENT).roles(STUDENT_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(bookCopyService).list(ArgumentMatchers.eq(7L), ArgumentMatchers.eq(null), ArgumentMatchers.eq(null), ArgumentMatchers.eq(0), ArgumentMatchers.eq(20),
                ArgumentMatchers.eq(java.util.List.of(COPY_BARCODE_SORT, "status,desc")));
    }

    }

    @Nested
    /* default */ class PermissionTests {

    @Test
    void authenticatedStudentCanReadButCannotCreateCatalogMetadata() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BOOKS_PATH)
                        .contentType(JSON_CONTENT_TYPE)
                        .content(BOOK_REQUEST_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user(STUDENT).roles(STUDENT_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        Mockito.verify(bookService, Mockito.never()).create(ArgumentMatchers.any(com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookDTO.class));
    }

    @Test
    void studentCannotAddCopies() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BOOK_COPIES_PATH)
                        .header("Idempotency-Key", "student-key")
                        .contentType("application/json")
                        .content("{\"quantity\":1,\"referenceOnly\":false}")
                        .with(SecurityMockMvcRequestPostProcessors.user(STUDENT).roles(STUDENT_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        Mockito.verify(bookCopyService, Mockito.never()).createBatch(ArgumentMatchers.eq(7L), ArgumentMatchers.any(com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookCopiesDTO.class), ArgumentMatchers.eq("student-key"));
    }

    @Test
    void librarianCanCreateCatalogMetadata() throws Exception {
        Mockito.when(bookService.create(ArgumentMatchers.any(com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookDTO.class))).thenReturn(new com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookDetailDTO(
                7L, null, BOOK_TITLE, BOOK_AUTHOR, null, null, null, null, null, 0, 0, 0L));

        mockMvc.perform(MockMvcRequestBuilders.post(BOOKS_PATH)
                        .contentType(JSON_CONTENT_TYPE)
                        .content(BOOK_REQUEST_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user(LIBRARIAN).roles(LIBRARIAN_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().string("Location",
                        org.hamcrest.Matchers.endsWith(BOOK_DETAIL_PATH)));

        Mockito.verify(bookService).create(ArgumentMatchers.any(com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookDTO.class));
    }

    }

    @Nested
    /* default */ class ValidationTests {

    @Test
    void rejectsInvalidBatchQuantityBeforeCallingService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BOOK_COPIES_PATH)
                        .header("Idempotency-Key", "batch-1")
                        .contentType("application/json")
                        .content("{\"quantity\":0,\"referenceOnly\":false}")
                        .with(SecurityMockMvcRequestPostProcessors.user(LIBRARIAN).roles(LIBRARIAN_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(ERROR_CODE_JSON_PATH).value(INVALID_CATALOG_REQUEST));

        Mockito.verify(bookCopyService, Mockito.never()).createBatch(ArgumentMatchers.eq(7L), ArgumentMatchers.any(com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookCopiesDTO.class), ArgumentMatchers.eq("batch-1"));
    }

    @Test
    void missingIdempotencyHeaderReturnsStableCode() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BOOK_COPIES_PATH)
                        .contentType(JSON_CONTENT_TYPE)
                        .content("{\"quantity\":1,\"referenceOnly\":false}")
                        .with(SecurityMockMvcRequestPostProcessors.user(LIBRARIAN).roles(LIBRARIAN_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(ERROR_CODE_JSON_PATH).value("INVALID_IDEMPOTENCY_KEY"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

        Mockito.verify(bookCopyService, Mockito.never()).createBatch(ArgumentMatchers.eq(7L), ArgumentMatchers.any(com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookCopiesDTO.class), ArgumentMatchers.any());
    }

    @Test
    void bookUpdateRequiresExpectedVersion() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(BOOK_DETAIL_PATH)
                        .contentType(JSON_CONTENT_TYPE)
                        .content(BOOK_REQUEST_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user(LIBRARIAN).roles(LIBRARIAN_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(ERROR_CODE_JSON_PATH).value(INVALID_CATALOG_REQUEST));

        Mockito.verify(bookService, Mockito.never()).update(ArgumentMatchers.eq(7L), ArgumentMatchers.any());
    }

    @Test
    void bookUpdateRejectsNegativeExpectedVersionBeforeCallingService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(BOOK_DETAIL_PATH)
                        .contentType(JSON_CONTENT_TYPE)
                        .content("{\"expectedVersion\":-1,\"title\":\"" + BOOK_TITLE + "\",\"author\":\"" + BOOK_AUTHOR + "\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user(LIBRARIAN).roles(LIBRARIAN_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(ERROR_CODE_JSON_PATH).value(INVALID_CATALOG_REQUEST));

        Mockito.verify(bookService, Mockito.never()).update(ArgumentMatchers.eq(7L), ArgumentMatchers.any());
    }

    @Test
    void copyUpdateRejectsNegativeExpectedVersionBeforeCallingService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch(
                        "/api/v2/book-copies/LIB-000000007")
                        .contentType("application/json")
                        .content("{\"expectedVersion\":-1}")
                        .with(SecurityMockMvcRequestPostProcessors.user(LIBRARIAN).roles(LIBRARIAN_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(ERROR_CODE_JSON_PATH).value(INVALID_CATALOG_REQUEST));

        Mockito.verify(bookCopyService, Mockito.never()).update(ArgumentMatchers.eq("LIB-000000007"), ArgumentMatchers.any());
    }

    @Test
    void archiveRequiresExpectedVersion() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(BOOK_DETAIL_PATH)
                        .with(SecurityMockMvcRequestPostProcessors.user(LIBRARIAN).roles(LIBRARIAN_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(ERROR_CODE_JSON_PATH).value(INVALID_CATALOG_REQUEST));

        Mockito.verify(bookService, Mockito.never()).archive(ArgumentMatchers.eq(7L), ArgumentMatchers.any());
    }

    @Test
    void unsupportedAvailabilityReturnsStableCatalogCode() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BOOKS_PATH)
                        .param("availability", "MAYBE")
                        .with(SecurityMockMvcRequestPostProcessors.user(STUDENT).roles(STUDENT_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(ERROR_CODE_JSON_PATH).value("INVALID_CATALOG_QUERY"));

        Mockito.verify(bookService, Mockito.never()).search(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyList());
    }

    }

    @Nested
    /* default */ class BarcodeTests {

    @Test
    void barcodeEndpointReturnsUnwrappedPngBytesWithNoStoreHeader() throws Exception {
        byte[] pngBytes = { 1, 2, 3 };
        Mockito.when(bookCopyService.barcodePng("LIB-000000501")).thenReturn(pngBytes);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/book-copies/LIB-000000501/barcode.png")
                        .with(SecurityMockMvcRequestPostProcessors.user(STUDENT).roles(STUDENT_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("image/png"))
                .andExpect(MockMvcResultMatchers.header().string("Cache-Control", "no-store"))
                .andExpect(MockMvcResultMatchers.content().bytes(pngBytes));
    }

    @Test
    void barcodeNotFoundKeepsLibraryCodeOnTheSplitController() throws Exception {
        Mockito.when(bookCopyService.barcodePng("LIB-MISSING")).thenThrow(new LibraryCatalogException(
                org.springframework.http.HttpStatus.NOT_FOUND, "COPY_NOT_FOUND", "Book copy not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/book-copies/LIB-MISSING/barcode.png")
                        .with(SecurityMockMvcRequestPostProcessors.user(STUDENT).roles(STUDENT_ROLE)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath(ERROR_CODE_JSON_PATH).value("COPY_NOT_FOUND"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    }

    @Nested
    /* default */ class AnonymousAccessTests {

    @Test
    void anonymousMutationIsRejectedBeforeCallingService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BOOKS_PATH)
                        .contentType(JSON_CONTENT_TYPE)
                        .content(BOOK_REQUEST_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        Mockito.verify(bookService, Mockito.never()).create(ArgumentMatchers.any(com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.requests.ReqCreateBookDTO.class));
    }

    }

    private ResultPaginationDTO<com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookSummaryDTO> emptyBookPage() {
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(0, 20, 0, 0), java.util.List.of());
    }

    private ResultPaginationDTO<com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response.ResBookCopyDTO> emptyCopyPage() {
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(0, 20, 0, 0), java.util.List.of());
    }

    @TestConfiguration
    @EnableMethodSecurity
    /* package */ static class SecurityTestConfiguration {

        @Bean
        /* package */
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
        /* package */
        BookService bookService() {
            return Mockito.mock(BookService.class);
        }

        @Bean
        /* package */
        BookCopyService bookCopyService() {
            return Mockito.mock(BookCopyService.class);
        }

        @Bean
        /* package */
        UserDetailsService userDetailsService() {
            return username -> {
                throw new UsernameNotFoundException("No test user: " + username);
            };
        }

        @Bean
        /* package */
        com.JavaTraining.BaiTap_RS.security.RestAuthenticationEntryPoint authenticationEntryPoint() {
            return new com.JavaTraining.BaiTap_RS.security.RestAuthenticationEntryPoint(new ObjectMapper());
        }

        @Bean
        /* package */
        com.JavaTraining.BaiTap_RS.security.RestAccessDeniedHandler accessDeniedHandler() {
            return new com.JavaTraining.BaiTap_RS.security.RestAccessDeniedHandler(new ObjectMapper());
        }
    }
}

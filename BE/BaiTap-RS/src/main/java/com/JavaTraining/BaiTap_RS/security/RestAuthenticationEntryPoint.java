package com.JavaTraining.BaiTap_RS.security;

import java.io.IOException;

import com.JavaTraining.BaiTap_RS.common.dto.RestResponse;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Object body = isLibraryCatalogPath(request.getRequestURI())
                ? new LibraryErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                        "AUTHENTICATION_REQUIRED", "Yêu cầu xác thực để tiếp tục", java.util.Map.of())
                : RestResponse.failure(HttpServletResponse.SC_UNAUTHORIZED,
                        "Chưa xác thực", "Yêu cầu xác thực để tiếp tục");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private boolean isLibraryCatalogPath(String path) {
        return "/api/v2/books".equals(path) || path.startsWith("/api/v2/books/")
                || "/api/v2/book-copies".equals(path) || path.startsWith("/api/v2/book-copies/");
    }
}

package com.JavaTraining.BaiTap_RS.security;

import java.io.IOException;

import com.JavaTraining.BaiTap_RS.common.dto.RestResponse;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Object body = isLibraryCatalogPath(request.getRequestURI())
                ? new LibraryErrorResponse(HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                        "LIBRARY_RESOURCE_FORBIDDEN", "Bạn không có quyền thực hiện thao tác này",
                        java.util.Map.of())
                : RestResponse.failure(HttpServletResponse.SC_FORBIDDEN,
                        "Không có quyền", "Bạn không có quyền thực hiện thao tác này");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private boolean isLibraryCatalogPath(String path) {
        return "/api/v2/books".equals(path) || path.startsWith("/api/v2/books/")
                || "/api/v2/book-copies".equals(path) || path.startsWith("/api/v2/book-copies/");
    }
}

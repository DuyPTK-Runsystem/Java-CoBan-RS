package com.JavaTraining.BaiTap_RS.common.util;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.dto.RestResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class FormatRestResponse implements ResponseBodyAdvice<Object> {

    private static final String DEFAULT_SUCCESS_MESSAGE = "CALL API SUCCESS";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        String path = request.getURI().getPath();
        if (shouldSkip(path, body)) {
            return body;
        }

        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int statusCode = servletResponse.getStatus();
        if (statusCode >= 400) {
            return body;
        }

        RestResponse<Object> restResponse = new RestResponse<>();
        restResponse.setStatusCode(statusCode);
        restResponse.setData(body);
        restResponse.setMessage(resolveMessage(returnType));
        return restResponse;
    }

    private boolean shouldSkip(String path, Object body) {
        return path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || body instanceof RestResponse<?>
                || body instanceof String
                || body instanceof byte[]
                || body instanceof Resource;
    }

    private String resolveMessage(MethodParameter returnType) {
        ApiMessage apiMessage = returnType.getMethodAnnotation(ApiMessage.class);
        if (apiMessage == null) {
            return DEFAULT_SUCCESS_MESSAGE;
        }
        return apiMessage.value();
    }
}

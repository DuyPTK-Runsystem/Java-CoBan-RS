package com.JavaTraining.BaiTap_RS.common.error;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.dto.RestResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception) {
        return badRequest(formatBindingErrors(exception.getBindingResult()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<RestResponse<Void>> handleMethodValidationException(
            HandlerMethodValidationException exception) {
        List<String> messages = exception.getParameterValidationResults().stream()
                .flatMap(result -> formatParameterValidationErrors(result).stream())
                .toList();
        return badRequest(messages);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RestResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        List<String> messages = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return badRequest(messages);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RestResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        return badRequest(List.of(exception.getName() + ": Giá trị không hợp lệ"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestResponse<Void>> handleUnreadableRequestException(
            HttpMessageNotReadableException exception) {
        return badRequest(List.of("request: Dữ liệu request không hợp lệ"));
    }

    private List<String> formatBindingErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();
    }

    private List<String> formatParameterValidationErrors(ParameterValidationResult result) {
        String parameterName = result.getMethodParameter().getParameterName();
        String field = parameterName == null ? "request" : parameterName;
        return result.getResolvableErrors().stream()
                .map(error -> formatParameterValidationError(field, error))
                .toList();
    }

    private String formatParameterValidationError(String field, MessageSourceResolvable error) {
        return field + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<RestResponse<Void>> badRequest(Object message) {
        RestResponse<Void> response = RestResponse.failure(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message);
        return ResponseEntity.badRequest().body(response);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}

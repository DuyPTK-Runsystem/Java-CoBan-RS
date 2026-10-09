package com.JavaTraining.BaiTap_RS.library.circulation.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {
        com.JavaTraining.BaiTap_RS.library.circulation.controller.LibraryLoanController.class,
        com.JavaTraining.BaiTap_RS.library.circulation.controller.LibraryReservationController.class,
        com.JavaTraining.BaiTap_RS.library.circulation.controller.LibraryFineController.class,
        com.JavaTraining.BaiTap_RS.library.circulation.controller.LibraryPolicyController.class,
        com.JavaTraining.BaiTap_RS.library.circulation.controller.LibraryLostController.class,
        com.JavaTraining.BaiTap_RS.library.circulation.controller.LibraryBatchController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LibraryCirculationExceptionHandler {

    @ExceptionHandler(LibraryCirculationException.class)
    public ResponseEntity<LibraryErrorResponse> handle(LibraryCirculationException exception) {
        HttpStatus status = exception.getStatus();
        return ResponseEntity.status(status).body(new LibraryErrorResponse(status.value(),
                status.getReasonPhrase(), exception.getCode(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<LibraryErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new LibraryErrorResponse(400, "Bad Request",
                "INVALID_LIBRARY_REQUEST", "Dữ liệu không hợp lệ", fields));
    }

    @ExceptionHandler({ ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class, HttpMessageNotReadableException.class })
    public ResponseEntity<LibraryErrorResponse> handleShape(Exception exception) {
        return ResponseEntity.badRequest().body(new LibraryErrorResponse(400, "Bad Request",
                "INVALID_LIBRARY_REQUEST", "Yêu cầu không hợp lệ", Map.of()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<LibraryErrorResponse> handleStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status).body(new LibraryErrorResponse(status.value(),
                status.getReasonPhrase(), "LIBRARY_RESOURCE_FORBIDDEN", "Không có quyền truy cập", Map.of()));
    }
}

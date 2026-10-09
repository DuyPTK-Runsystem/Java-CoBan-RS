package com.JavaTraining.BaiTap_RS.library.patron.exception;

import java.util.Map;

import com.JavaTraining.BaiTap_RS.library.card.controller.LibraryCardController;
import com.JavaTraining.BaiTap_RS.library.card.controller.LibraryPatronCardHistoryController;
import com.JavaTraining.BaiTap_RS.library.catalog.exception.LibraryErrorResponse;
import com.JavaTraining.BaiTap_RS.library.patron.controller.LibraryPatronController;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = { LibraryPatronController.class, LibraryCardController.class,
        LibraryPatronCardHistoryController.class })
public class LibraryPatronExceptionHandler {

    @ExceptionHandler(LibraryPatronException.class)
    public ResponseEntity<LibraryErrorResponse> handle(LibraryPatronException exception) {
        HttpStatus status = exception.getStatus();
        return ResponseEntity.status(status).body(new LibraryErrorResponse(status.value(),
                status.getReasonPhrase(), exception.getCode(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler({ ConstraintViolationException.class, HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class })
    public ResponseEntity<LibraryErrorResponse> handleRequestShape(Exception exception) {
        return ResponseEntity.badRequest().body(new LibraryErrorResponse(400, "Bad Request",
                "INVALID_LIBRARY_REQUEST", "Yêu cầu không hợp lệ", Map.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<LibraryErrorResponse> handleConstraint(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new LibraryErrorResponse(409, "Conflict",
                "LIBRARY_DATA_CONFLICT", "Dữ liệu thư viện xung đột với thao tác khác", Map.of()));
    }

    @ExceptionHandler({ OptimisticLockingFailureException.class, OptimisticLockException.class })
    public ResponseEntity<LibraryErrorResponse> handleOptimisticConflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new LibraryErrorResponse(409, "Conflict",
                "LIBRARY_VERSION_CONFLICT", "Dữ liệu đã được cập nhật; vui lòng tải lại", Map.of()));
    }
}

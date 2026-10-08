package com.JavaTraining.BaiTap_RS.library.catalog.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        com.JavaTraining.BaiTap_RS.library.catalog.controller.BookController.class,
        com.JavaTraining.BaiTap_RS.library.catalog.controller.BookCopyController.class,
        com.JavaTraining.BaiTap_RS.library.catalog.controller.BookCopyBatchController.class,
        com.JavaTraining.BaiTap_RS.library.catalog.controller.BookBarcodeController.class })
public class LibraryCatalogExceptionHandler {

    private static final String BAD_REQUEST = "Bad Request";
    private static final String INVALID_CATALOG_REQUEST = "INVALID_CATALOG_REQUEST";

    @ExceptionHandler(LibraryCatalogException.class)
    public ResponseEntity<LibraryErrorResponse> handleCatalogException(LibraryCatalogException exception) {
        HttpStatus status = exception.getStatus();
        Map<String, String> fieldErrors = LibraryCatalogErrorMapper.fieldErrors(
                exception.getCode(), exception.getMessage());
        return ResponseEntity.status(status).body(new LibraryErrorResponse(
                status.value(), status.getReasonPhrase(), exception.getCode(), exception.getMessage(), fieldErrors));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<LibraryErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new LibraryErrorResponse(400, BAD_REQUEST,
                INVALID_CATALOG_REQUEST, "Dữ liệu không hợp lệ", fields));
    }

    @ExceptionHandler({ ConstraintViolationException.class, MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class })
    public ResponseEntity<LibraryErrorResponse> handleRequestShape(Exception exception) {
        String code = LibraryCatalogErrorMapper.isIdempotencyKeyError(exception)
                ? "INVALID_IDEMPOTENCY_KEY" : INVALID_CATALOG_REQUEST;
        return ResponseEntity.badRequest().body(new LibraryErrorResponse(400, BAD_REQUEST, code,
                "Yêu cầu không hợp lệ", Map.of()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<LibraryErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(new LibraryErrorResponse(400, BAD_REQUEST,
                INVALID_CATALOG_REQUEST, "Missing required request parameter", Map.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<LibraryErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new LibraryErrorResponse(400, BAD_REQUEST,
                INVALID_CATALOG_REQUEST, "Request body is invalid", Map.of()));
    }

    @ExceptionHandler({ DataIntegrityViolationException.class })
    public ResponseEntity<LibraryErrorResponse> handleConstraint(DataIntegrityViolationException ex) {
        String code = LibraryCatalogErrorMapper.constraintCode(ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new LibraryErrorResponse(409, "Conflict",
                code, "Catalog data conflicts with an existing record",
                LibraryCatalogErrorMapper.fieldErrors(code, "Duplicate value")));
    }

    @ExceptionHandler({ OptimisticLockingFailureException.class, OptimisticLockException.class })
    public ResponseEntity<LibraryErrorResponse> handleVersionConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new LibraryErrorResponse(409, "Conflict",
                "VERSION_CONFLICT", "Catalog record changed; reload before retrying", Map.of()));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<LibraryErrorResponse> handleApplicationError(AppException ex) {
        HttpStatus status = ex.getStatus();
        return ResponseEntity.status(status).body(new LibraryErrorResponse(status.value(), status.getReasonPhrase(),
                "LIBRARY_REQUEST_FAILED", ex.getMessage(), Map.of()));
    }

}

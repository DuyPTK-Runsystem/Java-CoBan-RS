package com.JavaTraining.BaiTap_RS.library.catalog.exception;

import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MissingRequestHeaderException;

final class LibraryCatalogErrorMapper {

    private LibraryCatalogErrorMapper() {
    }

    /* default */
    static boolean isIdempotencyKeyError(Exception exception) {
        return exception instanceof MissingRequestHeaderException
                || exception instanceof ConstraintViolationException validationException
                        && validationException.getConstraintViolations().stream()
                                .anyMatch(violation -> violation.getPropertyPath().toString()
                                        .contains("idempotencyKey"));
    }

    /* default */
    static Map<String, String> fieldErrors(String code, String message) {
        if ("INVALID_ISBN".equals(code) || "DUPLICATE_ISBN".equals(code)) {
            return Map.of("isbn", message);
        }
        if ("INVALID_COVER_URL".equals(code)) {
            return Map.of("coverUrl", message);
        }
        return Map.of();
    }

    /* default */
    static String constraintCode(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                if ("uk_book_isbn".equals(name)) {
                    return "DUPLICATE_ISBN";
                }
                if ("uk_book_copy_barcode".equals(name)) {
                    return "DUPLICATE_BARCODE";
                }
            }
            cause = cause.getCause();
        }
        return "CATALOG_CONSTRAINT_CONFLICT";
    }
}

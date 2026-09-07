package com.JavaTraining.BaiTap_RS.common.error;

import com.JavaTraining.BaiTap_RS.common.dto.RestResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<RestResponse<Void>> handleAppException(AppException exception) {
        HttpStatus status = exception.getStatus();
        RestResponse<Void> response = RestResponse.failure(
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    // BR-ENROLL-001: database uniqueness races are exposed as a conflict, not a server error.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RestResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {
        RestResponse<Void> response = RestResponse.failure(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Dữ liệu vi phạm ràng buộc nghiệp vụ");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

}

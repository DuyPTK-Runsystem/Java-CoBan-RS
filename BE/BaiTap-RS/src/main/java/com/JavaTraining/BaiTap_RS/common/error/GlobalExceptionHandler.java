package com.JavaTraining.BaiTap_RS.common.error;

import com.JavaTraining.BaiTap_RS.common.dto.RestResponse;
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

}

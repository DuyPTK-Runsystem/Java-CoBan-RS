package com.JavaTraining.BaiTap_RS.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsSpringOptimisticLockingFailureToConflict() {
        var response = handler.handleOptimisticLockingFailure(
                new OptimisticLockingFailureException("stale"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatusCode());
    }

    @Test
    void mapsJpaOptimisticLockToConflict() {
        var response = handler.handleJpaOptimisticLock(new OptimisticLockException("stale"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatusCode());
    }

    @Test
    void localizesApplicationStatusTitleWithoutChangingHttpStatus() {
        var response = handler.handleAppException(
                new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem dữ liệu này"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Không có quyền", response.getBody().getError());
        assertEquals("Bạn không có quyền xem dữ liệu này", response.getBody().getMessage());
    }
}

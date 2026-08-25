package com.JavaTraining.BaiTap_RS.scorebook.controller;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResCalculationTaskDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskType;
import com.JavaTraining.BaiTap_RS.scorebook.service.CalculationTaskService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals",
        "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.UnitTestContainsTooManyAsserts"
})
class CalculationTaskControllerTest {

    @Mock
    private CalculationTaskService taskService;

    private CalculationTaskController controller;

    @BeforeEach
    void setUp() {
        controller = new CalculationTaskController(taskService);
    }

    @Test
    void retryDelegatesToService() {
        ResCalculationTaskDTO response = response(CalculationTaskStatus.PENDING);
        Mockito.when(taskService.retryTask(101L)).thenReturn(response);

        ResCalculationTaskDTO result = controller.retry(101L);

        Assertions.assertSame(response, result);
        Mockito.verify(taskService).retryTask(101L);
    }

    @Test
    void recalculateByStudentCodeReturnsAcceptedResponse() {
        ResCalculationTaskDTO response = response(CalculationTaskStatus.PENDING);
        Mockito.when(taskService.requestRecalculation("HS200", 10L)).thenReturn(response);

        ResponseEntity<ResCalculationTaskDTO> result = controller.recalculate("HS200", 10L);

        Assertions.assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
        Assertions.assertSame(response, result.getBody());
        Mockito.verify(taskService).requestRecalculation("HS200", 10L);
    }

    private static ResCalculationTaskDTO response(CalculationTaskStatus status) {
        return new ResCalculationTaskDTO(
                101L,
                200L,
                "HS200",
                10L,
                CalculationTaskType.STUDENT_YEAR_RECALC,
                1L,
                status,
                0,
                3,
                LocalDateTime.now(),
                null,
                null,
                null,
                LocalDateTime.now(),
                null,
                null);
    }
}

package com.JavaTraining.BaiTap_RS.academic.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResAcademicYearStatisticsDTO;
import com.JavaTraining.BaiTap_RS.academic.service.AcademicStatisticsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals",
        "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.UnitTestContainsTooManyAsserts"
})
class AcademicStatisticsControllerTest {

    @Mock
    private AcademicStatisticsService statisticsService;

    private AcademicStatisticsController controller;

    @BeforeEach
    void setUp() {
        controller = new AcademicStatisticsController(statisticsService);
    }

    @Test
    void getAcademicYearStatisticsDelegatesToService() {
        ResAcademicYearStatisticsDTO expected = new ResAcademicYearStatisticsDTO(
                1L, List.of(), List.of(), 0);
        Mockito.when(statisticsService.getAcademicYearStatistics(1L)).thenReturn(expected);

        ResAcademicYearStatisticsDTO actual = controller.getAcademicYearStatistics(1L);

        Assertions.assertSame(expected, actual);
        Mockito.verify(statisticsService).getAcademicYearStatistics(1L);
    }
}

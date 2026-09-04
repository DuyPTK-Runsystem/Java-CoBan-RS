package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResTranscriptCalculationStatusDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.TranscriptQueryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.UnitTestContainsTooManyAsserts"
})
class TranscriptQueryControllerTest {

    @Mock
    private TranscriptQueryService transcriptQueryService;

    private TranscriptQueryController controller;

    @BeforeEach
    void setUp() {
        controller = new TranscriptQueryController(transcriptQueryService);
    }

    @Test
    void delegatesMyTermQueryWithoutClientStudentId() {
        ResStudentTermTranscriptDTO response = Mockito.mock(ResStudentTermTranscriptDTO.class);
        Mockito.when(transcriptQueryService.getMyTermTranscript(10L)).thenReturn(response);

        Assertions.assertSame(response, controller.getMyTerm(10L));
        Mockito.verify(transcriptQueryService).getMyTermTranscript(10L);
    }

    @Test
    void delegatesMyAnnualQueryWithoutClientStudentId() {
        ResStudentAnnualTranscriptDTO response = Mockito.mock(ResStudentAnnualTranscriptDTO.class);
        Mockito.when(transcriptQueryService.getMyAnnualTranscript(20L)).thenReturn(response);

        Assertions.assertSame(response, controller.getMyAnnual(20L));
        Mockito.verify(transcriptQueryService).getMyAnnualTranscript(20L);
    }

    @Test
    void delegatesTermQueryWithStudentAndSemesterIds() {
        ResStudentTermTranscriptDTO response = Mockito.mock(ResStudentTermTranscriptDTO.class);
        Mockito.when(transcriptQueryService.getTermTranscript(100L, 10L)).thenReturn(response);

        Assertions.assertSame(response, controller.getTerm(100L, 10L));
        Mockito.verify(transcriptQueryService).getTermTranscript(100L, 10L);
    }

    @Test
    void delegatesAnnualQueryWithStudentAndAcademicYearIds() {
        ResStudentAnnualTranscriptDTO response = Mockito.mock(ResStudentAnnualTranscriptDTO.class);
        Mockito.when(transcriptQueryService.getAnnualTranscript(100L, 20L)).thenReturn(response);

        Assertions.assertSame(response, controller.getAnnual(100L, 20L));
        Mockito.verify(transcriptQueryService).getAnnualTranscript(100L, 20L);
    }

    @Test
    void delegatesMyTermStatusQuery() {
        ResTranscriptCalculationStatusDTO response = Mockito.mock(ResTranscriptCalculationStatusDTO.class);
        Mockito.when(transcriptQueryService.getMyTermCalculationStatus(10L)).thenReturn(response);

        Assertions.assertSame(response, controller.getMyTermStatus(10L));
        Mockito.verify(transcriptQueryService).getMyTermCalculationStatus(10L);
    }

    @Test
    void delegatesAnnualStatusQueryWithStudentAndAcademicYearIds() {
        ResTranscriptCalculationStatusDTO response = Mockito.mock(ResTranscriptCalculationStatusDTO.class);
        Mockito.when(transcriptQueryService.getAnnualCalculationStatus(100L, 20L)).thenReturn(response);

        Assertions.assertSame(response, controller.getAnnualStatus(100L, 20L));
        Mockito.verify(transcriptQueryService).getAnnualCalculationStatus(100L, 20L);
    }
}

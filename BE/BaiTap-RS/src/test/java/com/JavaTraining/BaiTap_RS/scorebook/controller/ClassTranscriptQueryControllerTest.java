package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.ClassTranscriptQueryService;
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
class ClassTranscriptQueryControllerTest {

    @Mock
    private ClassTranscriptQueryService classTranscriptQueryService;

    private ClassTranscriptQueryController controller;

    @BeforeEach
    void setUp() {
        controller = new ClassTranscriptQueryController(classTranscriptQueryService);
    }

    @Test
    void delegatesClassTermQuery() {
        ResClassTermTranscriptDTO response = Mockito.mock(ResClassTermTranscriptDTO.class);
        Mockito.when(classTranscriptQueryService.getClassTermTranscript(1L, 10L)).thenReturn(response);

        Assertions.assertSame(response, controller.getClassTermTranscript(1L, 10L));
        Mockito.verify(classTranscriptQueryService).getClassTermTranscript(1L, 10L);
    }

    @Test
    void delegatesClassAnnualQuery() {
        ResClassAnnualTranscriptDTO response = Mockito.mock(ResClassAnnualTranscriptDTO.class);
        Mockito.when(classTranscriptQueryService.getClassAnnualTranscript(1L, 20L)).thenReturn(response);

        Assertions.assertSame(response, controller.getClassAnnualTranscript(1L, 20L));
        Mockito.verify(classTranscriptQueryService).getClassAnnualTranscript(1L, 20L);
    }
}

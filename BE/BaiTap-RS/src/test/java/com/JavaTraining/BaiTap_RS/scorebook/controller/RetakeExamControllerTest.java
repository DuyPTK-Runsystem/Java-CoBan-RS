package com.JavaTraining.BaiTap_RS.scorebook.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpdateRetakeScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExamStatus;
import com.JavaTraining.BaiTap_RS.scorebook.service.RetakeExamService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class RetakeExamControllerTest {

    @Mock
    private RetakeExamService retakeExamService;

    private RetakeExamController controller;

    @BeforeEach
    void setUp() {
        controller = new RetakeExamController(retakeExamService);
    }

    @Test
    void createDelegatesToService() {
        ReqCreateRetakeExamDTO req = new ReqCreateRetakeExamDTO(
                100L, 10L, 20L, LocalDate.of(2026, 8, 30), null, "Kế hoạch");
        ResRetakeExamDTO res = sampleResponse(500L, RetakeExamStatus.PLANNED);
        Mockito.when(retakeExamService.createRetakeExam(req)).thenReturn(res);

        ResponseEntity<ResRetakeExamDTO> result = controller.create(req);

        Assertions.assertEquals(HttpStatus.CREATED, result.getStatusCode(), "Status should be CREATED");
        Assertions.assertSame(res, result.getBody(), "Response body should match");
        Mockito.verify(retakeExamService).createRetakeExam(req);
    }

    @Test
    void updateScoreDelegatesToService() {
        ReqUpdateRetakeScoreDTO req = new ReqUpdateRetakeScoreDTO(
                new BigDecimal("8.0"), LocalDate.of(2026, 9, 1), "Điểm thi");
        ResRetakeExamDTO res = sampleResponse(500L, RetakeExamStatus.SCORED);
        Mockito.when(retakeExamService.updateRetakeScore(500L, req)).thenReturn(res);

        ResRetakeExamDTO result = controller.updateScore(500L, req);

        Assertions.assertSame(res, result, "Response should match service result");
        Mockito.verify(retakeExamService).updateRetakeScore(500L, req);
    }

    @Test
    void cancelDelegatesToService() {
        ResRetakeExamDTO res = sampleResponse(500L, RetakeExamStatus.CANCELLED);
        Mockito.when(retakeExamService.cancelRetakeExam(500L)).thenReturn(res);

        ResRetakeExamDTO result = controller.cancel(500L);

        Assertions.assertSame(res, result, "Response should match service result");
        Mockito.verify(retakeExamService).cancelRetakeExam(500L);
    }

    @Test
    void findSingleRetakeExamDelegatesToService() {
        ResRetakeExamDTO res = sampleResponse(500L, RetakeExamStatus.PLANNED);
        Mockito.when(retakeExamService.getRetakeExam(500L)).thenReturn(res);

        ResRetakeExamDTO result = controller.get(500L);

        Assertions.assertSame(res, result, "Response should match service result");
        Mockito.verify(retakeExamService).getRetakeExam(500L);
    }

    @Test
    void findDelegatesToService() {
        ReqFilterRetakeExamDTO filter = new ReqFilterRetakeExamDTO();
        Page<ResRetakeExamDTO> page = new PageImpl<>(java.util.List.of(sampleResponse(500L, RetakeExamStatus.PLANNED)));
        Mockito.when(retakeExamService.findRetakeExams(filter)).thenReturn(page);

        Page<ResRetakeExamDTO> result = controller.find(filter);

        Assertions.assertSame(page, result, "Page result should match service page");
        Mockito.verify(retakeExamService).findRetakeExams(filter);
    }

    private static ResRetakeExamDTO sampleResponse(Long retakeId, RetakeExamStatus status) {
        return new ResRetakeExamDTO(
                retakeId,
                100L,
                10L,
                20L,
                new BigDecimal("4.5"),
                status == RetakeExamStatus.SCORED ? new BigDecimal("8.0") : null,
                LocalDate.of(2026, 8, 30),
                status,
                "Note",
                1L,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}

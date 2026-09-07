package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.scorebook.controller.ScorebookController;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScorebookLookupDelegationTest {

    @Mock
    private ScorebookLifecycleService lifecycleService;

    @Mock
    private ScorebookColumnService columnService;

    @Mock
    private ScorebookSkillWeightService skillWeightService;

    @Test
    void serviceLookupDelegatesToLifecycleService() {
        ResScorebookDTO expected = response();
        Mockito.when(lifecycleService.getScorebookByClassSubject(20L)).thenReturn(expected);
        ScorebookService service = new ScorebookService(lifecycleService, columnService, skillWeightService);

        ResScorebookDTO actual = service.getScorebookByClassSubject(20L);

        Assertions.assertSame(expected, actual, "service should return the lifecycle lookup response");
    }

    @Test
    void controllerLookupDelegatesToScorebookService() {
        ResScorebookDTO expected = response();
        ScorebookService service = Mockito.mock(ScorebookService.class);
        Mockito.when(service.getScorebookByClassSubject(20L)).thenReturn(expected);
        ScorebookController controller = new ScorebookController(service);

        ResScorebookDTO actual = controller.getScorebookByClassSubject(20L);

        Assertions.assertSame(expected, actual, "controller should return the service lookup response");
    }

    private ResScorebookDTO response() {
        return new ResScorebookDTO(
                90L,
                20L,
                ScorebookStatus.OPEN,
                null,
                null,
                null,
                List.of(),
                null);
    }
}


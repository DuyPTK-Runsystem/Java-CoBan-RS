package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadPolicyDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicyStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadPolicyRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadRuleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mockito;

class TeacherLoadPolicyServiceTest {

    @Test
    void canonicalActivePolicyIncludesTheRepositorySelectedVersion() {
        TeacherLoadPolicyRepository policies = Mockito.mock(TeacherLoadPolicyRepository.class);
        TeacherLoadRuleRepository rules = Mockito.mock(TeacherLoadRuleRepository.class);
        TeacherLoadPolicyService service = new TeacherLoadPolicyService(policies, rules);
        TeacherLoadPolicy active = new TeacherLoadPolicy(
                "2026-27", "Decision 01", LocalDate.of(2026, 9, 1), null, 19, 4, 3);
        ReflectionTestUtils.setField(active, "id", 17L);
        active.setStatus(TeacherLoadPolicyStatus.ACTIVE);
        Mockito.when(policies.findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        Mockito.when(rules.findAllByPolicyIdOrderByIdAsc(17L)).thenReturn(java.util.List.of());

        ResTeacherLoadPolicyDTO result = service.getCurrentActivePolicy();

        Assertions.assertTrue(result.id().equals(17L)
                        && "2026-27".equals(result.version())
                        && result.status() == TeacherLoadPolicyStatus.ACTIVE,
                "The response should expose the currently selected ACTIVE policy and its version");
    }
}

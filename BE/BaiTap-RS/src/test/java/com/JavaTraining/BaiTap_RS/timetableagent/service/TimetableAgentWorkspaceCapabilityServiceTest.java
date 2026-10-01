package com.JavaTraining.BaiTap_RS.timetableagent.service;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGatewayResolver;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TimetableAgentWorkspaceCapabilityServiceTest {

    private final TimetableAgentProperties properties = new TimetableAgentProperties();
    private final TimetableAgentModelGatewayResolver gatewayResolver =
            Mockito.mock(TimetableAgentModelGatewayResolver.class);
    private final TimetableAgentModelGateway gateway = Mockito.mock(TimetableAgentModelGateway.class);
    private TimetableAgentWorkspaceCapabilityService capabilityService;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        properties.setMaxClassCount(8);
        properties.setMaxContextCharacters(20_000);
        properties.setMaxRequestCharacters(2_000);
        properties.setMaxPreferencesCharacters(1_000);
        properties.setMaxProposalEntries(120);
        capabilityService = new TimetableAgentWorkspaceCapabilityService(properties, gatewayResolver);
        Mockito.when(gatewayResolver.getIfUnambiguous()).thenReturn(gateway);
        Mockito.when(gateway.supportsNativeStructuredOutput()).thenReturn(true);
        Mockito.when(gateway.supportsSaveToolCalling()).thenReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void capabilityAllowsConfiguredDraftForAdminAndAcademicOffice() {
        authenticate("ROLE_ADMIN");
        Assertions.assertTrue(capabilityService.canUse(TimetableRevisionStatus.DRAFT),
                "configured admins may use the draft capability");

        authenticate("ROLE_ACADEMIC_OFFICE");
        Assertions.assertTrue(capabilityService.canUse(TimetableRevisionStatus.DRAFT),
                "configured academic-office users may use the draft capability");
    }

    @Test
    void capabilityRequiresFeatureEnabledAndEveryPositiveBound() {
        authenticate("ROLE_ADMIN");
        properties.setEnabled(false);
        Assertions.assertFalse(capabilityService.canUse(TimetableRevisionStatus.DRAFT),
                "the feature must stay unavailable while disabled");

        properties.setEnabled(true);
        properties.setMaxContextCharacters(0);
        Assertions.assertFalse(capabilityService.canUse(TimetableRevisionStatus.DRAFT),
                "missing context bound must keep the feature unavailable");
    }

    @Test
    void capabilityRequiresDraftRoleAndSingleGatewaySupportingBothModes() {
        authenticate("ROLE_ADMIN");
        Assertions.assertFalse(capabilityService.canUse(TimetableRevisionStatus.PUBLISHED),
                "published revisions are outside the capability scope");

        authenticate("ROLE_TEACHER");
        Assertions.assertFalse(capabilityService.canUse(TimetableRevisionStatus.DRAFT),
                "teacher role does not grant the agent capability");

        authenticate("ROLE_ADMIN");
        Mockito.when(gatewayResolver.getIfUnambiguous()).thenReturn(null);
        Assertions.assertFalse(capabilityService.canUse(TimetableRevisionStatus.DRAFT),
                "missing or ambiguous provider must keep the capability unavailable");

        Mockito.when(gatewayResolver.getIfUnambiguous()).thenReturn(gateway);
        Mockito.when(gateway.supportsNativeStructuredOutput()).thenReturn(false);
        Assertions.assertFalse(capabilityService.canUse(TimetableRevisionStatus.DRAFT),
                "provider must support native proposal structure");

        Mockito.when(gateway.supportsNativeStructuredOutput()).thenReturn(true);
        Mockito.when(gateway.supportsSaveToolCalling()).thenReturn(false);
        Assertions.assertFalse(capabilityService.canUse(TimetableRevisionStatus.DRAFT),
                "provider must support guarded save tool calling");
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("actor", "unused", role));
    }
}

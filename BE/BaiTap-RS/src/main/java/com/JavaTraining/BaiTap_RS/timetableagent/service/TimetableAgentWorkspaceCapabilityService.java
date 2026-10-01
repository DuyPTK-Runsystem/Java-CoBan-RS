package com.JavaTraining.BaiTap_RS.timetableagent.service;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGatewayResolver;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TimetableAgentWorkspaceCapabilityService {

    private static final String ADMIN = "ROLE_ADMIN";
    private static final String ACADEMIC_OFFICE = "ROLE_ACADEMIC_OFFICE";

    private final TimetableAgentProperties properties;
    private final TimetableAgentModelGatewayResolver gatewayResolver;

    public TimetableAgentWorkspaceCapabilityService(TimetableAgentProperties properties,
            TimetableAgentModelGatewayResolver gatewayResolver) {
        this.properties = properties;
        this.gatewayResolver = gatewayResolver;
    }

    public boolean canUse(TimetableRevisionStatus status) {
        if (!properties.isEnabled() || !properties.hasRequiredBounds()
                || status != TimetableRevisionStatus.DRAFT || !authorized()) {
            return false;
        }
        TimetableAgentModelGateway gateway = gatewayResolver.getIfUnambiguous();
        return gateway != null && gateway.supportsNativeStructuredOutput() && gateway.supportsSaveToolCalling();
    }

    private boolean authorized() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> ADMIN.equals(authority) || ACADEMIC_OFFICE.equals(authority));
    }
}

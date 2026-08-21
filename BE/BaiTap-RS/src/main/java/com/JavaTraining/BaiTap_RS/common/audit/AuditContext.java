package com.JavaTraining.BaiTap_RS.common.audit;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class AuditContext {

    private static final String REQUEST_ID_KEY = "requestId";

    private AuditContext() {
    }

    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    public static String requestId() {
        return MDC.get(REQUEST_ID_KEY);
    }

    public static String ipAddress() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(attributes -> attributes.getRequest().getRemoteAddr())
                .orElse(null);
    }
}

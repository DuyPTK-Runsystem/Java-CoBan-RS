package com.JavaTraining.BaiTap_RS.common.util;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuditUtil {

    private static final String SYSTEM_USER = "system";

    private AuditUtil() {
    }

    public static String currentUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .filter(username -> !"anonymousUser".equals(username))
                .orElse(SYSTEM_USER);
    }
}

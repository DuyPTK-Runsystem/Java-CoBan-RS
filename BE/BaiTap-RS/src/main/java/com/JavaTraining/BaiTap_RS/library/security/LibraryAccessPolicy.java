package com.JavaTraining.BaiTap_RS.library.security;

import java.util.Collection;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.entity.RoleCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("libraryAccess")
public class LibraryAccessPolicy {

    public static final String MANAGER_CHECK = "@libraryAccess.isManager()";

    public boolean isManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return hasRole(authentication.getAuthorities(), RoleCode.ADMIN)
                || hasRole(authentication.getAuthorities(), RoleCode.LIBRARIAN);
    }

    public boolean isManagerOrOwner(Long userId) {
        return isManager() || (userId != null && userId.equals(AuditContext.currentUserId()));
    }

    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
                ? principal.getId() : null;
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, RoleCode role) {
        return authorities.stream().anyMatch(authority -> role.authority().equals(authority.getAuthority()));
    }
}

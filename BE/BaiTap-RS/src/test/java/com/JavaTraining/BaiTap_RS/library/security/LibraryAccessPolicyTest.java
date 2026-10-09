package com.JavaTraining.BaiTap_RS.library.security;

import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.RoleCode;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class LibraryAccessPolicyTest {

    private final LibraryAccessPolicy policy = new LibraryAccessPolicy();

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void limitsManagementToAdminOrLibrarianAndResolvesCurrentOwnerFromPrincipal() {
        authenticate(15L, RoleCode.TEACHER);

        assertFalse(policy.isManager());
        assertEquals(15L, policy.currentUserId());
        assertTrue(policy.isManagerOrOwner(15L));
        assertFalse(policy.isManagerOrOwner(16L));

        authenticate(16L, RoleCode.LIBRARIAN);

        assertTrue(policy.isManager());
        assertTrue(policy.isManagerOrOwner(15L));

        authenticate(17L, RoleCode.ADMIN);

        assertTrue(policy.isManager());
    }

    @Test
    void deniesManagementAndOwnershipWhenNoAuthenticatedPrincipalExists() {
        SecurityContextHolder.clearContext();

        assertFalse(policy.isManager());
        assertNull(policy.currentUserId());
        assertFalse(policy.isManagerOrOwner(15L));
    }

    private void authenticate(Long userId, RoleCode roleCode) {
        User user = new User("user" + userId, "not-a-real-password");
        ReflectionTestUtils.setField(user, "id", userId);
        user.addRole(new Role(roleCode.code(), roleCode.name(), null));
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
    }
}

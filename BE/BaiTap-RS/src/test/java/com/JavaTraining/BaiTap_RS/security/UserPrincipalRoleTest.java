package com.JavaTraining.BaiTap_RS.security;

import java.util.Set;

import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class UserPrincipalRoleTest {

    @Test
    void mapsRoleCodesToSpringRoleAuthorities() {
        User user = new User("admin01", "bcrypt-hash");
        user.addRole(new Role("ADMIN", "Administrator", "Admin"));
        user.addRole(new Role("TEACHER", "Teacher", "Teacher"));

        UserPrincipal principal = new UserPrincipal(user);
        Set<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        Assertions.assertEquals(
                Set.of("ROLE_ADMIN", "ROLE_TEACHER"),
                authorities,
                "role codes should become Spring Security role authorities");
    }
}

package com.JavaTraining.BaiTap_RS.security;

import java.util.Collection;
import java.util.List;

import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String password;
    private final Collection<Role> roles;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.roles = user.getRoles();
    }

    public Long getId() {
        return id;
    }

    public List<String> getRoleCodes() {
        return roles.stream()
                .map(Role::getCode)
                .sorted()
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoleCodes().stream()
                .map(code -> "ROLE_" + code)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}

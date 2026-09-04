package com.JavaTraining.BaiTap_RS.user.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);
}

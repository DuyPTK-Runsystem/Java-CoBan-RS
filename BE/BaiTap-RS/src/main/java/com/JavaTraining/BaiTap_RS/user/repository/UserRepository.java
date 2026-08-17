package com.JavaTraining.BaiTap_RS.user.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}

package com.JavaTraining.BaiTap_RS.placement.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlacementSessionRepository extends JpaRepository<PlacementSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PlacementSession s where s.id = :id")
    Optional<PlacementSession> findByIdForUpdate(@Param("id") Long id);
    Optional<PlacementSession> findByConfirmIdempotencyKey(String key);
}

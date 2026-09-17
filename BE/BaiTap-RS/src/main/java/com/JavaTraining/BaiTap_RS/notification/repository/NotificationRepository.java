package com.JavaTraining.BaiTap_RS.notification.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.idempotency.key = :idempotencyKey")
    Optional<Notification> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    Page<Notification> findBySchoolScope(String schoolScope, Pageable pageable);

    Page<Notification> findBySenderId(Long senderId, Pageable pageable);
}

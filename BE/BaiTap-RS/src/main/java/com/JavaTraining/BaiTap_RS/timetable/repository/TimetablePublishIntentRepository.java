package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePublishIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetablePublishIntentRepository extends JpaRepository<TimetablePublishIntent, Long> {

    Optional<TimetablePublishIntent> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}

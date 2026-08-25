package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalculationTaskRepository extends JpaRepository<CalculationTask, Long> {

    Optional<CalculationTask> findByIdempotencyKey(String idempotencyKey);
}

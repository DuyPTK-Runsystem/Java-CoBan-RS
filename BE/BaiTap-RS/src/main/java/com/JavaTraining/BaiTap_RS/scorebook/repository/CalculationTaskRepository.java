package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTask;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CalculationTaskRepository
        extends JpaRepository<CalculationTask, Long>, JpaSpecificationExecutor<CalculationTask> {

    Optional<CalculationTask> findByIdempotencyKey(String idempotencyKey);

    List<CalculationTask> findAllByStatusOrderByCreatedAtAsc(CalculationTaskStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT task
            FROM CalculationTask task
            WHERE task.status = :status
              AND task.availableAt <= :availableAt
            ORDER BY task.availableAt ASC, task.createdAt ASC
            """)
    List<CalculationTask> findAvailableForUpdate(
            @Param("status") CalculationTaskStatus status,
            @Param("availableAt") LocalDateTime availableAt,
            Pageable pageable);
}

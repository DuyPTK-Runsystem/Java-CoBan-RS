package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequest;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoreChangeRequestRepository
        extends JpaRepository<ScoreChangeRequest, Long>, JpaSpecificationExecutor<ScoreChangeRequest> {

    boolean existsByAssessmentColumnIdAndStudentIdAndStatus(
            Long assessmentColumnId, Long studentId, ScoreChangeRequestStatus status);

    Page<ScoreChangeRequest> findByRequestedBy(Long requestedBy, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT request
            FROM ScoreChangeRequest request
            WHERE request.id = :requestId
            """)
    Optional<ScoreChangeRequest> findForUpdate(@Param("requestId") Long requestId);
}

package com.JavaTraining.BaiTap_RS.placement.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlacementResultRepository extends JpaRepository<PlacementResult, Long> {
    List<PlacementResult> findAllBySessionIdOrderByStudentIdAsc(Long sessionId);
    Page<PlacementResult> findBySessionIdOrderByStudentIdAsc(Long sessionId, Pageable pageable);
    void deleteAllBySessionId(Long sessionId);
}

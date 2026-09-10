package com.JavaTraining.BaiTap_RS.placement.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementCandidateRepository extends JpaRepository<PlacementCandidate, Long> {
    List<PlacementCandidate> findAllBySessionIdOrderByStudentIdAsc(Long sessionId);
    void deleteAllBySessionId(Long sessionId);
}

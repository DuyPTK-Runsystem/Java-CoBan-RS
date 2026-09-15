package com.JavaTraining.BaiTap_RS.assignment.repository;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeroomAssignmentRepository extends JpaRepository<HomeroomAssignment, Long>,
                HomeroomAssignmentLookupRepository, HomeroomAssignmentConflictRepository {
}

package com.JavaTraining.BaiTap_RS.enrollment.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.ClassTransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassTransferHistoryRepository extends JpaRepository<ClassTransferHistory, Long> {

    List<ClassTransferHistory> findByEnrollmentIdOrderByEffectiveAtAsc(Long enrollmentId);

    boolean existsByFromClassId(Long classId);

    boolean existsByToClassId(Long classId);
}

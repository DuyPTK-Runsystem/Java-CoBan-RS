package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterLockReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterLockReportRepository extends JpaRepository<SemesterLockReport, Long> {

        Optional<SemesterLockReport> findByRunIdAndSemesterIdAndCheckpointCode(
                        Long runId,
                        Long semesterId,
                        String checkpointCode);

        Optional<SemesterLockReport> findFirstBySemesterIdAndCheckpointCodeOrderByEvaluatedAtDesc(
                        Long semesterId,
                        String checkpointCode);

        Optional<SemesterLockReport> findFirstBySemesterIdOrderByEvaluatedAtDesc(Long semesterId);

        List<SemesterLockReport> findAllBySemesterIdOrderByEvaluatedAtDesc(Long semesterId);
}

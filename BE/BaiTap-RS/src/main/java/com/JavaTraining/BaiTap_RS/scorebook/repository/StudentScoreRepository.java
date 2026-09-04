package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentScoreRepository extends JpaRepository<StudentScore, Long> {

    Optional<StudentScore> findByAssessmentColumnIdAndStudentId(Long assessmentColumnId, Long studentId);

    List<StudentScore> findAllByAssessmentColumnIdInAndStudentIdIn(
            Collection<Long> columnIds, Collection<Long> studentIds);

    List<StudentScore> findAllByAssessmentColumnIdIn(Collection<Long> columnIds);
}

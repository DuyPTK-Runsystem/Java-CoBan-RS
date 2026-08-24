package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentColumnRepository extends JpaRepository<AssessmentColumn, Long> {

    List<AssessmentColumn> findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(Long scorebookId);

    long countByScorebookIdAndAssessmentTypeAndStatus(
            Long scorebookId,
            AssessmentType assessmentType,
            AssessmentColumnStatus status);

    boolean existsByScorebookIdAndAssessmentTypeAndColumnNo(
            Long scorebookId,
            AssessmentType assessmentType,
            Integer columnNo);
}

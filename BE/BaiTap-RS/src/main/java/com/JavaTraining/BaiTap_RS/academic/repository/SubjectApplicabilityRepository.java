package com.JavaTraining.BaiTap_RS.academic.repository;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectApplicabilityRepository extends JpaRepository<SubjectApplicability, Long> {

    boolean existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelIdAndStatus(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long gradeLevelId,
            SubjectApplicabilityStatus status);

    boolean existsBySubjectIdAndSemesterIdAndScopeTypeAndClassIdAndStatus(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long classId,
            SubjectApplicabilityStatus status);

    long countBySubjectIdAndStatus(Long subjectId, SubjectApplicabilityStatus status);
}

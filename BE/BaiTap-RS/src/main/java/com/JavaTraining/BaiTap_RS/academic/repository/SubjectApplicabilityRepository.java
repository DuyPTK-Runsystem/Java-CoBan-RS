package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectApplicabilityRepository extends JpaRepository<SubjectApplicability, Long> {

    @Query("""
            select applicability
            from SubjectApplicability applicability
            where applicability.subjectId = :subjectId
              and (:semesterId is null or applicability.semesterId = :semesterId)
              and (:status is null or applicability.status = :status)
            order by applicability.semesterId asc,
                     applicability.scopeType asc,
                     coalesce(applicability.gradeLevelId, applicability.classId) asc,
                     applicability.id asc
            """)
    List<SubjectApplicability> findAllByFilters(
            @Param("subjectId") Long subjectId,
            @Param("semesterId") Long semesterId,
            @Param("status") SubjectApplicabilityStatus status);

    boolean existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelIdAndStatus(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long gradeLevelId,
            SubjectApplicabilityStatus status);

    boolean existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelId(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long gradeLevelId);

    boolean existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelIdAndIdNot(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long gradeLevelId,
            Long id);

    boolean existsBySubjectIdAndSemesterIdAndScopeTypeAndClassIdAndStatus(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long classId,
            SubjectApplicabilityStatus status);

    boolean existsBySubjectIdAndSemesterIdAndScopeTypeAndClassId(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long classId);

    boolean existsBySubjectIdAndSemesterIdAndScopeTypeAndClassIdAndIdNot(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long classId,
            Long id);

    long countBySubjectIdAndStatus(Long subjectId, SubjectApplicabilityStatus status);

    long countBySubjectIdAndStatusAndIdNot(Long subjectId, SubjectApplicabilityStatus status, Long id);
}

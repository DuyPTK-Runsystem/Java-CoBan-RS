package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    boolean existsByAcademicYearId(Long academicYearId);

    boolean existsByGradeLevelId(Long gradeLevelId);

    boolean existsByAcademicYearIdAndClassCode(Long academicYearId, String classCode);

    boolean existsByAcademicYearIdAndClassCodeAndIdNot(Long academicYearId, String classCode, Long id);

    List<SchoolClass> findAllByAcademicYearIdOrderByClassCodeAsc(Long academicYearId);

    long countByAcademicYearIdAndGradeLevelIdAndStatus(
            Long academicYearId,
            Long gradeLevelId,
            SchoolClassStatus status);

    boolean existsByAcademicYearIdAndStatusAndIdNot(
            Long academicYearId,
            SchoolClassStatus status,
            Long id);
}

package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import org.springframework.data.repository.Repository;

@org.springframework.stereotype.Repository
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ClassSubjectApplicabilityClassRepository extends Repository<SchoolClass, Long> {

    List<SchoolClass> findAllByAcademicYearIdAndGradeLevelIdOrderByClassCodeAsc(
            Long academicYearId, Long gradeLevelId);
}

package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolClassConstraintRepository {

    boolean existsByAcademicYearId(Long academicYearId);

    boolean existsByGradeLevelId(Long gradeLevelId);

    boolean existsByAcademicYearIdAndClassCode(Long academicYearId, String classCode);

    boolean existsByAcademicYearIdAndClassCodeAndIdNot(Long academicYearId, String classCode, Long id);

    long countByAcademicYearIdAndGradeLevelIdAndStatus(
            Long academicYearId, Long gradeLevelId, SchoolClassStatus status);

    boolean existsByAcademicYearIdAndStatusAndIdNot(
            Long academicYearId, SchoolClassStatus status, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select schoolClass from SchoolClass schoolClass where schoolClass.id = :id")
    Optional<SchoolClass> findByIdForUpdate(@Param("id") Long id);
}

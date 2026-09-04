package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

        boolean existsByAcademicYearId(Long academicYearId);

        boolean existsByGradeLevelId(Long gradeLevelId);

        boolean existsByAcademicYearIdAndClassCode(Long academicYearId, String classCode);

        boolean existsByAcademicYearIdAndClassCodeAndIdNot(Long academicYearId, String classCode, Long id);

        List<SchoolClass> findAllByAcademicYearIdOrderByClassCodeAsc(Long academicYearId);

        List<SchoolClass> findAllByIdInOrderByClassCodeAsc(Collection<Long> ids);

        List<SchoolClass> findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(
                        Collection<Long> ids, Long academicYearId);

        long countByAcademicYearIdAndGradeLevelIdAndStatus(
                        Long academicYearId,
                        Long gradeLevelId,
                        SchoolClassStatus status);

        boolean existsByAcademicYearIdAndStatusAndIdNot(
                        Long academicYearId,
                        SchoolClassStatus status,
                        Long id);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select schoolClass from SchoolClass schoolClass where schoolClass.id = :id")
        Optional<SchoolClass> findByIdForUpdate(@Param("id") Long id);
}

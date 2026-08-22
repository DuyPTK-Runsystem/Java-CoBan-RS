package com.JavaTraining.BaiTap_RS.academic.repository;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    boolean existsByAcademicYearIdAndCode(Long academicYearId, String code);

    boolean existsByAcademicYearIdAndCodeAndIdNot(Long academicYearId, String code, Long id);

    boolean existsByAcademicYearIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long academicYearId,
            LocalDate endDate,
            LocalDate startDate);

    boolean existsByAcademicYearIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
            Long academicYearId,
            LocalDate endDate,
            LocalDate startDate,
            Long id);

    List<Semester> findAllByAcademicYearIdOrderByDisplayOrderAsc(Long academicYearId);
}

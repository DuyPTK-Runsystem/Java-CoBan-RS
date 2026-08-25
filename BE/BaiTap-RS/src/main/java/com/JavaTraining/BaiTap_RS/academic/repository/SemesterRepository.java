package com.JavaTraining.BaiTap_RS.academic.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Semester s WHERE s.id = :id")
    Optional<Semester> findByIdForUpdate(@Param("id") Long id);

    List<Semester> findAllByStatusIn(List<SemesterStatus> statuses);
}

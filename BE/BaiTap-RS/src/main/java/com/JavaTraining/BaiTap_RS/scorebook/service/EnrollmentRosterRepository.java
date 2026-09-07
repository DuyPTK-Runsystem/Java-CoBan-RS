package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository chuyên biệt cho roster query phân trang dùng trong score grid.
 */
@Repository
public interface EnrollmentRosterRepository extends JpaRepository<StudentYearEnrollment, Long> {

    @Query("""
            SELECT e
            FROM StudentYearEnrollment e
            WHERE e.currentClassId = :classId
              AND e.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
            ORDER BY e.studentId ASC
            """)
    Page<StudentYearEnrollment> findActiveByClassId(
            @Param("classId") Long classId,
            Pageable pageable);
}

package com.JavaTraining.BaiTap_RS.enrollment.repository;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentYearEnrollmentRepository extends JpaRepository<StudentYearEnrollment, Long>,
                StudentYearEnrollmentLookupRepository, StudentYearEnrollmentRosterRepository {
}

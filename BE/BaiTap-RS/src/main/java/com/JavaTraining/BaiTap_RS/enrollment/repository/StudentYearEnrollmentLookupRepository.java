package com.JavaTraining.BaiTap_RS.enrollment.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;

public interface StudentYearEnrollmentLookupRepository {

    boolean existsByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    boolean existsByCurrentClassId(Long classId);

    boolean existsByAcademicYearId(Long academicYearId);

    Optional<StudentYearEnrollment> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    List<StudentYearEnrollment> findByCurrentClassIdAndStatusOrderByStudentIdAsc(
            Long classId, EnrollmentStatus status);

    List<StudentYearEnrollment> findByAcademicYearIdAndStatusOrderByStudentIdAsc(
            Long academicYearId, EnrollmentStatus status);

    List<StudentYearEnrollment> findByStudentIdOrderByEnrolledAtAsc(Long studentId);
}

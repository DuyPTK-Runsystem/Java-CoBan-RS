package com.JavaTraining.BaiTap_RS.student.repository;

import java.util.Collection;
import java.util.List;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentClassLookupRepository extends JpaRepository<StudentYearEnrollment, Long> {

  @Query("""
      SELECT e.studentId, c.classCode
      FROM StudentYearEnrollment e, SchoolClass c, AcademicYear y
      WHERE e.currentClassId = c.id
        AND e.academicYearId = y.id
        AND y.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
        AND e.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
        AND e.studentId IN :studentIds
      """)
  List<Object[]> findActiveClassCodesByStudentIds(@Param("studentIds") Collection<Long> studentIds);

  @Query("""
      SELECT c.classCode
      FROM StudentYearEnrollment e, SchoolClass c, AcademicYear y
      WHERE e.currentClassId = c.id
        AND e.academicYearId = y.id
        AND y.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
        AND e.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
        AND e.studentId = :studentId
      """)
  List<String> findActiveClassCodeByStudentId(@Param("studentId") Long studentId);

  @Query("""
      SELECT e.studentId, e.currentClassId, c.classCode
      FROM StudentYearEnrollment e, SchoolClass c, AcademicYear y
      WHERE e.currentClassId = c.id
        AND e.academicYearId = y.id
        AND y.status = com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus.ACTIVE
        AND e.status = com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus.ACTIVE
        AND e.studentId IN :studentIds
      """)
  List<Object[]> findActiveClassesByStudentIds(@Param("studentIds") Collection<Long> studentIds);
}

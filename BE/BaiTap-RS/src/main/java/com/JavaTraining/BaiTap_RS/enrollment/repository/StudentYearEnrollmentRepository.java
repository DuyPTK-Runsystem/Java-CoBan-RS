package com.JavaTraining.BaiTap_RS.enrollment.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentYearEnrollmentRepository extends JpaRepository<StudentYearEnrollment, Long> {

    boolean existsByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    boolean existsByCurrentClassId(Long classId);

    boolean existsByAcademicYearId(Long academicYearId);

    Optional<StudentYearEnrollment> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    List<StudentYearEnrollment> findByCurrentClassIdAndStatusOrderByStudentIdAsc(
            Long classId,
            EnrollmentStatus status);

    List<StudentYearEnrollment> findByStudentIdOrderByEnrolledAtAsc(Long studentId);

    @Query("""
            select count(e.id)
            from StudentYearEnrollment e, SchoolClass c
            where e.currentClassId = c.id
              and e.academicYearId = :academicYearId
              and c.gradeLevelId = :gradeLevelId
              and e.status = :status
            """)
    long countByAcademicYearAndGradeAndStatus(
            @Param("academicYearId") Long academicYearId,
            @Param("gradeLevelId") Long gradeLevelId,
            @Param("status") EnrollmentStatus status);

    long countByCurrentClassIdAndStatus(Long classId, EnrollmentStatus status);

    @Query("""
            select s
            from Student s
            where s.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
              and not exists (
                  select e.id
                  from StudentYearEnrollment e
                  where e.studentId = s.id
                    and e.academicYearId = :academicYearId
              )
            order by s.studentCode
            """)
    List<Student> findUnassignedStudents(@Param("academicYearId") Long academicYearId);
}

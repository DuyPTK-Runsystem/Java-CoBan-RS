package com.JavaTraining.BaiTap_RS.attendance.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceEnrollmentRepository extends JpaRepository<StudentYearEnrollment, Long> {

    @Query("""
            select distinct s
            from StudentYearEnrollment e, Student s
            where e.studentId = s.id
              and e.currentClassId = :classId
              and e.status = :status
              and e.enrolledAt <= :effectiveAtEnd
              and (e.completedAt is null or e.completedAt >= :effectiveAtStart)
              and s.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
            order by s.studentCode asc, s.id asc
            """)
    List<Student> findActiveStudentsInClassAt(
            @Param("classId") Long classId,
            @Param("status") EnrollmentStatus status,
            @Param("effectiveAtStart") LocalDateTime effectiveAtStart,
            @Param("effectiveAtEnd") LocalDateTime effectiveAtEnd);

    @Query("""
            select count(e.id) > 0
            from StudentYearEnrollment e
            where e.studentId = :studentId
              and e.currentClassId = :classId
              and e.status = :status
              and e.enrolledAt <= :effectiveAtEnd
              and (e.completedAt is null or e.completedAt >= :effectiveAtStart)
            """)
    boolean existsActiveStudentInClassAt(
            @Param("studentId") Long studentId,
            @Param("classId") Long classId,
            @Param("status") EnrollmentStatus status,
            @Param("effectiveAtStart") LocalDateTime effectiveAtStart,
            @Param("effectiveAtEnd") LocalDateTime effectiveAtEnd);

    @Query("""
            select e
            from StudentYearEnrollment e, Student s
            where e.studentId = s.id
              and e.currentClassId = :classId
              and e.status = :status
              and e.enrolledAt <= :effectiveAtEnd
              and (e.completedAt is null or e.completedAt >= :effectiveAtStart)
              and s.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
            order by s.studentCode asc, s.id asc
            """)
    List<StudentYearEnrollment> findActiveEnrollmentsInClassAt(
            @Param("classId") Long classId,
            @Param("status") EnrollmentStatus status,
            @Param("effectiveAtStart") LocalDateTime effectiveAtStart,
            @Param("effectiveAtEnd") LocalDateTime effectiveAtEnd);
}

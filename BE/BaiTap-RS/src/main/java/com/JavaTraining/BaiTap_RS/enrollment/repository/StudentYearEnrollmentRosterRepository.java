package com.JavaTraining.BaiTap_RS.enrollment.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentYearEnrollmentRosterRepository {

    @Query("""
            select count(e.id) from StudentYearEnrollment e, SchoolClass c
            where e.currentClassId = c.id and e.academicYearId = :academicYearId
              and c.gradeLevelId = :gradeLevelId and e.status = :status
            """)
    long countByAcademicYearAndGradeAndStatus(@Param("academicYearId") Long academicYearId,
            @Param("gradeLevelId") Long gradeLevelId, @Param("status") EnrollmentStatus status);

    long countByCurrentClassIdAndStatus(Long classId, EnrollmentStatus status);

    @Query("""
            select count(e.id) from StudentYearEnrollment e, SchoolClass c
            where c.id = :classId and e.academicYearId = c.academicYearId
              and e.enrolledAt <= :at and (e.completedAt is null or e.completedAt > :at)
              and ((e.currentClassId = :classId and not exists (select h.id from ClassTransferHistory h
                    where h.enrollmentId = e.id))
                or exists (select h.id from ClassTransferHistory h
                    where h.enrollmentId = e.id and h.effectiveAt <= :at and h.toClassId = :classId
                      and not exists (select newer.id from ClassTransferHistory newer
                        where newer.enrollmentId = e.id and newer.effectiveAt <= :at
                          and (newer.effectiveAt > h.effectiveAt
                            or (newer.effectiveAt = h.effectiveAt and newer.id > h.id)))))
            """)
    long countRosterAt(@Param("classId") Long classId, @Param("at") LocalDateTime at);

    @Query("""
            select count(e.id) from StudentYearEnrollment e, SchoolClass c
            where c.id = :classId and e.academicYearId = c.academicYearId
              and e.enrolledAt <= :at and (e.completedAt is null or e.completedAt > :at)
              and not exists (select h.id from ClassTransferHistory h
                where h.enrollmentId = e.id and h.effectiveAt <= :at)
            """)
    long countRosterRowsWithoutHistoryAt(@Param("classId") Long classId, @Param("at") LocalDateTime at);

    @Query("""
            select s from Student s
            where s.status = com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus.ACTIVE
              and not exists (select e.id from StudentYearEnrollment e
                where e.studentId = s.id and e.academicYearId = :academicYearId)
            order by s.studentCode
            """)
    List<Student> findUnassignedStudents(@Param("academicYearId") Long academicYearId);
}

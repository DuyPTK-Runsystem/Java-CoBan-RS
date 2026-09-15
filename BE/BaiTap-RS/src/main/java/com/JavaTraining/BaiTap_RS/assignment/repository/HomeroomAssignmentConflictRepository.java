package com.JavaTraining.BaiTap_RS.assignment.repository;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeroomAssignmentConflictRepository {

    boolean existsByClassIdAndTeacherIdAndStatus(Long classId, Long teacherId, AssignmentStatus status);

    @Query("""
            select count(assignment) > 0 from HomeroomAssignment assignment
            where assignment.classId = :classId and assignment.id <> :ignoredId
              and assignment.validFrom <= :validTo
              and (assignment.validTo is null or assignment.validTo >= :validFrom)
            """)
    boolean existsOverlap(@Param("classId") Long classId, @Param("ignoredId") Long ignoredId,
            @Param("validFrom") LocalDate validFrom, @Param("validTo") LocalDate validTo);

    @Query("""
            select count(assignment.id) > 0 from HomeroomAssignment assignment
            where assignment.classId = :classId and assignment.teacherId = :teacherId
              and assignment.status = :status and assignment.validFrom <= :effectiveDate
              and (assignment.validTo is null or assignment.validTo >= :effectiveDate)
            """)
    boolean existsActiveHomeroomAt(@Param("classId") Long classId, @Param("teacherId") Long teacherId,
            @Param("status") AssignmentStatus status, @Param("effectiveDate") LocalDate effectiveDate);

    @Query("""
            select count(assignment.id) > 0 from HomeroomAssignment assignment
            where assignment.classId = :classId and assignment.teacherId = :teacherId
              and assignment.status = :status and assignment.validFrom <= :to
              and (assignment.validTo is null or assignment.validTo >= :from)
            """)
    boolean existsActiveHomeroomBetween(@Param("classId") Long classId, @Param("teacherId") Long teacherId,
            @Param("status") AssignmentStatus status, @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select distinct assignment.classId from HomeroomAssignment assignment
            where assignment.teacherId = :teacherId and assignment.status = :status
            """)
    List<Long> findClassIdsByTeacherIdAndStatus(@Param("teacherId") Long teacherId,
            @Param("status") AssignmentStatus status);
}

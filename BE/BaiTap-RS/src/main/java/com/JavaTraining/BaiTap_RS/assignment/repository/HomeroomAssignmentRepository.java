package com.JavaTraining.BaiTap_RS.assignment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeroomAssignmentRepository extends JpaRepository<HomeroomAssignment, Long> {

    boolean existsByClassIdAndStatus(Long classId, AssignmentStatus status);

    boolean existsByTeacherId(Long teacherId);

    Optional<HomeroomAssignment> findFirstByClassIdAndStatus(Long classId, AssignmentStatus status);

    List<HomeroomAssignment> findAllByTeacherIdOrderByValidFromDesc(Long teacherId);

    List<HomeroomAssignment> findAllByClassIdOrderByValidFromDesc(Long classId);

    @Query("""
            select count(assignment) > 0
            from HomeroomAssignment assignment
            where assignment.classId = :classId
              and assignment.id <> :ignoredId
              and assignment.validFrom <= :validTo
              and (assignment.validTo is null or assignment.validTo >= :validFrom)
            """)
    boolean existsOverlap(
            @Param("classId") Long classId,
            @Param("ignoredId") Long ignoredId,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo);

    @Query("""
            select count(assignment.id) > 0
            from HomeroomAssignment assignment
            where assignment.classId = :classId
              and assignment.teacherId = :teacherId
              and assignment.status = :status
              and assignment.validFrom <= :effectiveDate
              and (assignment.validTo is null or assignment.validTo >= :effectiveDate)
            """)
    boolean existsActiveHomeroomAt(
            @Param("classId") Long classId,
            @Param("teacherId") Long teacherId,
            @Param("status") AssignmentStatus status,
            @Param("effectiveDate") LocalDate effectiveDate);
}

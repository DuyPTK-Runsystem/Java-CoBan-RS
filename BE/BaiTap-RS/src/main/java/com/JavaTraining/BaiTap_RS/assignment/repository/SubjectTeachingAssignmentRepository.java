package com.JavaTraining.BaiTap_RS.assignment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectTeachingAssignmentRepository extends JpaRepository<SubjectTeachingAssignment, Long> {

    boolean existsByClassSubjectIdAndStatus(Long classSubjectId, AssignmentStatus status);

    boolean existsByTeacherId(Long teacherId);

    Optional<SubjectTeachingAssignment> findFirstByClassSubjectIdAndStatus(
            Long classSubjectId,
            AssignmentStatus status);

    List<SubjectTeachingAssignment> findAllByTeacherIdOrderByValidFromDesc(Long teacherId);

    List<SubjectTeachingAssignment> findAllByClassSubjectIdOrderByValidFromDesc(Long classSubjectId);

    @Query("""
            select count(assignment) > 0
            from SubjectTeachingAssignment assignment
            where assignment.classSubjectId = :classSubjectId
              and assignment.id <> :ignoredId
              and assignment.validFrom <= :validTo
              and (assignment.validTo is null or assignment.validTo >= :validFrom)
            """)
    boolean existsOverlap(
            @Param("classSubjectId") Long classSubjectId,
            @Param("ignoredId") Long ignoredId,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo);

    @Query("""
            select count(assignment) > 0
            from SubjectTeachingAssignment assignment
            where assignment.teacherId = :teacherId
              and assignment.classSubjectId = :classSubjectId
              and assignment.status = 'ACTIVE'
              and assignment.validFrom <= :effectiveDate
              and (assignment.validTo is null or assignment.validTo >= :effectiveDate)
            """)
    boolean hasActiveAssignment(
            @Param("teacherId") Long teacherId,
            @Param("classSubjectId") Long classSubjectId,
            @Param("effectiveDate") LocalDate effectiveDate);

    @Query("""
            select count(assignment.id) > 0
            from SubjectTeachingAssignment assignment
            where assignment.teacherId = :teacherId
              and assignment.classSubjectId = :classSubjectId
              and assignment.status = :status
              and assignment.validFrom <= :to
              and (assignment.validTo is null or assignment.validTo >= :from)
            """)
    boolean existsActiveAssignmentBetween(
            @Param("teacherId") Long teacherId,
            @Param("classSubjectId") Long classSubjectId,
            @Param("status") AssignmentStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}

package com.JavaTraining.BaiTap_RS.assignment.repository;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EffectiveSubjectTeachingAssignmentRepository
        extends JpaRepository<SubjectTeachingAssignment, Long> {

    @Query("""
            select assignment
            from SubjectTeachingAssignment assignment
            where assignment.teacherId = :teacherId
              and assignment.status = 'ACTIVE'
              and assignment.validFrom <= :effectiveDate
              and (assignment.validTo is null or assignment.validTo >= :effectiveDate)
            order by assignment.validFrom desc
            """)
    List<SubjectTeachingAssignment> findEffectiveByTeacherId(
            @Param("teacherId") Long teacherId,
            @Param("effectiveDate") LocalDate effectiveDate);
}

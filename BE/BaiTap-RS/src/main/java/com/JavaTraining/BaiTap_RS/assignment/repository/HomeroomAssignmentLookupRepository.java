package com.JavaTraining.BaiTap_RS.assignment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeroomAssignmentLookupRepository {

    String CLASS_ID_PARAM = "classId";
    String STATUS_PARAM = "status";

    boolean existsByClassIdAndStatus(Long classId, AssignmentStatus status);

    boolean existsByTeacherId(Long teacherId);

    Optional<HomeroomAssignment> findFirstByClassIdAndStatus(Long classId, AssignmentStatus status);

    @Query("""
            select assignment from HomeroomAssignment assignment
            where assignment.classId = :classId and assignment.status = :status
              and assignment.validFrom <= :effectiveDate
              and (assignment.validTo is null or assignment.validTo >= :effectiveDate)
            order by assignment.validFrom desc, assignment.id desc
            """)
    Optional<HomeroomAssignment> findFirstActiveAt(
            @Param(CLASS_ID_PARAM) Long classId,
            @Param(STATUS_PARAM) AssignmentStatus status,
            @Param("effectiveDate") LocalDate effectiveDate);

    List<HomeroomAssignment> findAllByTeacherIdOrderByValidFromDesc(Long teacherId);

    List<HomeroomAssignment> findAllByClassIdOrderByValidFromDesc(Long classId);
}

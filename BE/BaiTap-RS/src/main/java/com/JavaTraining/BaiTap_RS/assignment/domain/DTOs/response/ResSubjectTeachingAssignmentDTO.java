package com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.response;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;

public record ResSubjectTeachingAssignmentDTO(
        Long id,
        Long classSubjectId,
        Long teacherId,
        LocalDate validFrom,
        LocalDate validTo,
        AssignmentStatus status,
        Long assignedBy,
        Long classId,
        String className,
        String classCode,
        Long subjectId,
        String subjectName,
        Long semesterId) {
}

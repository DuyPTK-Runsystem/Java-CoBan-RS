package com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.response;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;

public record ResHomeroomAssignmentDTO(
        Long id,
        Long classId,
        Long teacherId,
        LocalDate validFrom,
        LocalDate validTo,
        AssignmentStatus status,
        Long assignedBy) {
}

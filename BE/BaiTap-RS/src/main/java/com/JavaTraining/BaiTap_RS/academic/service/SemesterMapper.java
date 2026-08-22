package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import org.springframework.stereotype.Component;

@Component
public class SemesterMapper {

    public ResSemesterDTO toResponse(Semester semester) {
        return new ResSemesterDTO(
                semester.getId(),
                semester.getAcademicYearId(),
                semester.getCode(),
                semester.getName(),
                semester.getDisplayOrder(),
                semester.getStartDate(),
                semester.getEndDate(),
                semester.getAutomaticLockAt(),
                semester.getStatus(),
                semester.getLockedAt(),
                semester.getLockedBy(),
                semester.getLockReason());
    }

    public Map<String, Object> toAuditData(Semester semester) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", semester.getId());
        data.put("academicYearId", semester.getAcademicYearId());
        data.put("code", semester.getCode());
        data.put("status", semester.getStatus().name());
        data.put("lockedAt", semester.getLockedAt());
        data.put("lockedBy", semester.getLockedBy());
        data.put("lockReason", semester.getLockReason());
        return data;
    }
}

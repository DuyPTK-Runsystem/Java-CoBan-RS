package com.JavaTraining.BaiTap_RS.attendance.service;

import java.util.Comparator;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqClassAttendanceSummaryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResClassAttendanceSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class ClassAttendanceSummaryResponseMapper {

    public ResClassAttendanceSummaryDTO toResponse(
            SchoolClass schoolClass,
            ReqClassAttendanceSummaryQuery query,
            ClassAttendanceSummaryCollector.AggregatedClassData aggregatedData) {
        int page = query.resolvedPage();
        int size = query.resolvedSize();

        List<ResClassAttendanceSummaryDTO.StudentSummary> studentSummaries = aggregatedData.studentSummaries();
        int totalElements = studentSummaries.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        List<ResClassAttendanceSummaryDTO.StudentSummary> sortedStudents = studentSummaries.stream()
                .sorted(Comparator.comparing(ResClassAttendanceSummaryDTO.StudentSummary::studentCode)
                        .thenComparing(ResClassAttendanceSummaryDTO.StudentSummary::studentId))
                .toList();

        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);
        List<ResClassAttendanceSummaryDTO.StudentSummary> pagedStudents = sortedStudents.subList(start, end);

        ResClassAttendanceSummaryDTO.ClassInfo classInfo = new ResClassAttendanceSummaryDTO.ClassInfo(
                schoolClass.getId(),
                schoolClass.getClassName(),
                schoolClass.getGradeLevelId());

        return new ResClassAttendanceSummaryDTO(
                classInfo,
                query.semesterId(),
                query.from(),
                query.to(),
                aggregatedData.validSessionCount(),
                aggregatedData.classSummary(),
                pagedStudents,
                page,
                size,
                totalElements,
                totalPages);
    }
}

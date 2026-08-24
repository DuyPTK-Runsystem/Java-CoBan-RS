package com.JavaTraining.BaiTap_RS.attendance.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqClassAttendanceSummaryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResClassAttendanceSummaryDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassAttendanceSummaryService {

    private final AttendanceGuard attendanceGuard;
    private final ClassAttendanceSummaryCollector collector;
    private final ClassAttendanceSummaryResponseMapper responseMapper;

    public ClassAttendanceSummaryService(
            AttendanceGuard attendanceGuard,
            ClassAttendanceSummaryCollector collector,
            ClassAttendanceSummaryResponseMapper responseMapper) {
        this.attendanceGuard = attendanceGuard;
        this.collector = collector;
        this.responseMapper = responseMapper;
    }

    @Transactional(readOnly = true)
    public ResClassAttendanceSummaryDTO getClassSummary(Long classId, ReqClassAttendanceSummaryQuery query) {
        validateDateRange(query);
        SchoolClass schoolClass = attendanceGuard.findSchoolClass(classId);
        Semester semester = attendanceGuard.findSemester(query.semesterId());
        attendanceGuard.validateClassAndSemester(schoolClass, semester);
        attendanceGuard.validateCurrentUserHomeroomInRange(classId, query.from(), query.to());

        ClassAttendanceSummaryCollector.AggregatedClassData aggregatedData = collector.collect(
                classId,
                query.semesterId(),
                query.from(),
                query.to());

        return responseMapper.toResponse(schoolClass, query, aggregatedData);
    }

    private void validateDateRange(ReqClassAttendanceSummaryQuery query) {
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Khoảng ngày không hợp lệ");
        }
    }
}

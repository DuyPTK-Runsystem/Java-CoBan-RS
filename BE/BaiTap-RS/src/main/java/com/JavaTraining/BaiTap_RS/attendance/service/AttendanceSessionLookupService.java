package com.JavaTraining.BaiTap_RS.attendance.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqCreateAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceSessionLookupService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceGuard guard;
    private final AttendanceMapper mapper;

    public AttendanceSessionLookupService(
            AttendanceSessionRepository sessionRepository,
            AttendanceGuard guard,
            AttendanceMapper mapper) {
        this.sessionRepository = sessionRepository;
        this.guard = guard;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ResAttendanceSessionDTO getForTeacher(ReqCreateAttendanceSessionDTO request) {
        SchoolClass schoolClass = guard.findSchoolClass(request.classId());
        guard.assertCurrentUserHomeroom(schoolClass.getId(), request.attendanceDate());
        return find(request);
    }

    @Transactional(readOnly = true)
    public ResAttendanceSessionDTO getForOffice(ReqCreateAttendanceSessionDTO request) {
        return find(request);
    }

    private ResAttendanceSessionDTO find(ReqCreateAttendanceSessionDTO request) {
        AttendanceSession session = sessionRepository
                .findByClassIdAndSemesterIdAndAttendanceDateAndSessionPeriod(
                        request.classId(), request.semesterId(), request.attendanceDate(), request.sessionPeriod())
                .orElseThrow(() -> guard.notFound("Không tìm thấy buổi điểm danh"));
        return mapper.toSessionResponse(session);
    }
}

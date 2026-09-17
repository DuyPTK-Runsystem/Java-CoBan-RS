package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogClassResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogScheduleResponse;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogScheduleReadService {
    private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String SEMESTER_NOT_FOUND = "Không tìm thấy học kỳ";

    private final LessonLogScheduleService scheduleService;
    private final TeacherRepository teachers;
    private final SchoolClassRepository schoolClasses;
    private final SemesterRepository semesters;
    private final HomeroomAssignmentRepository homerooms;
    private final LessonLogResponseService responseService;
    private final StudentYearEnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public LessonLogScheduleResponse mySchedule(LocalDate date) {
        if (date == null) {
            throw error(HttpStatus.BAD_REQUEST, "date là bắt buộc");
        }
        Long userId = AuditContext.currentUserId();
        if (userId == null) {
            return new LessonLogScheduleResponse(date, TIME_ZONE.getId(), List.of());
        }
        Long teacherId = teachers.findByUserId(userId)
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Tài khoản chưa có giáo viên")).getId();
        return scheduleService.teacherSchedule(date, teacherId, responseService::map, this::roster);
    }

    @Transactional(readOnly = true)
    public List<LessonLogClassResponse> classes(Long semesterId) {
        semesters.findById(semesterId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, SEMESTER_NOT_FOUND));
        Set<Long> classIds = new LinkedHashSet<>();
        if (LessonLogActorAuthorization.isManager()) {
            schoolClasses.findAllBySemesterId(semesterId).stream().map(SchoolClass::getId).forEach(classIds::add);
        } else {
            Long userId = AuditContext.currentUserId();
            Long teacherId = teachers.findByUserId(userId == null ? -1L : userId)
                    .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Tài khoản chưa có giáo viên")).getId();
            Set<Long> semesterClassIds = schoolClasses.findAllBySemesterId(semesterId).stream()
                    .map(SchoolClass::getId).collect(java.util.stream.Collectors.toSet());
            homerooms.findClassIdsByTeacherIdAndStatus(teacherId, AssignmentStatus.ACTIVE).stream()
                    .filter(semesterClassIds::contains).forEach(classIds::add);
        }
        return schoolClasses.findAllByIdInOrderByClassCodeAsc(classIds).stream()
                .map(value -> new LessonLogClassResponse(value.getId(), semesterId, value.getClassCode(), value.getClassName()))
                .toList();
    }

    private int roster(Long classId, java.time.LocalDateTime at) {
        return Math.toIntExact(enrollmentRepository.countRosterAt(classId, at));
    }

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }
}

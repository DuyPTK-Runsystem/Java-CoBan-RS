package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogClassResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogClassWeekResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogPolicyResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogRevisionResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogScheduleResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;

class LessonLogReadFacade {
    protected LessonLogSourceResolver sourceResolver;
    protected LessonLogEntryLifecycleService lifecycleService;
    @Autowired
    private LessonLogQueryService queryService;

    protected LocalDate homeroomScopeDate(LocalDate weekStart, Semester semester) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (semester.getEndDate() != null && weekEnd.isAfter(semester.getEndDate())) {
            return semester.getEndDate();
        }
        if (semester.getStartDate() != null && weekEnd.isBefore(semester.getStartDate())) {
            return semester.getStartDate();
        }
        return weekEnd;
    }

    @Transactional(readOnly = true)
    public LessonLogEntryResponse get(Long entryId) {
        return queryService.get(entryId);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevision> revisions(Long entryId, int page, int size) {
        return queryService.revisions(entryId, page, size);
    }

    @Transactional(readOnly = true)
    public LessonLogScheduleResponse mySchedule(LocalDate date) {
        return queryService.mySchedule(date);
    }

    @Transactional(readOnly = true)
    public List<LessonLogClassResponse> classes(Long semesterId) {
        return queryService.classes(semesterId);
    }

    @Transactional(readOnly = true)
    public LessonLogClassWeekResponse classWeek(Long classId, Long semesterId, LocalDate weekStart) {
        return queryService.classWeek(classId, semesterId, weekStart);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevisionResponse> weeklyRevisions(Long classId, Long semesterId,
            LocalDate weekStart, int page, int size) {
        return queryService.weeklyRevisions(classId, semesterId, weekStart, page, size);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevisionResponse> policyRevisions(int page, int size) {
        return queryService.policyRevisions(page, size);
    }

    @Transactional(readOnly = true)
    public LessonLogPolicyResponse getPolicy(LocalDate date) {
        return queryService.getPolicy(date);
    }
}

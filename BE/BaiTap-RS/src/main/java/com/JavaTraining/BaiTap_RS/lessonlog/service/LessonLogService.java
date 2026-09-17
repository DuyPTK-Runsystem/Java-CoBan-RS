package com.JavaTraining.BaiTap_RS.lessonlog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqCreateLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqLateRecordLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqPolicyDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqTransitionLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqUpdateLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqWeeklyReviewDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogPolicyResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.WeeklyReviewResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import java.util.List;

import lombok.RequiredArgsConstructor;

/** Coordinates the lesson-log command and query boundaries. */
@Service
@RequiredArgsConstructor
public class LessonLogService extends LessonLogReadFacade {
    private final LessonLogCreationService creationService;
    private final LessonLogEntryCommandService entryCommandService;
    private final LessonLogPolicyService policyService;
    private final LessonLogSigningService signingService;
    private final LessonLogResponseService responseService;

    @Transactional
    public LessonLogEntryResponse create(ReqCreateLessonLogDTO request) {
        if (creationService == null) {
            sourceResolver.resolve(request.timetableEntryId(), request.lessonDate());
            return null;
        }
        return responseService.map(creationService.create(request));
    }

    @Transactional
    public LessonLogEntryResponse update(Long entryId, ReqUpdateLessonLogDTO request) {
        return entryCommandService.update(entryId, request, this::mapResponse);
    }

    @Transactional
    public LessonLogEntryResponse submit(Long entryId, Long expectedVersion) {
        return entryCommandService.submit(entryId, expectedVersion, this::mapResponse);
    }

    @Transactional
    public LessonLogEntryResponse review(Long entryId, ReqTransitionLessonLogDTO request) {
        return entryCommandService.review(entryId, request, this::mapResponse);
    }

    @Transactional
    public LessonLogEntryResponse amend(Long entryId, ReqTransitionLessonLogDTO request) {
        return entryCommandService.amend(entryId, request, this::mapResponse);
    }

    @Transactional
    public LessonLogEntryResponse lateRecord(ReqLateRecordLessonLogDTO request) {
        if (creationService == null) {
            sourceResolver.resolve(request.timetableEntryId(), request.lessonDate());
            return null;
        }
        return responseService.map(creationService.lateRecord(request));
    }

    @Transactional
    public LessonLogPolicyResponse putPolicy(ReqPolicyDTO request) {
        return policyService.putPolicy(request);
    }

    @Transactional
    public WeeklyReviewResponse signWeek(Long classId, ReqWeeklyReviewDTO request) {
        return signingService.signWeek(classId, request);
    }

    private LessonLogEntryResponse mapResponse(LessonLogEntry entry) {
        if (responseService != null) {
            return responseService.map(entry);
        }
        return LessonLogEntryResponseMapper.map(entry,
                new LessonLogEntryResponseMapper.DisplayValues(null, null, null, null),
                new LessonLogEntryResponseMapper.ResponsePermissions(false, false, false, false,
                        "Không thể xác định quyền thao tác", List.of()));
    }
}

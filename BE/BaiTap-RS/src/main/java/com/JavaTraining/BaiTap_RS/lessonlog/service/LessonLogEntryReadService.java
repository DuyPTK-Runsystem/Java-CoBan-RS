package com.JavaTraining.BaiTap_RS.lessonlog.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogRevisionRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogEntryReadService {
    private final LessonLogEntryRepository entries;
    private final LessonLogRevisionRepository audits;
    private final TeacherRepository teachers;
    private final HomeroomAssignmentRepository homerooms;
    private final LessonLogResponseService responseService;

    @Transactional(readOnly = true)
    public LessonLogEntryResponse get(Long id) {
        LessonLogEntry entry = load(id);
        LessonLogScopeAuthorization.assertEntryReadScope(entry, teachers, homerooms);
        return responseService.map(entry);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevision> revisions(Long id, int page, int size) {
        LessonLogEntry entry = load(id);
        LessonLogScopeAuthorization.assertEntryReadScope(entry, teachers, homerooms);
        org.springframework.data.domain.Page<LessonLogRevision> resultPage = audits.findByEntryIdOrderByCreatedAtDesc(id,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(resultPage.getNumber(), resultPage.getSize(),
                resultPage.getTotalPages(), resultPage.getTotalElements()), resultPage.getContent());
    }

    private LessonLogEntry load(Long id) {
        return entries.findById(id).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                "Không tìm thấy sổ đầu bài"));
    }
}

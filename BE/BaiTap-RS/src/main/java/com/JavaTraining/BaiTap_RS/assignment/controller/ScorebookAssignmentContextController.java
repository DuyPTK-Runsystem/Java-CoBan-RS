package com.JavaTraining.BaiTap_RS.assignment.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.response.ResSubjectTeachingAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.service.SubjectTeachingAssignmentAccessService;
import com.JavaTraining.BaiTap_RS.assignment.service.SubjectTeachingAssignmentService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
public class ScorebookAssignmentContextController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SubjectTeachingAssignmentService subjectTeachingAssignmentService;
    private final SubjectTeachingAssignmentAccessService subjectTeachingAssignmentAccessService;

    public ScorebookAssignmentContextController(
            SubjectTeachingAssignmentService subjectTeachingAssignmentService,
            SubjectTeachingAssignmentAccessService subjectTeachingAssignmentAccessService) {
        this.subjectTeachingAssignmentService = subjectTeachingAssignmentService;
        this.subjectTeachingAssignmentAccessService = subjectTeachingAssignmentAccessService;
    }

    @GetMapping("/assignments/me/scorebook-context")
    @ApiMessage("Lấy phân công sổ điểm hiện tại")
    @PreAuthorize("hasRole('TEACHER')")
    public List<ResSubjectTeachingAssignmentDTO> listMyEffectiveScorebookAssignments() {
        Long teacherId = subjectTeachingAssignmentAccessService.currentTeacherId();
        return subjectTeachingAssignmentService.listEffectiveScorebookAssignments(
                teacherId, LocalDate.now(BUSINESS_ZONE));
    }
}

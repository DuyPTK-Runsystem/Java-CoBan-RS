package com.JavaTraining.BaiTap_RS.assignment.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqCreateHomeroomAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqCreateSubjectTeachingAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqEndAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqReplaceAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.response.ResHomeroomAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.response.ResSubjectTeachingAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.service.HomeroomAssignmentService;
import com.JavaTraining.BaiTap_RS.assignment.service.SubjectTeachingAssignmentAccessService;
import com.JavaTraining.BaiTap_RS.assignment.service.SubjectTeachingAssignmentService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2")
@SuppressWarnings("PMD.GuardLogStatement")
public class AssignmentController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String VIEW_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";
    private static final String ASSIGNMENT_ID = "assignmentId";

    private final HomeroomAssignmentService homeroomAssignmentService;
    private final SubjectTeachingAssignmentService subjectTeachingAssignmentService;
    private final SubjectTeachingAssignmentAccessService subjectTeachingAssignmentAccessService;

    public AssignmentController(
            HomeroomAssignmentService homeroomAssignmentService,
            SubjectTeachingAssignmentService subjectTeachingAssignmentService,
            SubjectTeachingAssignmentAccessService subjectTeachingAssignmentAccessService) {
        this.homeroomAssignmentService = homeroomAssignmentService;
        this.subjectTeachingAssignmentService = subjectTeachingAssignmentService;
        this.subjectTeachingAssignmentAccessService = subjectTeachingAssignmentAccessService;
    }

    @GetMapping("/assignments/classes/{classId}")
    @ApiMessage("Lấy phân công theo lớp")
    @PreAuthorize(VIEW_ROLES)
    public List<ResHomeroomAssignmentDTO> listHomeroomByClass(
            @PathVariable("classId") @Positive Long classId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AssignmentController.class,
                        "AssignmentController.listHomeroomByClass");
        subjectTeachingAssignmentAccessService.assertCanViewClass(classId);
        return homeroomAssignmentService.listHomeroomByClass(classId);
    }

    @GetMapping("/assignments/teachers/{teacherId}")
    @ApiMessage("Lấy phân công GVBM theo giáo viên")
    @PreAuthorize(VIEW_ROLES)
    public List<ResSubjectTeachingAssignmentDTO> listSubjectTeachingByTeacher(
            @PathVariable("teacherId") @Positive Long teacherId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AssignmentController.class,
                        "AssignmentController.listSubjectTeachingByTeacher");
        subjectTeachingAssignmentAccessService.assertCanViewAssignments(teacherId);
        return subjectTeachingAssignmentService.listSubjectTeachingByTeacher(teacherId);
    }

    @GetMapping("/assignments/classes/{classId}/subjects")
    @ApiMessage("Lấy phân công GVBM theo lớp và học kỳ")
    @PreAuthorize(VIEW_ROLES)
    public List<ResSubjectTeachingAssignmentDTO> listSubjectTeachingByClassAndSemester(
            @PathVariable("classId") @Positive Long classId,
            @RequestParam("semesterId") @Positive Long semesterId) {
        subjectTeachingAssignmentAccessService.assertCanViewClass(classId);
        return subjectTeachingAssignmentService.listSubjectTeachingByClassAndSemester(classId, semesterId);
    }

    @GetMapping("/assignments/teachers/{teacherId}/homeroom")
    @ApiMessage("Lấy phân công GVCN theo giáo viên")
    @PreAuthorize(VIEW_ROLES)
    public List<ResHomeroomAssignmentDTO> listHomeroomByTeacher(
            @PathVariable("teacherId") @Positive Long teacherId) {
        subjectTeachingAssignmentAccessService.assertCanViewAssignments(teacherId);
        return homeroomAssignmentService.listHomeroomByTeacher(teacherId);
    }

    @PostMapping("/classes/{classId}/homeroom-assignments")
    @ApiMessage("Tạo phân công GVCN")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResHomeroomAssignmentDTO> createHomeroomAssignment(
            @PathVariable("classId") @Positive Long classId,
            @Valid @RequestBody ReqCreateHomeroomAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AssignmentController.class,
                        "AssignmentController.createHomeroomAssignment");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(homeroomAssignmentService.createHomeroomAssignment(classId, request));
    }

    @PostMapping("/homeroom-assignments/{assignmentId}/replace")
    @ApiMessage("Thay phân công GVCN")
    @PreAuthorize(OFFICE_ROLES)
    public ResHomeroomAssignmentDTO replaceHomeroomAssignment(
            @PathVariable(ASSIGNMENT_ID) @Positive Long assignmentId,
            @Valid @RequestBody ReqReplaceAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AssignmentController.class,
                        "AssignmentController.replaceHomeroomAssignment");
        return homeroomAssignmentService.replaceHomeroomAssignment(assignmentId, request);
    }

    @PostMapping("/homeroom-assignments/{assignmentId}/end")
    @ApiMessage("Kết thúc phân công GVCN")
    @PreAuthorize(OFFICE_ROLES)
    public ResHomeroomAssignmentDTO endHomeroomAssignment(
            @PathVariable(ASSIGNMENT_ID) @Positive Long assignmentId,
            @Valid @RequestBody ReqEndAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AssignmentController.class,
                        "AssignmentController.endHomeroomAssignment");
        return homeroomAssignmentService.endHomeroomAssignment(assignmentId, request);
    }

    @PostMapping("/class-subjects/{classSubjectId}/teaching-assignments")
    @ApiMessage("Tạo phân công GVBM")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResSubjectTeachingAssignmentDTO> createSubjectTeachingAssignment(
            @PathVariable("classSubjectId") @Positive Long classSubjectId,
            @Valid @RequestBody ReqCreateSubjectTeachingAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AssignmentController.class,
                        "AssignmentController.createSubjectTeachingAssignment");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectTeachingAssignmentService.createSubjectTeachingAssignment(classSubjectId, request));
    }

    @PostMapping("/subject-teaching-assignments/{assignmentId}/replace")
    @ApiMessage("Thay phân công GVBM")
    @PreAuthorize(OFFICE_ROLES)
    public ResSubjectTeachingAssignmentDTO replaceSubjectTeachingAssignment(
            @PathVariable(ASSIGNMENT_ID) @Positive Long assignmentId,
            @Valid @RequestBody ReqReplaceAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AssignmentController.class,
                        "AssignmentController.replaceSubjectTeachingAssignment");
        return subjectTeachingAssignmentService.replaceSubjectTeachingAssignment(assignmentId, request);
    }

    @PostMapping("/subject-teaching-assignments/{assignmentId}/end")
    @ApiMessage("Kết thúc phân công GVBM")
    @PreAuthorize(OFFICE_ROLES)
    public ResSubjectTeachingAssignmentDTO endSubjectTeachingAssignment(
            @PathVariable(ASSIGNMENT_ID) @Positive Long assignmentId,
            @Valid @RequestBody ReqEndAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AssignmentController.class,
                        "AssignmentController.endSubjectTeachingAssignment");
        return subjectTeachingAssignmentService.endSubjectTeachingAssignment(assignmentId, request);
    }
}

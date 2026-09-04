package com.JavaTraining.BaiTap_RS.teacher.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.requests.ReqCreateTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.requests.ReqUpdateTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.response.ResTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.service.TeacherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/teachers")
@SuppressWarnings("PMD.GuardLogStatement")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách giáo viên")
    @PreAuthorize("isAuthenticated()")
    public List<ResTeacherDTO> listTeachers(
            @RequestParam(value = "status", required = false) TeacherStatus status) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TeacherController.class,
                        "TeacherController.listTeachers");
        return teacherService.listTeachers(status);
    }

    @GetMapping("/{teacherId}")
    @ApiMessage("Lấy chi tiết giáo viên")
    @PreAuthorize("isAuthenticated()")
    public ResTeacherDTO getTeacher(@PathVariable("teacherId") @Positive Long teacherId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                TeacherController.class,
                "TeacherController.getTeacher");
        return teacherService.getTeacher(teacherId);
    }

    @PostMapping
    @ApiMessage("Tạo giáo viên")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    public ResponseEntity<ResTeacherDTO> createTeacher(@Valid @RequestBody ReqCreateTeacherDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                TeacherController.class,
                "TeacherController.createTeacher");
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.createTeacher(request));
    }

    @PutMapping("/{teacherId}")
    @ApiMessage("Cập nhật giáo viên")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    public ResTeacherDTO updateTeacher(
            @PathVariable("teacherId") @Positive Long teacherId,
            @Valid @RequestBody ReqUpdateTeacherDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TeacherController.class,
                        "TeacherController.updateTeacher");
        return teacherService.updateTeacher(teacherId, request);
    }

    @DeleteMapping("/{teacherId}")
    @ApiMessage("Xóa giáo viên")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    public ResponseEntity<Void> deleteTeacher(@PathVariable("teacherId") @Positive Long teacherId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                TeacherController.class,
                "TeacherController.deleteTeacher");
        teacherService.deleteTeacher(teacherId);
        return ResponseEntity.noContent().build();
    }
}

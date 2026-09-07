package com.JavaTraining.BaiTap_RS.student.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpdateStudentStatusDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpsertStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentCodeDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentV2PageDTO;
import com.JavaTraining.BaiTap_RS.student.service.StudentCodeGenerationService;
import com.JavaTraining.BaiTap_RS.student.service.StudentV2Service;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/students")
public class StudentV2Controller {

    private static final String READ_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";
    private static final String MUTATION_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String STUDENT_ID = "studentId";

    private final StudentV2Service studentV2Service;
    private final StudentCodeGenerationService studentCodeGenerationService;

    public StudentV2Controller(
            StudentV2Service studentV2Service, StudentCodeGenerationService studentCodeGenerationService) {
        this.studentV2Service = studentV2Service;
        this.studentCodeGenerationService = studentCodeGenerationService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách học sinh v2")
    @PreAuthorize(READ_ROLES)
    public ResStudentV2PageDTO fetchStudents(@Valid ReqFetchStudentV2DTO request) {
        return studentV2Service.fetchStudents(request);
    }

    @GetMapping("/{studentId}")
    @ApiMessage("Lấy hồ sơ học sinh v2")
    @PreAuthorize(READ_ROLES)
    public ResStudentV2DTO getStudent(@PathVariable(STUDENT_ID) @Positive Long studentId) {
        return studentV2Service.getStudent(studentId);
    }

    @GetMapping("/by-code/{studentCode}")
    @ApiMessage("Lấy hồ sơ học sinh v2 theo mã")
    @PreAuthorize(READ_ROLES)
    public ResStudentV2DTO getStudentByCode(@PathVariable("studentCode") String studentCode) {
        return studentV2Service.getStudentByCode(studentCode);
    }

    @PostMapping("/code")
    @ApiMessage("Tạo mã học sinh v2")
    @PreAuthorize(READ_ROLES)
    public ResStudentCodeDTO generateStudentCode() {
        return studentCodeGenerationService.generateStudentCode();
    }

    @PostMapping
    @ApiMessage("Tạo học sinh v2")
    @PreAuthorize(MUTATION_ROLES)
    public ResponseEntity<ResStudentV2DTO> createStudent(@Valid @RequestBody ReqUpsertStudentV2DTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentV2Service.createStudent(request));
    }

    @PutMapping("/{studentId}")
    @ApiMessage("Cập nhật học sinh v2")
    @PreAuthorize(MUTATION_ROLES)
    public ResStudentV2DTO updateStudent(
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @Valid @RequestBody ReqUpsertStudentV2DTO request) {
        return studentV2Service.updateStudent(studentId, request);
    }

    @PatchMapping("/{studentId}/status")
    @ApiMessage("Chuyển trạng thái học sinh")
    @PreAuthorize(MUTATION_ROLES)
    public ResStudentV2DTO transitionStatus(
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @Valid @RequestBody ReqUpdateStudentStatusDTO request) {
        return studentV2Service.transitionStatus(studentId, request);
    }

    @DeleteMapping("/{studentId}")
    @ApiMessage("Xóa học sinh khi chưa có dữ liệu học vụ")
    @PreAuthorize(MUTATION_ROLES)
    public ResponseEntity<Void> deleteStudent(@PathVariable(STUDENT_ID) @Positive Long studentId) {
        studentV2Service.deleteStudent(studentId);
        return ResponseEntity.noContent().build();
    }
}

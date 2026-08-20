package com.JavaTraining.BaiTap_RS.student.controller;

import com.JavaTraining.BaiTap_RS.batch.studentcsv.StudentCsvExportService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqCreateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpdateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentCodeDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentPageDTO;
import com.JavaTraining.BaiTap_RS.student.service.StudentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/students")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")
public class StudentController {

    private final StudentService studentService;
    private final StudentCsvExportService studentCsvExportService;

    public StudentController(StudentService studentService, StudentCsvExportService studentCsvExportService) {
        this.studentService = studentService;
        this.studentCsvExportService = studentCsvExportService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách sinh viên")
    public ResStudentPageDTO fetchStudents(@Valid ReqFetchStudentDTO request) {
        return studentService.fetchStudents(request);
    }

    @GetMapping("/{studentId}")
    @ApiMessage("Lấy thông tin sinh viên")
    public ResStudentDTO getStudent(
            @PathVariable("studentId") @Positive(message = "ID sinh viên phải lớn hơn 0") Long studentId) {
        return studentService.getStudent(studentId);
    }

    @PostMapping
    @ApiMessage("Tạo sinh viên")
    public ResponseEntity<ResStudentDTO> createStudent(@Valid @RequestBody ReqCreateStudentDTO request) {
        ResStudentDTO student = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(student);
    }

    @PutMapping("/{studentId}")
    @ApiMessage("Cập nhật sinh viên")
    public ResStudentDTO updateStudent(
            @PathVariable("studentId") @Positive(message = "ID sinh viên phải lớn hơn 0") Long studentId,
            @Valid @RequestBody ReqUpdateStudentDTO request) {
        return studentService.updateStudent(studentId, request);
    }

    @DeleteMapping("/{studentId}")
    @ApiMessage("Xóa sinh viên")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable("studentId") @Positive(message = "ID sinh viên phải lớn hơn 0") Long studentId) {
        studentService.deleteStudent(studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/code")
    @ApiMessage("Tạo mã sinh viên")
    public ResStudentCodeDTO generateStudentCode() {
        return studentService.generateStudentCode();
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportStudents() {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students.csv")
                .body(studentCsvExportService.exportStudents());
    }
}

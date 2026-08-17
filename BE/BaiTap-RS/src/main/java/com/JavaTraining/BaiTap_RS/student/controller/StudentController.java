package com.JavaTraining.BaiTap_RS.student.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqCreateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqUpdateStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentCodeDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentPageDTO;
import com.JavaTraining.BaiTap_RS.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách sinh viên")
    public ResStudentPageDTO fetchStudents(ReqFetchStudentDTO request) {
        return studentService.fetchStudents(request);
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
            @PathVariable Long studentId,
            @Valid @RequestBody ReqUpdateStudentDTO request) {
        return studentService.updateStudent(studentId, request);
    }

    @DeleteMapping("/{studentId}")
    @ApiMessage("Xóa sinh viên")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long studentId) {
        studentService.deleteStudent(studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/code")
    @ApiMessage("Tạo mã sinh viên")
    public ResStudentCodeDTO generateStudentCode() {
        return studentService.generateStudentCode();
    }
}

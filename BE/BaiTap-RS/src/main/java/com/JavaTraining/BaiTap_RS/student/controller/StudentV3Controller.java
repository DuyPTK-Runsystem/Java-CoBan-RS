package com.JavaTraining.BaiTap_RS.student.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqCreateStudentV3DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentWithAccountDTO;
import com.JavaTraining.BaiTap_RS.student.service.StudentAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3/students")
public class StudentV3Controller {

    private final StudentAccountService studentAccountService;

    public StudentV3Controller(StudentAccountService studentAccountService) {
        this.studentAccountService = studentAccountService;
    }

    @PostMapping
    @ApiMessage("Tạo học sinh và tài khoản")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    public ResponseEntity<ResStudentWithAccountDTO> createStudentWithAccount(
            @Valid @RequestBody ReqCreateStudentV3DTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentAccountService.createStudentWithAccount(request));
    }
}

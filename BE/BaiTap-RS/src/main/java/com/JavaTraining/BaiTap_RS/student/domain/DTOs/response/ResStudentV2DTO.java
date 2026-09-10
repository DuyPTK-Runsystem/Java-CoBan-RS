package com.JavaTraining.BaiTap_RS.student.domain.DTOs.response;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentGender;

public record ResStudentV2DTO(
        Long studentId,
        String studentCode,
        String studentName,
        LocalDate dateOfBirth,
        String address,
        StudentGender gender,
        StudentStatus status,
        Long currentClassId,
        String currentClassCode,
        Account account) {

    public record Account(Long userId, String username, String role) {
    }
}

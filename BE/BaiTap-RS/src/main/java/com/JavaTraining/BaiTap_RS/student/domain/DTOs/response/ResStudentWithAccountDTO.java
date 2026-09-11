package com.JavaTraining.BaiTap_RS.student.domain.DTOs.response;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentGender;

public record ResStudentWithAccountDTO(
        Long studentId,
        String studentCode,
        String studentName,
        LocalDate dateOfBirth,
        String address,
        Double averageScore,
        StudentGender gender,
        Account account) {

    public record Account(Long userId, String username, String role) {
    }
}

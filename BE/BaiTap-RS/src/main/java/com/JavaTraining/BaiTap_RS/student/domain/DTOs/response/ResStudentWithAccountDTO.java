package com.JavaTraining.BaiTap_RS.student.domain.DTOs.response;

import java.time.LocalDate;

public record ResStudentWithAccountDTO(
        Long studentId,
        String studentCode,
        String studentName,
        LocalDate dateOfBirth,
        String address,
        Double averageScore,
        Account account) {

    public record Account(Long userId, String username, String role) {
    }
}

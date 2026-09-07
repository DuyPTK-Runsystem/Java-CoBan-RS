package com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.response;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;

public record ResTeacherDTO(
        Long id,
        Long userId,
        String teacherCode,
        String teacherName,
        LocalDate dateOfBirth,
        String gender,
        String phone,
        String email,
        String department,
        LocalDate joinDate,
        TeacherStatus status) {
}

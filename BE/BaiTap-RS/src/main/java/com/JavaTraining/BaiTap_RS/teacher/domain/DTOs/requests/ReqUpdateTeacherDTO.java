package com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.requests;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqUpdateTeacherDTO(
        @Positive Long userId,
        @NotBlank @Size(max = 50) String teacherCode,
        @NotBlank @Size(max = 150) String teacherName,
        LocalDate dateOfBirth,
        @Size(max = 20) String gender,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email,
        @Size(max = 100) String department,
        LocalDate joinDate,
        @NotNull TeacherStatus status) {
}

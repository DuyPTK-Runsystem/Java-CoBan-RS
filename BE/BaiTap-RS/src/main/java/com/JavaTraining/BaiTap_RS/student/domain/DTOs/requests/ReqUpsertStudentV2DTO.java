package com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentGender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReqUpsertStudentV2DTO(
        @NotBlank
        @Pattern(regexp = "STU[0-9]{7}")
        String studentCode,
        @NotBlank
        @Size(max = 35)
        String studentName,
        @PastOrPresent
        LocalDate dateOfBirth,
        @Size(max = 255)
        String address,
        StudentGender gender) {

    public ReqUpsertStudentV2DTO(
            String studentCode, String studentName, LocalDate dateOfBirth, String address) {
        this(studentCode, studentName, dateOfBirth, address, null);
    }
}

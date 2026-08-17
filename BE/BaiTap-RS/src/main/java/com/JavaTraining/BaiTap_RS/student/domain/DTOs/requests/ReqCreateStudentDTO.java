package com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqCreateStudentDTO {

    @NotBlank(message = "Mã sinh viên không được để trống")
    @Pattern(regexp = "STU[0-9]{7}", message = "Mã sinh viên phải có định dạng STU và 7 chữ số")
    private String studentCode;

    @NotBlank(message = "Tên sinh viên không được để trống")
    @Size(max = 35, message = "Tên sinh viên tối đa 35 ký tự")
    private String studentName;

    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    private Double averageScore;
}

package com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReqCreateStudentV3DTO(
        @NotBlank(message = "Mã sinh viên không được để trống")
        @Pattern(regexp = "STU[0-9]{7}", message = "Mã sinh viên phải có định dạng STU và 7 chữ số")
        String studentCode,

        @NotBlank(message = "Tên sinh viên không được để trống")
        @Size(max = 35, message = "Tên sinh viên tối đa 35 ký tự")
        String studentName,

        @PastOrPresent(message = "Ngày sinh không được ở tương lai")
        LocalDate dateOfBirth,

        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        String address,

        @DecimalMin(value = "0.0", message = "Điểm trung bình phải từ 0 đến 10")
        @DecimalMax(value = "10.0", message = "Điểm trung bình phải từ 0 đến 10")
        Double averageScore,

        @Size(max = 20, message = "Tên đăng nhập tối đa 20 ký tự")
        @Pattern(regexp = "\\A\\p{ASCII}+\\z", message = "Tên đăng nhập chỉ được dùng ký tự 1-byte")
        String username,

        @Size(min = 6, max = 15, message = "Mật khẩu phải từ 6 đến 15 ký tự")
        @Pattern(regexp = "\\A\\p{ASCII}+\\z", message = "Mật khẩu chỉ được dùng ký tự 1-byte")
        String password) {
}

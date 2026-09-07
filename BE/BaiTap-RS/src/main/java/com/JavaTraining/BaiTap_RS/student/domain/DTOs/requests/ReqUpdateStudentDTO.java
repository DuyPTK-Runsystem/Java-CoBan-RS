package com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqUpdateStudentDTO {

    @NotBlank(message = "Tên sinh viên không được để trống")
    @Size(max = 35, message = "Tên sinh viên tối đa 35 ký tự")
    private String studentName;

    @PastOrPresent(message = "Ngày sinh không được ở tương lai")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @DecimalMin(value = "0.0", message = "Điểm trung bình phải từ 0 đến 10")
    @DecimalMax(value = "10.0", message = "Điểm trung bình phải từ 0 đến 10")
    private Double averageScore;
}

package com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqFetchStudentDTO {

    @Size(max = 10, message = "Mã sinh viên tìm kiếm tối đa 10 ký tự")
    private String studentCode;

    @Size(max = 35, message = "Tên sinh viên tìm kiếm tối đa 35 ký tự")
    private String studentName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "Ngày sinh không được ở tương lai")
    private LocalDate birthday;

    @PositiveOrZero(message = "Trang không được nhỏ hơn 0")
    private int page;

    @PositiveOrZero(message = "Kích thước trang không được nhỏ hơn 0")
    private int size;

    private String sortField;

    private String sortDirection;
}

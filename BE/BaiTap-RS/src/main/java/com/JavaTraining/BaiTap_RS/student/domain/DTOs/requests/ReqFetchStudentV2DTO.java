package com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
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
public class ReqFetchStudentV2DTO {

    @Size(max = 10)
    private String studentCode;

    @Size(max = 35)
    private String studentName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent
    private LocalDate birthday;

    private StudentStatus status;

    @Positive
    private Long classId;

    @PositiveOrZero
    private int page;

    @PositiveOrZero
    private int size;

    private String sortField;

    private String sortDirection;
}

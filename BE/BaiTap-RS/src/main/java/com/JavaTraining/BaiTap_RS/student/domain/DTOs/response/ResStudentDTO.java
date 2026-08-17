package com.JavaTraining.BaiTap_RS.student.domain.DTOs.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResStudentDTO {

    private Long studentId;

    private String studentCode;

    private String studentName;

    private LocalDate dateOfBirth;

    private String address;

    private Double averageScore;
}

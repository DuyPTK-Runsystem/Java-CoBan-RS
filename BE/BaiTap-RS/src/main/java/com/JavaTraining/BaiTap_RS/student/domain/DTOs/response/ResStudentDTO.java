package com.JavaTraining.BaiTap_RS.student.domain.DTOs.response;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
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

    private StudentStatus status;

    private String currentClassCode;

    public ResStudentDTO(Long studentId, String studentCode, String studentName,
            LocalDate dateOfBirth, String address, Double averageScore) {
        this(studentId, studentCode, studentName, dateOfBirth, address, averageScore, null, null);
    }
}

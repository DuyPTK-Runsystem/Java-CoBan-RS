package com.JavaTraining.BaiTap_RS.student.domain.DTOs.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResStudentPageDTO {

    private List<ResStudentDTO> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;
}

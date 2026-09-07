package com.JavaTraining.BaiTap_RS.student.domain.DTOs.response;

import java.util.List;

public record ResStudentV2PageDTO(
        List<ResStudentV2DTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}

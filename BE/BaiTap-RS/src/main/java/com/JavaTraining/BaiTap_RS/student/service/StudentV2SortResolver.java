package com.JavaTraining.BaiTap_RS.student.service;

import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

final class StudentV2SortResolver {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "studentCode", "studentCode",
            "studentName", "studentName",
            "dateOfBirth", "studentInfo.dateOfBirth",
            "status", "status");

    private StudentV2SortResolver() {
    }

    /* default */ static Sort resolve(String sortField, String sortDirection) {
        String field = StringUtils.hasText(sortField) ? sortField.trim() : "studentCode";
        String property = SORT_FIELDS.get(field);
        if (property == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Trường sắp xếp không được hỗ trợ");
        }
        if (!StringUtils.hasText(sortDirection) || "asc".equalsIgnoreCase(sortDirection.trim())) {
            return Sort.by(Sort.Direction.ASC, property);
        }
        if ("desc".equalsIgnoreCase(sortDirection.trim())) {
            return Sort.by(Sort.Direction.DESC, property);
        }
        throw new AppException(HttpStatus.BAD_REQUEST, "Chiều sắp xếp không được hỗ trợ");
    }
}

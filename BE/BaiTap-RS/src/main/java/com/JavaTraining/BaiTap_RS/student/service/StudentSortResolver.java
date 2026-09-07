package com.JavaTraining.BaiTap_RS.student.service;

import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

final class StudentSortResolver {

    /* default */ static final String STUDENT_CODE_FIELD = "studentCode";
    /* default */ static final String STUDENT_NAME_FIELD = "studentName";
    /* default */ static final String STUDENT_INFO_FIELD = "studentInfo";
    /* default */ static final String DATE_OF_BIRTH_FIELD = "dateOfBirth";

    private static final Map<String, String> SORT_FIELDS = Map.of(
            STUDENT_CODE_FIELD, STUDENT_CODE_FIELD,
            STUDENT_NAME_FIELD, STUDENT_NAME_FIELD,
            DATE_OF_BIRTH_FIELD, STUDENT_INFO_FIELD + "." + DATE_OF_BIRTH_FIELD,
            "address", STUDENT_INFO_FIELD + ".address",
            "averageScore", STUDENT_INFO_FIELD + ".averageScore");

    private StudentSortResolver() {
    }

    /* default */ static Sort resolve(String sortField, String sortDirection) {
        String field = StringUtils.hasText(sortField) ? sortField.trim() : STUDENT_CODE_FIELD;
        String property = SORT_FIELDS.get(field);
        if (property == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Trường sắp xếp không được hỗ trợ");
        }
        return Sort.by(resolveDirection(sortDirection), property);
    }

    private static Sort.Direction resolveDirection(String sortDirection) {
        if (!StringUtils.hasText(sortDirection) || "asc".equalsIgnoreCase(sortDirection.trim())) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(sortDirection.trim())) {
            return Sort.Direction.DESC;
        }
        throw new AppException(HttpStatus.BAD_REQUEST, "Chiều sắp xếp không được hỗ trợ");
    }
}

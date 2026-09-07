package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.util.List;

public record ClassSubjectIncompleteDetail(
        Long classSubjectId,
        Long classId,
        Long subjectId,
        String className,
        String subjectName,
        List<String> issues) {
}

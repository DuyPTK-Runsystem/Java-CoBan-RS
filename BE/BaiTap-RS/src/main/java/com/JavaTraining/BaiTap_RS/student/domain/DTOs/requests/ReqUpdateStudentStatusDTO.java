package com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import jakarta.validation.constraints.NotNull;

public record ReqUpdateStudentStatusDTO(@NotNull StudentStatus status) {
}

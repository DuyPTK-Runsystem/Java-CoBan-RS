package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import jakarta.validation.constraints.NotNull;

public record ReqUpdateClassSubjectDTO(@NotNull ClassSubjectStatus status) {
}

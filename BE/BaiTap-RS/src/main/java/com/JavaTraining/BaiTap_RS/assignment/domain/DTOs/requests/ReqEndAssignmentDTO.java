package com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ReqEndAssignmentDTO(@NotNull LocalDate validTo) {
}

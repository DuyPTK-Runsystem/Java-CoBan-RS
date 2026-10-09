package com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqUpdateLibraryPatronStatusDTO(
        @NotNull LibraryPatronStatus status,
        @Size(max = 500) String reason) {
}

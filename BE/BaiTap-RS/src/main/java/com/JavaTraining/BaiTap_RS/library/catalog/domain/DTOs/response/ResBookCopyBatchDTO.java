package com.JavaTraining.BaiTap_RS.library.catalog.domain.DTOs.response;

import java.util.List;

public record ResBookCopyBatchDTO(Long bookId, int createdCount, List<ResBookCopyDTO> copies) {
}

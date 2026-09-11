package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record ReqUpdateSubjectFunctionalRoomsDTO(
                @NotNull(message = "Danh sách phòng chức năng không được null") List<Long> functionalRoomIds,

                Long expectedVersion) {
}

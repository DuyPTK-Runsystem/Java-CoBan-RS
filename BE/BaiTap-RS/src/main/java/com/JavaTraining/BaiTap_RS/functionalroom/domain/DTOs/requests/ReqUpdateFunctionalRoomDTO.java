package com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqUpdateFunctionalRoomDTO(
                @NotBlank(message = "Mã phòng không được để trống") @Size(max = 50, message = "Mã phòng tối đa 50 ký tự") String code,

                @NotBlank(message = "Tên phòng không được để trống") @Size(max = 100, message = "Tên phòng tối đa 100 ký tự") String name,

                @NotNull(message = "Trạng thái không được để trống") RoomStatus status,

                @NotNull(message = "expectedVersion không được để trống") @Min(value = 0, message = "expectedVersion phải >= 0") Long expectedVersion) {
}

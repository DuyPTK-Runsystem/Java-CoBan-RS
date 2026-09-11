package com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReqCreateFunctionalRoomDTO(
                @NotBlank(message = "Mã phòng không được để trống") @Size(max = 50, message = "Mã phòng tối đa 50 ký tự") String code,

                @NotBlank(message = "Tên phòng không được để trống") @Size(max = 100, message = "Tên phòng tối đa 100 ký tự") String name) {
}

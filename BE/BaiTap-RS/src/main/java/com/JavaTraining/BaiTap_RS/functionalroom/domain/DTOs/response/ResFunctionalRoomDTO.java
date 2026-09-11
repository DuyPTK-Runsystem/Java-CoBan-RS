package com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;

public record ResFunctionalRoomDTO(
                Long id,
                String code,
                String name,
                RoomStatus status,
                Long version,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}
